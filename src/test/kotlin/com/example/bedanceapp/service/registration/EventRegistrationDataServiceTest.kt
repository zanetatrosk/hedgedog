package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.validation.EventAccessValidator
import com.example.bedanceapp.service.event.EventService
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

@DisplayName("EventRegistrationDataService Tests")
class EventRegistrationDataServiceTest {

    @Mock private lateinit var eventAccessValidator: EventAccessValidator
    @Mock private lateinit var eventService: EventService
    @Mock private lateinit var registrationStrategyFactory: RegistrationStrategyFactory
    @Mock private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
    @Mock private lateinit var strategy: RegistrationDataStrategy

    private lateinit var service: EventRegistrationDataService

    private val eventId = UUID.randomUUID()
    private val parentEventId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = EventRegistrationDataService(
            eventAccessValidator,
            eventService,
            registrationStrategyFactory,
            eventRegistrationSettingsRepository
        )
    }

    @Test
    fun `getAllRegistrationsByEvent uses OPEN mode when settings missing`() {
        val event = createEvent()
        val registrationData = RegistrationData(headers = emptyList(), registrations = emptyList())
        val recurring = listOf(RecurringDateInfo(date = "2026-04-20", id = UUID.randomUUID().toString()))

        whenever(eventAccessValidator.requireOwnedEvent(eventId, userId)).thenReturn(event)
        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(null)
        whenever(registrationStrategyFactory.getStrategy(RegistrationMode.OPEN)).thenReturn(strategy)
        whenever(strategy.getRegistrationData(event)).thenReturn(registrationData)
        whenever(eventService.getUpcomingDates(parentEventId, true)).thenReturn(recurring)

        val response = service.getAllRegistrationsByEvent(eventId, userId)

        assertEquals(eventId, response.eventId)
        assertEquals("Event", response.eventName)
        assertEquals(event.eventDate.toString(), response.date)
        assertEquals(RegistrationMode.OPEN, response.registrationMode)
        assertEquals(registrationData, response.registrationData)
        assertEquals(recurring, response.recurringDates)
        verify(eventService).getUpcomingDates(parentEventId, true)
        verify(registrationStrategyFactory).getStrategy(eq(RegistrationMode.OPEN))
    }

    @Test
    fun `getAllRegistrationsByEvent uses mode from settings`() {
        val event = createEvent()
        val registrationData = RegistrationData(headers = emptyList(), registrations = emptyList())
        val settings = EventRegistrationSettings(
            eventId = eventId,
            event = null,
            registrationMode = RegistrationMode.COUPLE,
            formId = null,
            formStructure = null,
            requireApproval = false
        )

        whenever(eventAccessValidator.requireOwnedEvent(eventId, userId)).thenReturn(event)
        whenever(eventRegistrationSettingsRepository.findByEventId(eventId)).thenReturn(settings)
        whenever(registrationStrategyFactory.getStrategy(RegistrationMode.COUPLE)).thenReturn(strategy)
        whenever(strategy.getRegistrationData(event)).thenReturn(registrationData)
        whenever(eventService.getUpcomingDates(parentEventId, true)).thenReturn(emptyList())

        val response = service.getAllRegistrationsByEvent(eventId, userId)

        assertEquals(RegistrationMode.COUPLE, response.registrationMode)
        assertEquals(registrationData, response.registrationData)
        verify(eventService).getUpcomingDates(parentEventId, true)
        verify(registrationStrategyFactory).getStrategy(eq(RegistrationMode.COUPLE))
    }

    @Test
    fun `getAllRegistrationsByEvent throws when event not found`() {
        whenever(eventAccessValidator.requireOwnedEvent(eventId, userId)).thenThrow(
            IllegalArgumentException("Event not found with id: $eventId")
        )

        assertThrows<IllegalArgumentException> {
            service.getAllRegistrationsByEvent(eventId, userId)
        }
    }

    private fun createEvent(): Event {
        val organizerId = UUID.randomUUID()
        return Event(
            id = eventId,
            parentEventId = parentEventId,
            organizerId = organizerId,
            organizer = User(
                id = organizerId,
                email = "organizer@example.com",
                provider = "google",
                providerId = "provider-org"
            ),
            eventName = "Event",
            description = "desc",
            eventDate = LocalDate.of(2026, 4, 20),
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = BigDecimal.ZERO,
            maxAttendees = 30,
            status = EventStatus.PUBLISHED,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
