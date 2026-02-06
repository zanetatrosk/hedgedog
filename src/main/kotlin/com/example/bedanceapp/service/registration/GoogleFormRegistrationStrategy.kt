package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.GoogleFormsService
import com.google.api.services.forms.v1.model.FormResponse
import com.google.api.services.forms.v1.model.Item
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
    private val userRepository: UserRepository
) : RegistrationDataStrategy {

    private val logger = LoggerFactory.getLogger(GoogleFormRegistrationStrategy::class.java)

    @Transactional(readOnly = true)
    override fun getRegistrationData(event: Event): RegistrationData {
        // Validate that formId exists
        val formId = event.formId
            ?: throw IllegalArgumentException("Event ${event.id} is set to GOOGLE_FORM mode but has no formId")

        // Get event organizer to access their Google Forms
        val organizer = event.organizer
            ?: throw IllegalArgumentException("Event organizer not found for event ${event.id}")

        // Fetch form structure and responses from Google Forms API
        val headers = getDynamicHeaders(organizer, formId)
        val registrations = getFormResponses(organizer, formId)

        return RegistrationData(headers, registrations)
    }

    /**
     * Get dynamic headers from Google Form questions
     * Each form can have completely different questions
     */
    private fun getDynamicHeaders(organizer: com.example.bedanceapp.model.User, formId: String): List<Header> {
        try {
            val form = googleFormsService.getForm(organizer, formId)

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
        } catch (e: Exception) {
            // If we can't fetch the form, return basic headers
            // This could happen if permissions are not granted or form is deleted
            logger.error("Failed to fetch Google Form structure for formId: $formId", e)
            return listOf(
                GoogleFormHeaders.TIMESTAMP,
                GoogleFormHeaders.ERROR
            )
        }
    }

    /**
     * Get registrations from Google Form responses
     * Data comes from Google Forms API, not our database
     */
    private fun getFormResponses(organizer: com.example.bedanceapp.model.User, formId: String): List<RegistrationRow> {
        try {
            val responsesResponse = googleFormsService.getFormResponses(organizer, formId)
            val responses = responsesResponse.responses ?: return emptyList()

            return responses.mapNotNull { response ->
                mapFormResponseToRegistrationRow(response)
            }
        } catch (e: Exception) {
            // If we can't fetch responses, return empty list
            // This could happen if permissions are not granted or form has no responses
            logger.error("Failed to fetch Google Form responses for formId: $formId", e)
            return emptyList()
        }
    }

    /**
     * Extract a question header from a Google Form item
     */
    private fun extractQuestionFromItem(item: Item): Header? {
        val questionItem = item.questionItem ?: return null
        val question = questionItem.question ?: return null

        val questionId = question.questionId ?: return null
        val title = item.title ?: "Question"

        // Determine question type based on the question structure
        val questionType = when {
            question.choiceQuestion != null -> FormQuestionType.SET
            question.textQuestion != null -> FormQuestionType.TEXT
            question.scaleQuestion != null -> FormQuestionType.TEXT
            question.dateQuestion != null -> FormQuestionType.TEXT
            question.timeQuestion != null -> FormQuestionType.TEXT
            else -> FormQuestionType.TEXT
        }

        return Header(questionId, title, questionType)
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
        response.createTime?.let {
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
            data = dataFields
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

