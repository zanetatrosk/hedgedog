package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.SkillLevel
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = TextHeader::class, name = "TEXT"),
    JsonSubTypes.Type(value = ChoiceHeader::class, name = "SET")
)
sealed class Header(
    open val id: String,
    open val question: String
) {
    abstract val type: FormQuestionType
}

data class TextHeader(
    override val id: String,
    override val question: String
) : Header(id, question) {
    @JsonProperty("type")
    override val type: FormQuestionType = FormQuestionType.TEXT
}

data class ChoiceHeader(
    override val id: String,
    override val question: String,
    val answerSet: List<String>
) : Header(id, question) {
    @JsonProperty("type")
    override val type: FormQuestionType = FormQuestionType.SET
}

data class RegistrationRow(
    val id: String,
    val user: RegistrationUserDto,
    val data: List<RegistrationDataDto>,
    val status: RegistrationStatus
)

data class RowStructure (
    val data: List<RegistrationDataDto>,
    val lastSubmittedTime: String? = null
)

data class RegistrationUserDto(
    val email: String? = null,
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

data class StructuredForm (
    val revisionId: String,
    val headers: List<Header>,
)

/**
 * Common headers used across registration modes
 */
object RegistrationHeaders {
    const val FULLNAME_ID = "fullname"
    const val EMAIL_ID = "email"
    const val EXPERIENCE_ID = "experience"
    const val UPDATED_AT_ID = "updatedAt"

    val FULLNAME = TextHeader(FULLNAME_ID, "Full name")
    val EMAIL = TextHeader(EMAIL_ID, "Email")

    /**
     * Create EXPERIENCE header with answer set from database
     */
    fun experience(skillLevels: List<SkillLevel>): ChoiceHeader {
        return ChoiceHeader(
            EXPERIENCE_ID,
            "Experience level",
            skillLevels.sortedBy { it.levelOrder }.map { it.name }
        )
    }

    val UPDATED_AT = TextHeader(UPDATED_AT_ID, "Last updated")
}

/**
 * Headers specific to couple mode
 */
object CoupleHeaders {
    const val ROLE_ID = "role"

    /**
     * Create ROLE header with answer set from database
     */
    fun role(dancerRoles: List<DancerRole>): ChoiceHeader {
        return ChoiceHeader(
            ROLE_ID,
            "Role",
            dancerRoles.map { it.name }
        )
    }

}

/**
 * Headers specific to Google Forms mode
 */
object GoogleFormHeaders {
    val TIMESTAMP = TextHeader("timestamp", "Last submitted")
    val EMAIL = TextHeader("email", "Email Address")
}

