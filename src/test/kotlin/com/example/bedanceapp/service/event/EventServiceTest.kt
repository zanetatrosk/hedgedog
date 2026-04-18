package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.AttendeeStats
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventDetailAdditionalDetails
import com.example.bedanceapp.model.EventDetailBasicInfo
import com.example.bedanceapp.model.EventDetailDto
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.EventSummaryDto
import com.example.bedanceapp.model.LocationRequest
import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.RegistrationStats
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.dto.OrganizerDto
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.mapping.EventMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("EventService Tests")
class EventServiceTest {

    @Mock private lateinit var eventRepository: EventRepository
    @Mock private lateinit var eventMapper: EventMapper

    private lateinit var service: EventService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = EventService(eventRepository, eventMapper)
    }

    @Test
    fun `getAllPublishedEventsPaginated maps repository page to summary dto page`() {
        val userId = UUID.randomUUID()
        val pageable = PageRequest.of(0, 10)
        val first = createEvent(id = UUID.randomUUID())
        val second = createEvent(id = UUID.randomUUID())
        val firstDto = createSummaryDto(first.id.toString(), "Event A")
        val secondDto = createSummaryDto(second.id.toString(), "Event B")

        whenever(eventRepository.findAll(any<org.springframework.data.jpa.domain.Specification<Event>>(), eq(pageable)))
            .thenReturn(PageImpl(listOf(first, second), pageable, 2))
        whenever(eventMapper.toDto(first, userId)).thenReturn(firstDto)
        whenever(eventMapper.toDto(second, userId)).thenReturn(secondDto)

        val page = service.getAllPublishedEventsPaginated(userId = userId, pageable = pageable)

        assertEquals(listOf(firstDto, secondDto), page.content)
        verify(eventMapper).toDto(first, userId)
        verify(eventMapper).toDto(second, userId)
    }

    @Test
    fun `getEventDetailById returns mapped detail with recurring dates`() {
        val eventId = UUID.randomUUID()
        val parentId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = createEvent(id = eventId, parentEventId = parentId)
        val firstRecurringId = UUID.randomUUID()
        val secondRecurringId = UUID.randomUUID()
        val recurringDates = listOf(
            RecurringDateInfo("2026-05-01", firstRecurringId.toString()),
            RecurringDateInfo("2026-05-15", secondRecurringId.toString())
        )
        val detail = createDetailDto(eventId)

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(eventRepository.findByParentEventId(parentId)).thenReturn(
            listOf(
                createEvent(id = firstRecurringId, parentEventId = parentId, eventDate = LocalDate.of(2026, 5, 1)),
                createEvent(id = secondRecurringId, parentEventId = parentId, eventDate = LocalDate.of(2026, 5, 15))
            )
        )
        whenever(eventMapper.toDetailData(event, userId, recurringDates)).thenReturn(detail)

        val result = service.getEventDetailById(eventId, userId)

        assertEquals(detail, result)
        verify(eventRepository).findByParentEventId(parentId)
        verify(eventMapper).toDetailData(event, userId, recurringDates)
    }

    @Test
    fun `getEventDetailById allows organizer to read draft event`() {
        val eventId = UUID.randomUUID()
        val parentId = UUID.randomUUID()
        val organizerId = UUID.randomUUID()
        val draftEvent = createEvent(
            id = eventId,
            parentEventId = parentId,
            organizerId = organizerId,
            status = EventStatus.DRAFT
        )
        val recurringDraftId = UUID.randomUUID()
        val recurringPublishedId = UUID.randomUUID()
        val recurringDates = listOf(
            RecurringDateInfo("2026-05-01", recurringDraftId.toString()),
            RecurringDateInfo("2026-05-15", recurringPublishedId.toString())
        )
        val detail = createDetailDto(eventId)

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent))
        whenever(eventRepository.findByParentEventId(parentId)).thenReturn(
            listOf(
                createEvent(id = recurringDraftId, parentEventId = parentId, eventDate = LocalDate.of(2026, 5, 1), status = EventStatus.DRAFT),
                createEvent(id = recurringPublishedId, parentEventId = parentId, eventDate = LocalDate.of(2026, 5, 15), status = EventStatus.PUBLISHED)
            )
        )
        whenever(eventMapper.toDetailData(draftEvent, organizerId, recurringDates)).thenReturn(detail)

        val result = service.getEventDetailById(eventId, organizerId)

        assertEquals(detail, result)
        verify(eventMapper).toDetailData(draftEvent, organizerId, recurringDates)
    }

    @Test
    fun `getEventDetailById throws when non organizer reads non published event`() {
        val eventId = UUID.randomUUID()
        val event = createEvent(id = eventId, status = EventStatus.DRAFT, organizerId = UUID.randomUUID())

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))

        val exception = assertThrows<IllegalArgumentException> {
            service.getEventDetailById(eventId, UUID.randomUUID())
        }

        assertEquals("Event not found with id: $eventId", exception.message)
    }

    @Test
    fun `getEventDetailById throws when event does not exist`() {
        val eventId = UUID.randomUUID()
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            service.getEventDetailById(eventId)
        }

        assertEquals("Event not found with id: $eventId", exception.message)
    }

    private fun createEvent(
        id: UUID,
        parentEventId: UUID? = null,
        eventDate: LocalDate = LocalDate.of(2026, 5, 1),
        organizerId: UUID = UUID.randomUUID(),
        status: EventStatus = EventStatus.PUBLISHED
    ): Event {
        return Event(
            id = id,
            parentEventId = parentEventId,
            organizerId = organizerId,
            organizer = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "p1"),
            eventName = "Event",
            description = "desc",
            eventDate = eventDate,
            endDate = null,
            eventTime = LocalTime.NOON,
            status = status,
            location = null,
            currency = null,
            price = null,
            maxAttendees = null,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createSummaryDto(id: String, name: String): EventSummaryDto {
        return EventSummaryDto(
            id = id,
            organizer = OrganizerDto(userId = UUID.randomUUID().toString(), firstName = null, lastName = null),
            eventName = name,
            description = null,
            date = "2026-05-01",
            endDate = null,
            time = "12:00",
            location = null,
            price = null,
            currency = null,
            maxAttendees = null,
            tags = emptyList(),
            attendees = 0,
            interested = 0,
            promoMedia = null,
            registrationStatus = null,
            status = EventStatus.PUBLISHED,
            registrationType = RegistrationMode.OPEN,
            formId = null
        )
    }

    private fun createDetailDto(eventId: UUID): EventDetailDto {
        return EventDetailDto(
            id = eventId.toString(),
            basicInfo = EventDetailBasicInfo(
                eventName = "Event",
                location = LocationRequest(
                    name = "Studio",
                    street = "Street",
                    city = "City",
                    country = "Country",
                    postalCode = null,
                    houseNumber = null,
                    state = null,
                    county = null
                ),
                date = "2026-05-01",
                time = "12:00",
                price = null,
                currency = null,
                endDate = null,
                organizer = OrganizerDto(userId = UUID.randomUUID().toString(), firstName = null, lastName = null),
                recurringDates = emptyList(),
                status = "PUBLISHED",
                registrationStatus = null,
                registrationType = RegistrationMode.OPEN,
                formId = null
            ),
            additionalDetails = EventDetailAdditionalDetails(
                danceStyles = emptyList(),
                skillLevel = emptyList(),
                typeOfEvent = emptyList(),
                maxAttendees = null
            ),
            description = "desc",
            coverImage = null,
            facebookEventUrl = null,
            media = emptyList(),
            attendeeStats = AttendeeStats(
                going = RegistrationStats(total = 0, leaders = 0, followers = 0),
                interested = 0
            )
        )
    }
}


