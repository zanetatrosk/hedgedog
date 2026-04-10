package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.GoogleFormsService
import com.google.api.services.forms.v1.model.Form
import com.google.api.services.forms.v1.model.FormResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.time.Instant
import java.util.UUID

@Service
class GoogleFormSyncService(
    private val googleFormsService: GoogleFormsService,
    private val userRepository: UserRepository,
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val registrationStatusService: RegistrationStatusService,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    private val googleFormMapper: GoogleFormMapper,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = LoggerFactory.getLogger(GoogleFormSyncService::class.java)

    fun syncRegistrationData(eventId: UUID, organizerId: UUID, maxAttendees: Int?) {
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventId)
            ?: throw IllegalArgumentException("Event $eventId has no registration settings")

        val formId = registrationSettings.formId
            ?: throw IllegalArgumentException("Event $eventId is set to GOOGLE_FORM mode but has no formId")

        val organizer = userRepository.findById(organizerId).orElseThrow { IllegalArgumentException("Organizer not found with id: $organizerId") }
        logger.info("Fetching Google Form metadata for formId: $formId")
        val form = googleFormsService.getForm(organizer, formId)
        val responses = googleFormsService.getFormResponses(organizer, formId).responses ?: emptyList()

        transactionTemplate.executeWithoutResult {
            saveSyncData(eventId, maxAttendees, form, responses)
        }
    }

    private fun saveSyncData(eventId: UUID, maxAttendees: Int?, form: Form, responses: List<FormResponse>) {
        updateFormStructure(eventId, form)

        responses.forEach { response ->
            try {
                processResponse(eventId, maxAttendees, response)
            } catch (e: Exception) {
                logger.error("Failed to process individual form response: ${response.responseId}", e)
            }
        }

        eventRegistrationRepository.flush()
    }

    private fun updateFormStructure(eventId: UUID, form: Form) {
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventId)
            ?: throw IllegalArgumentException("Event $eventId has no registration settings")

        val existingRevisionId = googleFormMapper.extractRevisionId(registrationSettings.formStructure)
        val newRevisionId = form.revisionId

        if (existingRevisionId == newRevisionId) {
            logger.info("Form structure unchanged for event $eventId (revision: $existingRevisionId)")
            return
        }

        logger.info("Form structure changed for event $eventId. Old revision: $existingRevisionId, New revision: $newRevisionId")

        val structuredForm = googleFormMapper.convertGoogleFormToStructuredForm(form)
        val updatedSettings = registrationSettings.copy(formStructure = googleFormMapper.writeStructuredForm(structuredForm))
        eventRegistrationSettingsRepository.save(updatedSettings)

        logger.info("Synced form structure for event $eventId with revision ${structuredForm.revisionId}")
    }

    private fun processResponse(eventId: UUID, maxAttendees: Int?, response: FormResponse) {
        val userId = response.respondentEmail?.let { email -> userRepository.findByEmail(email)?.id }
        val registrationRow = googleFormMapper.mapFormResponseToRegistrationRow(response, userId) ?: return

        saveFormResponseToDatabase(
            registrationRow = registrationRow,
            eventId = eventId,
            responseId = registrationRow.id,
            maxAttendees = maxAttendees,
            lastUpdated = response.lastSubmittedTime
        )
    }

    private fun saveFormResponseToDatabase(
        registrationRow: RegistrationRow,
        eventId: UUID,
        responseId: String,
        maxAttendees: Int?,
        lastUpdated: String? = null
    ) {
        try {
            val userId = registrationRow.user.userId
            val existingRegistration = eventRegistrationRepository.findByEventIdAndResponseId(eventId, responseId).firstOrNull()
                ?: userId?.let { eventRegistrationRepository.findByEventIdAndUserId(eventId, it).firstOrNull() }

            val registrations = eventRegistrationRepository.findByEventId(eventId)

            if (existingRegistration == null) {
                val now = LocalDateTime.now()
                val assignedStatus = registrationStatusService.assignRegistrationStatus(
                    registrations,
                    RegistrationStatus.REGISTERED,
                    eventId,
                    null,
                    maxAttendees
                )
                val newRegistration = EventRegistration(
                    eventId = eventId,
                    userId = userId,
                    status = assignedStatus,
                    roleId = null,
                    email = registrationRow.user.email,
                    isAnonymous = true,
                    responseId = responseId,
                    formResponses = googleFormMapper.writeRowStructure(registrationRow),
                    updatedAt = now,
                    waitlistedAt = RegistrationWaitlistTimestampResolver.resolve(
                        previousStatus = null,
                        previousWaitlistedAt = null,
                        newStatus = assignedStatus,
                        now = now
                    )
                )

                eventRegistrationRepository.save(newRegistration)
            } else {
                maybeUpdateUserId(existingRegistration, registrationRow)
                maybeUpdateResponses(existingRegistration, registrationRow, lastUpdated)
            }

        } catch (e: Exception) {
            logger.error("Failed to save Google Form response to database: ${registrationRow.id}", e)
            throw RuntimeException("Failed to save Google Form response: ${e.message}", e)
        }
    }

    private fun maybeUpdateUserId(existingRegistration: EventRegistration, registrationRow: RegistrationRow) {
        if (registrationRow.user.userId != null && existingRegistration.userId == null) {
            eventRegistrationRepository.save(
                existingRegistration.copy(
                    userId = registrationRow.user.userId,
                    updatedAt = LocalDateTime.now()
                )
            )
        }
    }

    private fun maybeUpdateResponses(existingRegistration: EventRegistration, registrationRow: RegistrationRow, lastUpdated: String?) {
        val existingTimestamp = googleFormMapper.parseRowStructure(existingRegistration.formResponses)?.lastSubmittedTime
        val shouldUpdate = try {
            existingTimestamp?.let { existing ->
                lastUpdated?.let { incoming -> Instant.parse(existing).isBefore(Instant.parse(incoming)) }
            } ?: (lastUpdated != null)
        } catch (e: Exception) {
            logger.warn(
                "Failed to compare timestamps for registration ${existingRegistration.id}: existing=$existingTimestamp, new=$lastUpdated",
                e
            )
            false
        }

        if (shouldUpdate) {
            eventRegistrationRepository.save(
                existingRegistration.copy(
                    formResponses = googleFormMapper.writeRowStructure(registrationRow),
                    updatedAt = LocalDateTime.now()
                )
            )
        }
    }
}

