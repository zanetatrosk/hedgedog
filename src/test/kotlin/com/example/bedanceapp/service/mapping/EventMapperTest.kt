package com.example.bedanceapp.service.mapping

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.DanceStyle
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.EventType
import com.example.bedanceapp.model.Location
import com.example.bedanceapp.model.Media
import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.MediaService
import com.example.bedanceapp.service.registration.EventRegistrationQueryService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("EventMapper Tests")
class EventMapperTest {

    @Mock private lateinit var mediaService: MediaService
    @Mock private lateinit var eventRegistrationQueryService: EventRegistrationQueryService
    @Mock private lateinit var settingsRepository: EventRegistrationSettingsRepository

    private lateinit var mapper: EventMapper

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mapper = EventMapper(mediaService, eventRegistrationQueryService, settingsRepository)
    }

    @Test
    fun `toDto maps summary and uses OPEN fallback when settings missing`() {
        val eventId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = createEvent(id = eventId)

        whenever(eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId))
            .thenReturn(com.example.bedanceapp.model.EventRegistrationStats(total = 4, leaders = 2, followers = 2))
        whenever(eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)).thenReturn(3)
        whenever(eventRegistrationQueryService.getLastRegistrationByEventIdAndUserId(eventId, userId))
            .thenReturn(EventRegistration(id = UUID.randomUUID(), eventId = eventId, status = RegistrationStatus.REGISTERED, email = "u@e.com"))
        whenever(settingsRepository.findByEventId(eventId)).thenReturn(null)
        whenever(mediaService.mapToDTO(event.promoMedia)).thenReturn(null)

        val dto = mapper.toDto(event, userId)

        assertEquals(eventId.toString(), dto.id)
        assertEquals("Weekly Class", dto.eventName)
        assertEquals(4, dto.attendees)
        assertEquals(3, dto.interested)
        assertEquals(RegistrationMode.OPEN, dto.registrationType)
        assertEquals(RegistrationStatus.REGISTERED, dto.registrationStatus?.status)
        assertEquals(listOf("Bachata", "Intermediate", "Workshop"), dto.tags)
    }

    @Test
    fun `toDetailData maps recurring data and explicit settings`() {
        val eventId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val event = createEvent(id = eventId)
        val recurring = listOf(RecurringDateInfo(date = "2026-05-01", id = eventId.toString()))

        whenever(eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId))
            .thenReturn(com.example.bedanceapp.model.EventRegistrationStats(total = 6, leaders = 3, followers = 3))
        whenever(eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)).thenReturn(1)
        whenever(eventRegistrationQueryService.getLastRegistrationByEventIdAndUserId(eventId, userId))
            .thenReturn(EventRegistration(id = UUID.randomUUID(), eventId = eventId, status = RegistrationStatus.PENDING, email = "u@e.com"))
        whenever(settingsRepository.findByEventId(eventId)).thenReturn(
            EventRegistrationSettings(
                eventId = eventId,
                registrationMode = RegistrationMode.GOOGLE_FORM,
                formId = "form-123",
                formStructure = "{}",
                requireApproval = true
            )
        )
        whenever(mediaService.mapToDTO(any())).thenReturn(null)

        val dto = mapper.toDetailData(event, userId, recurring)

        assertEquals(eventId.toString(), dto.id)
        assertEquals("GOOGLE_FORM", dto.basicInfo.registrationType.name)
        assertEquals("form-123", dto.basicInfo.formId)
        assertEquals(recurring, dto.basicInfo.recurringDates)
        assertEquals(6, dto.attendeeStats.going.total)
        assertEquals(1, dto.attendeeStats.interested)
    }

    @Test
    fun `toSingleEventDto maps user status and role`() {
        val eventId = UUID.randomUUID()
        val event = createEvent(id = eventId)
        val registration = EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            status = RegistrationStatus.WAITLISTED,
            role = DancerRole(id = UUID.randomUUID(), name = "Follower"),
            email = "u@e.com"
        )

        whenever(eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId))
            .thenReturn(com.example.bedanceapp.model.EventRegistrationStats(total = 8, leaders = 4, followers = 4))
        whenever(eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)).thenReturn(2)

        val dto = mapper.toSingleEventDto(event, registration)

        assertEquals(eventId.toString(), dto.id)
        assertEquals(RegistrationStatus.WAITLISTED, dto.userStatus)
        assertEquals("Follower", dto.role?.name)
        assertEquals(8, dto.attendeeStats.going.total)
    }

    @Test
    fun `toDto throws when event id is null`() {
        val event = createEvent(id = null)

        val exception = assertThrows<IllegalStateException> {
            mapper.toDto(event, null)
        }

        assertEquals("Event ID cannot be null for mapping", exception.message)
    }

    @Test
    fun `toDetailData leaves registration status null when user id not provided`() {
        val eventId = UUID.randomUUID()
        val event = createEvent(id = eventId)

        whenever(eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId))
            .thenReturn(com.example.bedanceapp.model.EventRegistrationStats(total = 0, leaders = 0, followers = 0))
        whenever(eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)).thenReturn(0)
        whenever(settingsRepository.findByEventId(eventId)).thenReturn(null)
        whenever(mediaService.mapToDTO(any())).thenReturn(null)

        val dto = mapper.toDetailData(event, null, emptyList())

        assertNull(dto.basicInfo.registrationStatus)
    }

    private fun createEvent(id: UUID?): Event {
        val organizerId = UUID.randomUUID()
        val organizer = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "p1")
        return Event(
            id = id,
            organizerId = organizerId,
            organizer = organizer,
            eventName = "Weekly Class",
            description = "Description",
            eventDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 1),
            eventTime = LocalTime.of(19, 0),
            locationId = UUID.randomUUID(),
            location = Location(id = UUID.randomUUID(), name = "Studio", city = "Warsaw", country = "Poland"),
            currency = com.example.bedanceapp.model.Currency(code = "EUR", name = "Euro", symbol = "EUR"),
            price = BigDecimal("10.00"),
            maxAttendees = 30,
            status = EventStatus.PUBLISHED,
            facebookEventUrl = "https://facebook.com/event",
            promoMediaId = UUID.randomUUID(),
            promoMedia = Media(id = UUID.randomUUID(), mediaType = "image", filePath = "/tmp/promo.jpg"),
            danceStyles = listOf(DanceStyle(id = UUID.randomUUID(), name = "Bachata")),
            skillLevels = listOf(SkillLevel(id = UUID.randomUUID(), name = "Intermediate", levelOrder = 2)),
            typesOfEvents = listOf(EventType(id = UUID.randomUUID(), name = "Workshop")),
            media = mutableListOf(Media(id = UUID.randomUUID(), mediaType = "image", filePath = "/tmp/1.jpg"))
        )
    }
}


