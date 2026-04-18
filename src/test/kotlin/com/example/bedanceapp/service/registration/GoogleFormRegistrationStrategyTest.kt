package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("GoogleFormRegistrationStrategy Tests")
class GoogleFormRegistrationStrategyTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
    @Mock private lateinit var googleFormMapper: GoogleFormMapper

    private lateinit var strategy: GoogleFormRegistrationStrategy

    private val eventId = UUID.randomUUID()
    private val organizerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = GoogleFormRegistrationStrategy(
            eventRegistrationRepository,
            eventRegistrationSettingsRepository,
            googleFormMapper
        )
    }

    @Test
    fun `getRegistrationData returns parsed headers and mapped rows`() {
        val formStructure = "form-structure-json"
        val settings = EventRegistrationSettings(
            eventId = eventId,
            registrationMode = RegistrationMode.GOOGLE_FORM,
            formStructure = formStructure,
            formId = "form-1",
            requireApproval = false
        )
        val registration1 = createRegistration(responseId = "resp-1")
        val registration2 = createRegistration(responseId = "resp-2")
        val row1 = RegistrationRow("resp-1", RegistrationUserDto("a@example.com"), emptyList(), status = RegistrationStatus.PENDING)
        val row2 = RegistrationRow("resp-2", RegistrationUserDto("b@example.com"), emptyList(), status = RegistrationStatus.REGISTERED)
        val headers = listOf(GoogleFormHeaders.TIMESTAMP, GoogleFormHeaders.EMAIL)

        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(settings)
        whenever(googleFormMapper.parseFormStructure(formStructure)).thenReturn(StructuredForm("rev-1", headers))
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(listOf(registration1, registration2))
        whenever(googleFormMapper.mapRegistrationToRegistrationRow(registration1)).thenReturn(row1)
        whenever(googleFormMapper.mapRegistrationToRegistrationRow(registration2)).thenReturn(row2)

        val result = strategy.getRegistrationData(createEvent(eventId))

        assertEquals(1, result.headers.size)
        assertEquals(2, result.registrations.size)
        assertEquals("resp-1", result.registrations[0].id)
        assertEquals("resp-2", result.registrations[1].id)

        verify(eventRegistrationSettingsRepository).findByEventId(eq(eventId))
        verify(eventRegistrationRepository).findByEventId(eq(eventId))
    }

    @Test
    fun `getRegistrationData returns empty headers when no form structure available`() {
        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(null)
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(emptyList())

        val result = strategy.getRegistrationData(createEvent(eventId))

        assertEquals(listOf(RegistrationHeaders.UPDATED_AT), result.headers)
        assertEquals(emptyList(), result.registrations)
    }

    @Test
    fun `getRegistrationData throws when event id is null`() {
        assertThrows<IllegalArgumentException> {
            strategy.getRegistrationData(createEvent(null))
        }
    }

    @Test
    fun `getRegistrationData throws when registration responseId is null`() {
        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(null)
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(listOf(createRegistration(responseId = null)))

        assertThrows<IllegalArgumentException> {
            strategy.getRegistrationData(createEvent(eventId))
        }
    }

    private fun createRegistration(responseId: String?): EventRegistration {
        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            userId = UUID.randomUUID(),
            status = RegistrationStatus.PENDING,
            roleId = null,
            email = "test@example.com",
            responseId = responseId
        )
    }

    private fun createEvent(id: UUID?): Event {
        return Event(
            id = id,
            organizerId = organizerId,
            organizer = User(
                id = organizerId,
                email = "organizer@example.com",
                provider = "google",
                providerId = "provider-1"
            ),
            eventName = "Google Form Event",
            description = "desc",
            eventDate = LocalDate.now().plusDays(1),
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = BigDecimal.ZERO,
            maxAttendees = 20,
            status = EventStatus.PUBLISHED,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}

