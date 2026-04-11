package com.example.bedanceapp.service.user

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.AttendeeStats
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventParent
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.EventTimeline
import com.example.bedanceapp.model.MyEventDisplayMode
import com.example.bedanceapp.model.RegistrationStats
import com.example.bedanceapp.model.SeriesEventDto
import com.example.bedanceapp.model.SingleEventDTO
import com.example.bedanceapp.model.StatusFilter
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.dto.OrganizerDto
import com.example.bedanceapp.repository.EventParentRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.mapping.EventMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("UserEventService Tests")
class UserEventServiceTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var eventRepository: EventRepository
    @Mock private lateinit var eventParentRepository: EventParentRepository
    @Mock private lateinit var eventMapper: EventMapper

    private lateinit var service: UserEventService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = UserEventService(
            eventRegistrationRepository,
            eventRepository,
            eventParentRepository,
            eventMapper
        )
    }

    @Test
    fun `getUserEventsPaginated throws when size is not positive`() {
        val exception = assertThrows<IllegalArgumentException> {
            service.getUserEventsPaginated(
                userId = UUID.randomUUID(),
                filter = null,
                timeline = null,
                page = 0,
                size = 0
            )
        }

        assertEquals("Size must be greater than 0", exception.message)
    }

    @Test
    fun `getUserEventsPaginated returns upcoming series when at least one occurrence is future`() {
        val userId = UUID.randomUUID()
        val parentId = UUID.randomUUID()
        val today = LocalDate.now()
        val childPast = createEvent(id = UUID.randomUUID(), userId = userId, date = today.minusDays(1), parentId = parentId)
        val childFuture = createEvent(id = UUID.randomUUID(), userId = userId, date = today.plusDays(1), parentId = parentId)
        val singleLater = createEvent(id = UUID.randomUUID(), userId = userId, date = today.plusDays(2))

        whenever(eventRegistrationRepository.findByUserId(userId)).thenReturn(emptyList())
        whenever(eventRepository.findByOrganizerId(userId)).thenReturn(listOf(childPast, childFuture, singleLater))
        whenever(eventRepository.findAllById(any<Iterable<UUID>>())).thenReturn(listOf(childPast, childFuture, singleLater))
        whenever(eventParentRepository.findAllById(listOf(parentId))).thenReturn(listOf(EventParent(id = parentId, name = "Weekly Series")))
        whenever(eventMapper.toSingleEventDto(childPast, null)).thenReturn(singleDto(childPast.id.toString(), childPast.eventName))
        whenever(eventMapper.toSingleEventDto(childFuture, null)).thenReturn(singleDto(childFuture.id.toString(), childFuture.eventName))
        whenever(eventMapper.toSingleEventDto(singleLater, null)).thenReturn(singleDto(singleLater.id.toString(), singleLater.eventName))

        val result = service.getUserEventsPaginated(
            userId = userId,
            filter = StatusFilter.HOSTING,
            timeline = EventTimeline.UPCOMING,
            page = 0,
            size = 10
        )

        assertEquals(2, result.content.size)
        assertTrue(result.content.first() is SeriesEventDto)
        val firstSeries = result.content.first() as SeriesEventDto
        assertEquals(MyEventDisplayMode.SERIES, firstSeries.displayMode)
        assertEquals(2, firstSeries.occurrences.size)
    }

    @Test
    fun `getUserEventsPaginated sorts past by most recent anchor first`() {
        val userId = UUID.randomUUID()
        val parentId = UUID.randomUUID()
        val today = LocalDate.now()

        val seriesOld = createEvent(id = UUID.randomUUID(), userId = userId, date = today.minusDays(10), parentId = parentId)
        val seriesRecent = createEvent(id = UUID.randomUUID(), userId = userId, date = today.minusDays(2), parentId = parentId)
        val singlePast = createEvent(id = UUID.randomUUID(), userId = userId, date = today.minusDays(3))

        whenever(eventRegistrationRepository.findByUserId(userId)).thenReturn(emptyList())
        whenever(eventRepository.findByOrganizerId(userId)).thenReturn(listOf(seriesOld, seriesRecent, singlePast))
        whenever(eventRepository.findAllById(any<Iterable<UUID>>())).thenReturn(listOf(seriesOld, seriesRecent, singlePast))
        whenever(eventParentRepository.findAllById(listOf(parentId))).thenReturn(listOf(EventParent(id = parentId, name = "Series")))
        whenever(eventMapper.toSingleEventDto(seriesOld, null)).thenReturn(singleDto(seriesOld.id.toString(), seriesOld.eventName))
        whenever(eventMapper.toSingleEventDto(seriesRecent, null)).thenReturn(singleDto(seriesRecent.id.toString(), seriesRecent.eventName))
        whenever(eventMapper.toSingleEventDto(singlePast, null)).thenReturn(singleDto(singlePast.id.toString(), singlePast.eventName))

        val result = service.getUserEventsPaginated(
            userId = userId,
            filter = StatusFilter.HOSTING,
            timeline = EventTimeline.PAST,
            page = 0,
            size = 10
        )

        assertEquals(2, result.content.size)
        assertTrue(result.content.first() is SeriesEventDto)
        val firstSeries = result.content.first() as SeriesEventDto
        assertEquals(today.minusDays(10).toString(), firstSeries.overallStartDate)
        assertEquals(today.minusDays(2).toString(), firstSeries.overallEndDate)
    }

    private fun createEvent(id: UUID, userId: UUID, date: LocalDate, parentId: UUID? = null): Event {
        return Event(
            id = id,
            parentEventId = parentId,
            organizerId = userId,
            organizer = User(id = userId, email = "org@example.com", provider = "google", providerId = "p1"),
            eventName = "Event-$id",
            description = null,
            eventDate = date,
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = null,
            maxAttendees = null,
            status = EventStatus.PUBLISHED,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun singleDto(id: String, name: String): SingleEventDTO {
        return SingleEventDTO(
            id = id,
            eventName = name,
            organizer = OrganizerDto(userId = UUID.randomUUID().toString(), firstName = null, lastName = null),
            userStatus = RegistrationStatus.REGISTERED,
            status = EventStatus.PUBLISHED,
            role = null,
            date = LocalDate.now().toString(),
            time = "12:00",
            location = null,
            attendeeStats = AttendeeStats(
                going = RegistrationStats(total = 0, leaders = 0, followers = 0),
                interested = 0
            )
        )
    }
}

