package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.EventRegistrationManager
import com.example.bedanceapp.service.GoogleFormsService
import tools.jackson.databind.ObjectMapper
import com.google.api.services.forms.v1.model.Form
import com.google.api.services.forms.v1.model.FormResponse
import com.google.api.services.forms.v1.model.Item
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jsonMapper
import java.time.Instant
import java.util.UUID

/**
 * Strategy for GOOGLE_FORM registration mode
 *
 * Completely different implementation from Open/Couple modes:
 * - Headers are dynamic based on Google Form questions
 * - Data is synced from Google Forms API, not from our database
 * - No code sharing with Open/Couple modes (they use our DB, this uses external API)
 *
 * This is why it doesn't extend OpenModeRegistrationStrategy - it's fundamentally different
 */
@Component
class GoogleFormRegistrationStrategy(
    private val googleFormsService: GoogleFormsService,
    private val userRepository: UserRepository,
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRegistrationManager: EventRegistrationManager,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    private val objectMapper: ObjectMapper
) : RegistrationDataStrategy {

    private val logger = LoggerFactory.getLogger(GoogleFormRegistrationStrategy::class.java)

    @Transactional
    override fun getRegistrationData(event: Event): RegistrationData {
        val eventId = event.id ?: throw IllegalArgumentException("Event ID cannot be null")

        val form = eventRegistrationSettingsRepository.findByEventId(eventId)?.formStructure
        val headers = parseFormStructure(form)?.headers ?: emptyList()
        val fetchedRegistrations = matchGoogleResponsesToDbRegistrations(eventId)

//        val formId = eventRegistrationSettingsRepository.findByEventId(eventId)?.formId ?: throw IllegalArgumentException("Event ID cannot be null")
//        val form = googleFormsService.getForm(event.organizer, formId)
//        val headers = getDynamicHeaders(form)
//        val fetchedReg = googleFormsService.getFormResponses(event.organizer, formId)
//        val registrations = eventRegistrationRepository.findByEventId(eventId)
//        val fetchedRegistrations = fetchedReg.responses?.mapNotNull { googleRegistration ->
//            val registrationInDb = registrations.find { it.responseId == googleRegistration.responseId }
//            if (registrationInDb != null) {
//                mapFormResponseToRegistrationRow(googleRegistration)
//            } else {
//                null
//            }
//        } ?: emptyList()

        return RegistrationData(headers, fetchedRegistrations)
    }

    private fun matchGoogleResponsesToDbRegistrations(eventId: UUID): List<RegistrationRow>{
        val registrations = eventRegistrationRepository.findByEventId(eventId)
        return registrations.mapNotNull { googleRegistration ->
            val registrationInDb = registrations.find { it.responseId == googleRegistration.responseId }
            if (registrationInDb != null) {
                mapRegistrationToRegistrationRow(registrationInDb)
            } else {
                null
            }
        }
    }

    private fun mapRegistrationToRegistrationRow(registration: EventRegistration): RegistrationRow {
        return RegistrationRow(
            id = registration.id.toString(),
            user = RegistrationUserDto(
                email = registration.email,
                userId = registration.userId
            ),
            data = parseRowStructure(registration.formResponses)?.data ?: emptyList(),
            status = registration.status
        )
    }

    @Transactional
    fun syncRegistrationData(event: Event){
        val eventId = event.id ?: throw IllegalArgumentException("Event ID cannot be null")
        // Get registration settings to get formId
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventId)
            ?: throw IllegalArgumentException("Event ${event.id} has no registration settings")

        val formId = registrationSettings.formId
            ?: throw IllegalArgumentException("Event ${event.id} is set to GOOGLE_FORM mode but has no formId")
        // Get event organizer to access their Google Forms
        val organizer = event.organizer

        //fetch google form
        val form = googleFormsService.getForm(organizer, formId)
        syncForm(form, eventId)
        syncFormResponses(organizer, formId, event)
    }



    private fun convertGoogleFormToStructuredForm(form: Form, eventId: UUID): StructuredForm {
        val revisionId = form.revisionId ?: throw IllegalArgumentException("Form has no revision ID")

        return StructuredForm(
            revisionId = revisionId,
            headers = getDynamicHeaders(form)
        )
    }

    @Transactional
    fun syncForm(form: Form, eventId: UUID) {
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventId)
            ?: throw IllegalArgumentException("Event $eventId has no registration settings")

        val existingFormJson = registrationSettings.formStructure
        val existingRevisionId = extractRevisionId(existingFormJson)
        val newRevisionId = form.revisionId

        // Check if form has changed
        if (existingRevisionId == newRevisionId) {
            logger.info("Form structure unchanged for event $eventId (revision: $existingRevisionId)")
            return
        }

        logger.info("Form structure changed for event $eventId. Old revision: $existingRevisionId, New revision: $newRevisionId")

        // Convert and save new structure
        val structuredForm = convertGoogleFormToStructuredForm(form, eventId)
        val formStructureJson = objectMapper.writeValueAsString(structuredForm)

        // Create a new copy with updated formStructure (since it's a data class with val properties)
        val updatedSettings = registrationSettings.copy(formStructure = formStructureJson)
        eventRegistrationSettingsRepository.save(updatedSettings)

        logger.info("Synced form structure for event $eventId with revision ${structuredForm.revisionId}")
    }

    /**
     * Generic JSON parser that handles null/blank checks and error logging
     */
    private inline fun <reified T> parseJson(
        json: String?,
        typeName: String = T::class.simpleName ?: "object",
        verbose: Boolean = false
    ): T? {
        if (json.isNullOrBlank()) {
            if (verbose) logger.debug("$typeName JSON is null or blank")
            return null
        }

        return try {
            if (verbose) logger.debug("Attempting to parse $typeName JSON: $json")
            val result = objectMapper.readValue(json, T::class.java)
            if (verbose && result is RowStructure) {
                logger.debug("Successfully parsed $typeName with ${result.data.size} data items")
            }
            result
        } catch (e: Exception) {
            logger.error("Failed to parse $typeName JSON: $json", e)
            if (verbose) {
                logger.error("Exception details: ${e.javaClass.name}: ${e.message}")
                e.printStackTrace()
            }
            null
        }
    }

    /**
     * Parse formStructure JSON from database into StructuredForm object
     */
    private fun parseFormStructure(formStructureJson: String?): StructuredForm? =
        parseJson(formStructureJson, "StructuredForm")

    /**
     * Parse rowStructure JSON from database into RowStructure object
     */
    private fun parseRowStructure(rowStructureJson: String?): RowStructure? =
        parseJson(rowStructureJson, "RowStructure", verbose = true)

    /**
     * Extract just the revisionId from JSONB string without parsing the entire object
     * This is faster than parsing the whole JSON when you only need one field
     */
    private fun extractRevisionId(formStructureJson: String?): String? {
        if (formStructureJson.isNullOrBlank()) {
            return null
        }

        return try {
            val jsonNode = objectMapper.readTree(formStructureJson)
            jsonNode.get("revisionId")?.asText()
        } catch (e: Exception) {
            logger.error("Failed to extract revisionId from JSON: $formStructureJson", e)
            null
        }
    }

    /**
     * Get dynamic headers from Google Form questions
     * Each form can have completely different questions
     */
    private fun getDynamicHeaders(form: Form): List<Header> {
            val headers = mutableListOf<Header>()

            // Add timestamp header (always present in Google Forms)
            headers.add(GoogleFormHeaders.TIMESTAMP)
            headers.add(GoogleFormHeaders.EMAIL)

            // Extract questions from form items
            form.items?.forEach { item ->
                val question = extractQuestionFromItem(item)
                if (question != null) {
                    headers.add(question)
                }
            }

            // Add email header if form collects email
            if (form.settings?.quizSettings?.isQuiz == true || form.responderUri?.contains("emailAddress") == true) {
                headers.add(GoogleFormHeaders.EMAIL)
            }

            return headers
    }

    /**
     * Get registrations from Google Form responses
     * Data comes from Google Forms API, not our database
     * Also saves/updates the registrations in our database
     */
    private fun syncFormResponses(organizer: User, formId: String, event: Event): List<RegistrationRow> {
        try {
            logger.info("Fetching form responses for formId: $formId")
            val responsesResponse = googleFormsService.getFormResponses(organizer, formId)
            val responses = responsesResponse.responses ?: return emptyList()
            logger.info("Found ${responses.size} form responses")
            val eventId = event.id ?: throw IllegalArgumentException("Event ID cannot be null")

            return responses.mapNotNull { response ->
                try {
                    val registrationRow = mapFormResponseToRegistrationRow(response)
                    if (registrationRow != null) {
                        logger.info("Mapped form response to registration row: ${registrationRow.id}")
                        // Save or update the registration in our database
                        saveFormResponseToDatabase(registrationRow, eventId, response.responseId, event.maxAttendees, response.lastSubmittedTime, )
                    } else {
                        logger.warn("Failed to map form response: ${response.responseId}")
                    }
                    registrationRow
                } catch (e: Exception) {
                    logger.error("Failed to process individual form response: ${response.responseId}", e)
                    // Continue processing other responses even if one fails
                    null
                }
            }
        } catch (e: Exception) {
            // If we can't fetch responses, return empty list
            // This could happen if permissions are not granted or form has no responses
            logger.error("Failed to fetch Google Form responses for formId: $formId", e)
            return emptyList()
        }
    }

    /**
     * Save or update a Google Form response to our registrations table
     */
    private fun saveFormResponseToDatabase(registrationRow: RegistrationRow, eventId: UUID, responseId: String, maxAttendees: Int?, lastUpdated: String? = null) {
        logger.info("=== ENTERING saveFormResponseToDatabase ===")
        logger.info("Registration Row ID: ${registrationRow.id}")
        logger.info("Event ID: $eventId")

        try {
            val email = registrationRow.user.email
            val userId = registrationRow.user.userId
            logger.info("Checking for existing registration...")
            val existingRegistration = eventRegistrationRepository.findByEventIdAndResponseId(eventId, responseId).firstOrNull()
                ?: userId?.let { eventRegistrationRepository.findByEventIdAndUserId(eventId, it).firstOrNull() }
            val registrations = eventRegistrationRepository.findByEventId(eventId)

            if (existingRegistration == null) {
                logger.info("No existing registration found, creating new one...")

                // Create new registration
                val newRegistration = EventRegistration(
                    eventId = eventId,
                    userId = userId,
                    status = eventRegistrationManager.assignEventStatus(registrations, eventId, RegistrationStatus.GOING, null, maxAttendees), // Default status for Google Form submissions
                    roleId = null, // Google Forms may not have role info
                    email = email,
                    responseId = responseId,
                    formResponses = objectMapper.writeValueAsString(RowStructure(registrationRow.data, registrationRow.lastSubmittedTime))
                )

                logger.info("About to save registration to database...")
                val savedRegistration = eventRegistrationRepository.save(newRegistration)
                logger.info("=== SUCCESS === Saved Google Form registration with ID: ${savedRegistration.id} for eventId: $eventId, email: $email")
            } else {
                val structuredRow = parseRowStructure(existingRegistration.formResponses)

                // Update userId if we now have a userId and didn't before
                if (registrationRow.user.userId != null && existingRegistration.userId == null) {
                    val updatedRegistration = existingRegistration.copy(userId = registrationRow.user.userId)
                    eventRegistrationRepository.save(updatedRegistration)
                }

                // Update form responses if the submission was updated
                val existingTimestamp = structuredRow?.lastSubmittedTime
                if (existingTimestamp != null && lastUpdated != null) {
                    try {
                        val existingInstant = Instant.parse(existingTimestamp)
                        val newInstant = Instant.parse(lastUpdated)

                        if (existingInstant.isBefore(newInstant)) {
                            val formResponses = objectMapper.writeValueAsString(RowStructure(registrationRow.data, registrationRow.lastSubmittedTime))
                            val updatedRegistration = existingRegistration.copy(formResponses = formResponses)
                            eventRegistrationRepository.save(updatedRegistration)
                            logger.info("Updated form responses for registration ${existingRegistration.id} - timestamp changed from $existingTimestamp to $lastUpdated")
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to compare timestamps for registration ${existingRegistration.id}: existing=$existingTimestamp, new=$lastUpdated", e)
                    }
                } else if (lastUpdated != null && existingTimestamp == null) {
                    // If we have a new timestamp but didn't have one before, always update
                    val formResponses = objectMapper.writeValueAsString(RowStructure(registrationRow.data, registrationRow.lastSubmittedTime))
                    val updatedRegistration = existingRegistration.copy(formResponses = formResponses)
                    eventRegistrationRepository.save(updatedRegistration)
                    logger.info("Updated form responses for registration ${existingRegistration.id} - added timestamp $lastUpdated")
                }

            }
            eventRegistrationRepository.flush()
        } catch (e: Exception) {
            logger.error("=== ERROR === Failed to save Google Form response to database: ${registrationRow.id}", e)
            logger.error("Exception type: ${e.javaClass.simpleName}")
            logger.error("Exception message: ${e.message}")
            e.printStackTrace()
            // Re-throw to ensure transaction rollback if needed
            throw RuntimeException("Failed to save Google Form response: ${e.message}", e)
        }

        logger.info("=== EXITING saveFormResponseToDatabase ===")
    }


    /**
     * Extract a question header from a Google Form item
     */
    private fun extractQuestionFromItem(item: Item): Header? {
        val questionItem = item.questionItem ?: return null
        val question = questionItem.question ?: return null

        val questionId = question.questionId ?: return null
        val title = item.title ?: "Question"

        // Create appropriate header type based on the question structure
        return when {
            question.choiceQuestion != null -> {
                // Extract answer options from choice question
                val options = question.choiceQuestion.options?.mapNotNull { it.value } ?: emptyList()
                ChoiceHeader(questionId, title, options)
            }
            question.textQuestion != null -> TextHeader(questionId, title)
            question.scaleQuestion != null -> TextHeader(questionId, title)
            question.dateQuestion != null -> TextHeader(questionId, title)
            question.timeQuestion != null -> TextHeader(questionId, title)
            else -> TextHeader(questionId, title)
        }
    }

    /**
     * Map a Google Form response to our RegistrationRow structure
     */
    private fun mapFormResponseToRegistrationRow(response: FormResponse): RegistrationRow? {
        val responseId = response.responseId ?: return null
        val answers = response.answers ?: return null

        // Extract email from respondent info (if available)
        val email = response.respondentEmail ?: "unknown@example.com"

        // Try to find user in our system by email
        val user = try {
            userRepository.findByEmail(email)
        } catch (e: Exception) {
            logger.warn("Failed to find user by email: $email", e)
            null
        }

        // Build data fields from answers
        val dataFields = mutableListOf<RegistrationDataDto>()

        // Add timestamp
        response.lastSubmittedTime?.let {
            dataFields.add(RegistrationDataDto(GoogleFormHeaders.TIMESTAMP.id, it))
        }

        // Add all question answers
        answers.forEach { (questionId, answer) ->
            val answerText = extractAnswerText(answer)
            dataFields.add(RegistrationDataDto(questionId, answerText))
        }

        // Add email if available
        if (response.respondentEmail != null) {
            dataFields.add(RegistrationDataDto(GoogleFormHeaders.EMAIL.id, response.respondentEmail))
        }

        return RegistrationRow(
            id = responseId,
            user = RegistrationUserDto(
                email = email,
                userId = user?.id
            ),
            data = dataFields,
            lastSubmittedTime = response.lastSubmittedTime,
            status = RegistrationStatus.PENDING
        )
    }

    /**
     * Extract answer text from various answer types
     */
    private fun extractAnswerText(answer: com.google.api.services.forms.v1.model.Answer): String {
        return when {
            answer.textAnswers?.answers?.isNotEmpty() == true -> {
                answer.textAnswers.answers.joinToString(", ") { it.value ?: "" }
            }
            answer.fileUploadAnswers?.answers?.isNotEmpty() == true -> {
                answer.fileUploadAnswers.answers.joinToString(", ") { it.fileName ?: "File" }
            }
            else -> ""
        }
    }
}

