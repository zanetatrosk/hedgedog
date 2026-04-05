package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.google.api.services.forms.v1.model.Answer
import com.google.api.services.forms.v1.model.Form
import com.google.api.services.forms.v1.model.FormResponse
import com.google.api.services.forms.v1.model.Item
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class GoogleFormMapper(
    private val objectMapper: ObjectMapper
) {
    fun mapRegistrationToRegistrationRow(registration: EventRegistration): RegistrationRow {
        val registrationId = registration.id ?: throw IllegalArgumentException("Registration ID cannot be null")
        return RegistrationRow(
            id = registrationId.toString(),
            user = RegistrationUserDto(
                email = registration.email,
                userId = registration.userId
            ),
            data = parseRowStructure(registration.formResponses)?.data ?: emptyList(),
            status = registration.status
        )
    }

    fun mapFormResponseToRegistrationRow(response: FormResponse, userId: UUID?): RegistrationRow? {
        val responseId = response.responseId ?: return null
        val answers = response.answers ?: return null
        val email = response.respondentEmail ?: "unknown@example.com"

        val dataFields = mutableListOf<RegistrationDataDto>()

        response.lastSubmittedTime?.let {
            dataFields.add(RegistrationDataDto(GoogleFormHeaders.TIMESTAMP.id, it))
        }

        answers.forEach { (questionId, answer) ->
            dataFields.add(RegistrationDataDto(questionId, extractAnswerText(answer)))
        }

        if (response.respondentEmail != null) {
            dataFields.add(RegistrationDataDto(GoogleFormHeaders.EMAIL.id, response.respondentEmail))
        }

        return RegistrationRow(
            id = responseId,
            user = RegistrationUserDto(
                email = email,
                userId = userId
            ),
            data = dataFields,
            lastSubmittedTime = response.lastSubmittedTime,
            status = RegistrationStatus.PENDING
        )
    }

    fun convertGoogleFormToStructuredForm(form: Form): StructuredForm {
        val revisionId = form.revisionId ?: throw IllegalArgumentException("Form has no revision ID")
        return StructuredForm(
            revisionId = revisionId,
            headers = getDynamicHeaders(form)
        )
    }

    fun writeStructuredForm(structuredForm: StructuredForm): String = objectMapper.writeValueAsString(structuredForm)

    fun writeRowStructure(row: RegistrationRow): String =
        objectMapper.writeValueAsString(RowStructure(row.data, row.lastSubmittedTime))

    fun parseFormStructure(formStructureJson: String?): StructuredForm? =
        formStructureJson.toObject(objectMapper, StructuredForm::class.java)

    fun parseRowStructure(rowStructureJson: String?): RowStructure? =
        rowStructureJson.toObject(objectMapper, RowStructure::class.java)

    fun extractRevisionId(formStructureJson: String?): String? =
        parseFormStructure(formStructureJson)?.revisionId

    private fun getDynamicHeaders(form: Form): List<Header> {
        val headers = mutableListOf<Header>()
        headers.add(GoogleFormHeaders.TIMESTAMP)
        headers.add(GoogleFormHeaders.EMAIL)

        form.items?.forEach { item ->
            extractQuestionFromItem(item)?.let { headers.add(it) }
        }

        if (form.settings?.quizSettings?.isQuiz == true || form.responderUri?.contains("emailAddress") == true) {
            headers.add(GoogleFormHeaders.EMAIL)
        }

        return headers
    }

    private fun extractQuestionFromItem(item: Item): Header? {
        val questionItem = item.questionItem ?: return null
        val question = questionItem.question ?: return null

        val questionId = question.questionId ?: return null
        val title = item.title ?: "Question"

        return when {
            question.choiceQuestion != null -> {
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

    private fun extractAnswerText(answer: Answer): String {
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



