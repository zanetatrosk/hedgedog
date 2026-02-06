package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.service.*
import java.util.UUID

/**
 * Strategy interface for loading registration data based on registration mode
 */
interface RegistrationDataStrategy {
    fun getRegistrationData(event: Event): RegistrationData
}

data class RegistrationData(
    val headers: List<Header>,
    val registrations: List<RegistrationRow>
)

data class Header(
    val id: String,
    val question: String,
    val type: FormQuestionType
)

data class RegistrationRow(
    val id: String,
    val user: RegistrationUserDto,
    val data: List<RegistrationDataDto>
)

data class RegistrationUserDto(
    val email: String,
    val userId: UUID? = null,
)

data class RegistrationDataDto(
    val id: String,
    val value: String
)

enum class FormQuestionType {
    SET,
    TEXT
}

/**
 * Common headers used across registration modes
 */
object RegistrationHeaders {
    val FULLNAME = Header("fullname", "Full name", FormQuestionType.TEXT)
    val EXPERIENCE = Header("experience", "Experience level", FormQuestionType.SET)
    val STATUS = Header("status", "Status", FormQuestionType.SET)
    val CREATED_AT = Header("createdAt", "Created at", FormQuestionType.TEXT)
}

/**
 * Headers specific to couple mode
 */
object CoupleHeaders {
    val ROLE = Header("role", "Role", FormQuestionType.SET)
    val PARTNER = Header("partner", "Partner", FormQuestionType.TEXT)
}

/**
 * Headers specific to Google Forms mode
 */
object GoogleFormHeaders {
    val TIMESTAMP = Header("timestamp", "Timestamp", FormQuestionType.TEXT)
    val EMAIL = Header("email", "Email Address", FormQuestionType.TEXT)
    val ERROR = Header("error", "Error", FormQuestionType.TEXT)
}

