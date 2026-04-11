package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.repository.EventRegistrationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("RegistrationAccessValidator Tests")
class RegistrationAccessValidatorTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository

    private lateinit var validator: RegistrationAccessValidator

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        validator = RegistrationAccessValidator(eventRegistrationRepository)
    }

    @Test
    fun `requireForEvent returns registration when it belongs to event`() {
        val eventId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val registration = createRegistration(registrationId, eventId, UUID.randomUUID())
        whenever(eventRegistrationRepository.findByIdAndEventId(registrationId, eventId)).thenReturn(registration)

        val result = validator.requireForEvent(registrationId, eventId)

        assertEquals(registration, result)
    }

    @Test
    fun `requireForEvent throws when registration does not belong to event`() {
        val eventId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        whenever(eventRegistrationRepository.findByIdAndEventId(registrationId, eventId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            validator.requireForEvent(registrationId, eventId)
        }

        assertEquals("Registration does not belong to this event", exception.message)
    }

    @Test
    fun `requireForUserInEvent returns registration when user owns it`() {
        val eventId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val registration = createRegistration(registrationId, eventId, userId)
        whenever(eventRegistrationRepository.findByIdAndEventIdAndUserId(registrationId, eventId, userId)).thenReturn(registration)

        val result = validator.requireForUserInEvent(registrationId, eventId, userId)

        assertEquals(registration, result)
    }

    @Test
    fun `requireForUserInEvent throws when user is not authorized`() {
        val eventId = UUID.randomUUID()
        val registrationId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        whenever(eventRegistrationRepository.findByIdAndEventIdAndUserId(registrationId, eventId, userId)).thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> {
            validator.requireForUserInEvent(registrationId, eventId, userId)
        }

        assertEquals("User is not authorized for this registration", exception.message)
    }

    private fun createRegistration(id: UUID, eventId: UUID, userId: UUID): EventRegistration {
        return EventRegistration(
            id = id,
            eventId = eventId,
            userId = userId,
            status = RegistrationStatus.REGISTERED,
            email = "test@example.com"
        )
    }
}

