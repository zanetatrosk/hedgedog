package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.google.api.services.forms.v1.model.Answer
import com.google.api.services.forms.v1.model.Form
import com.google.api.services.forms.v1.model.FormResponse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("GoogleFormMapper Tests")
class GoogleFormMapperTest {

    private val mapper = GoogleFormMapper(ObjectMapper())

    @Test
    fun `mapRegistrationToRegistrationRow maps registration and parsed form responses`() {
        val registrationId = UUID.randomUUID()
        val rowJson = mapper.writeRowStructure(
            RegistrationRow(
                id = "resp-1",
                user = RegistrationUserDto(email = "x@example.com"),
                data = listOf(RegistrationDataDto("q1", "value")),
                lastSubmittedTime = "2026-04-10T10:00:00Z",
                status = RegistrationStatus.PENDING
            )
        )

        val registration = EventRegistration(
            id = registrationId,
            eventId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            status = RegistrationStatus.REGISTERED,
            roleId = null,
            email = "mapped@example.com",
            formResponses = rowJson,
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now()
        )

        val mapped = mapper.mapRegistrationToRegistrationRow(registration)

        assertEquals(registrationId.toString(), mapped.id)
        assertEquals("mapped@example.com", mapped.user.email)
        assertEquals(registration.userId, mapped.user.userId)
        assertEquals(RegistrationStatus.REGISTERED, mapped.status)
        assertEquals("value", mapped.data.first().value)
    }

    @Test
    fun `mapRegistrationToRegistrationRow throws when registration id is null`() {
        val registration = EventRegistration(
            id = null,
            eventId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            status = RegistrationStatus.PENDING,
            roleId = null,
            email = "mapped@example.com"
        )

        assertThrows<IllegalArgumentException> {
            mapper.mapRegistrationToRegistrationRow(registration)
        }
    }

    @Test
    fun `mapFormResponseToRegistrationRow maps metadata answers and defaults`() {
        val response = FormResponse()
            .setResponseId("resp-2")
            .setRespondentEmail("form@example.com")
            .setLastSubmittedTime("2026-04-10T12:00:00Z")
            .setAnswers(mapOf("q1" to Answer()))

        val mapped = mapper.mapFormResponseToRegistrationRow(response, UUID.randomUUID())

        assertNotNull(mapped)
        assertEquals("resp-2", mapped.id)
        assertEquals("form@example.com", mapped.user.email)
        assertEquals(RegistrationStatus.PENDING, mapped.status)
        assertEquals("2026-04-10T12:00:00Z", mapped.lastSubmittedTime)
        assertEquals("timestamp", mapped.data.first().id)
        assertEquals("email", mapped.data.last().id)
    }

    @Test
    fun `mapFormResponseToRegistrationRow returns null when response id missing`() {
        val response = FormResponse().setAnswers(mapOf("q1" to Answer()))

        val mapped = mapper.mapFormResponseToRegistrationRow(response, null)

        assertNull(mapped)
    }

    @Test
    fun `convertGoogleFormToStructuredForm throws when revision id missing`() {
        assertThrows<IllegalArgumentException> {
            mapper.convertGoogleFormToStructuredForm(Form())
        }
    }

    @Test
    fun `write and parse structured form round-trip`() {
        val structured = StructuredForm(
            revisionId = "rev-1",
            headers = listOf(GoogleFormHeaders.TIMESTAMP, GoogleFormHeaders.EMAIL)
        )

        val json = mapper.writeStructuredForm(structured)
        val parsed = mapper.parseFormStructure(json)

        assertNotNull(parsed)
        assertEquals("rev-1", parsed.revisionId)
        assertEquals(2, parsed.headers.size)
        assertEquals("timestamp", parsed.headers[0].id)
    }

    @Test
    fun `extractRevisionId returns null for invalid json`() {
        val revision = mapper.extractRevisionId("{not-json}")

        assertNull(revision)
    }
}

