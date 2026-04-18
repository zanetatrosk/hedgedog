package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.OrganizerAction
import com.example.bedanceapp.model.RegistrationAction
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.validation.EventAccessValidator
import com.example.bedanceapp.service.validation.RegistrationAccessValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class OrganizerRegistrationService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val registrationStatusService: RegistrationStatusService,
    private val registrationRecalculateService: RegistrationRecalculateService,
    private val googleFormSyncService: GoogleFormSyncService,
    private val eventAccessValidator: EventAccessValidator,
    private val registrationAccessValidator: RegistrationAccessValidator,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {

    @Transactional
    fun updateRegistrationStatus(
        eventId: UUID,
        registrationId: UUID,
        organizerId: UUID,
        action: RegistrationAction
    ): EventRegistration {
        val event = eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.PUBLISHED)
        val registration = registrationAccessValidator.requireForEvent(registrationId, eventId)

        val organizerAction = when (action) {
            RegistrationAction.APPROVE -> OrganizerAction.APPROVE
            RegistrationAction.REJECT -> OrganizerAction.REJECT
            else -> throw IllegalArgumentException("Invalid organizer action")
        }

        return handleOrganizerAction(registration, organizerAction, event.maxAttendees)
    }

    @Transactional
    fun handleOrganizerAction(registration: EventRegistration, action: OrganizerAction, maxAttendees: Int?): EventRegistration {
        val newStatus = when (action) {
            OrganizerAction.APPROVE -> when (registration.status) {
                RegistrationStatus.PENDING -> resolveApprovedStatus(registration, maxAttendees)
                else -> throw IllegalStateException("Cannot approve from ${registration.status}")
            }

            OrganizerAction.REJECT -> when (registration.status) {
                RegistrationStatus.PENDING, RegistrationStatus.REGISTERED, RegistrationStatus.WAITLISTED -> RegistrationStatus.REJECTED
                else -> throw IllegalStateException("Cannot reject from ${registration.status}")
            }
        }

        registration.status.requireTransitionTo(newStatus)
        val now = LocalDateTime.now()

        val savedRegistration = eventRegistrationRepository.save(
            registration.copy(
                status = newStatus,
                updatedAt = now,
                waitlistedAt = RegistrationWaitlistTimestampResolver.resolve(
                    registration.status,
                    registration.waitlistedAt,
                    newStatus,
                    now
                )
            )
        )
        eventRegistrationRepository.flush()

        if (action == OrganizerAction.REJECT) {
            maxAttendees?.let { maxAttendees ->
                registrationRecalculateService.recalculate(registration.eventId, maxAttendees)
            }
        }

        return savedRegistration
    }

    fun syncGoogleFormData(eventId: UUID, organizerId: UUID) {
        val event = eventAccessValidator.requireOwnedEvent(eventId, organizerId)
        googleFormSyncService.syncRegistrationData(eventId, organizerId, event.maxAttendees)
    }

    private fun resolveApprovedStatus(registration: EventRegistration, maxAttendees: Int?): RegistrationStatus {
        val registrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(registration.eventId)
        val settings = eventRegistrationSettingsRepository.findByEventId(registration.eventId)

        return registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            roleId = registration.roleId,
            maxAttendees = maxAttendees,
            settings = settings
        )

    }
}
