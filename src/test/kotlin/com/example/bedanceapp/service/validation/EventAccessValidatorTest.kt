package com.example.bedanceapp.service

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.validation.EventAccessValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("EventAccessValidator Tests")
class EventAccessValidatorTest {

    @Mock private lateinit var eventRepository: EventRepository

    private lateinit var validator: EventAccessValidator

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        validator = EventAccessValidator(eventRepository)
    }

    @Test
    fun `requireOwnedEvent returns event when organizer and status are valid`() {
        val eventId = UUID.randomUUID()
        val organizerId = UUID.randomUUID()
        val event = createEvent(eventId, organizerId, EventStatus.DRAFT)
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))

        val result = validator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)

        assertEquals(event, result)
    }

    @Test
    fun `requireOwnedEvent throws when event not found`() {
        val eventId = UUID.randomUUID()
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            validator.requireOwnedEvent(eventId, UUID.randomUUID())
        }

        assertEquals("Event not found with id: $eventId", exception.message)
    }

    @Test
    fun `requireOwnedEvent throws when organizer does not match`() {
        val eventId = UUID.randomUUID()
        val event = createEvent(eventId, UUID.randomUUID(), EventStatus.PUBLISHED)
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))

        val exception = assertThrows<IllegalArgumentException> {
            validator.requireOwnedEvent(eventId, UUID.randomUUID())
        }

        assertEquals("Only the event organizer can manage this event", exception.message)
    }

    @Test
    fun `requireOwnedEvent throws when status is not allowed`() {
        val eventId = UUID.randomUUID()
        val organizerId = UUID.randomUUID()
        val event = createEvent(eventId, organizerId, EventStatus.CANCELLED)
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))

        val exception = assertThrows<IllegalArgumentException> {
            validator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT, EventStatus.PUBLISHED)
        }

        assertEquals(
            "Action not allowed for events with status: CANCELLED. Required: DRAFT, PUBLISHED",
            exception.message
        )
    }

    private fun createEvent(eventId: UUID, organizerId: UUID, status: EventStatus): Event {
        return Event(
            id = eventId,
            organizerId = organizerId,
            organizer = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "pid"),
            eventName = "Event",
            description = "desc",
            eventDate = LocalDate.now().plusDays(1),
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = null,
            maxAttendees = null,
            status = status,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}

