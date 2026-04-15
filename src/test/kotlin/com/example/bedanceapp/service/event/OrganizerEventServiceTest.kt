package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.BasicInfoRequest
import com.example.bedanceapp.model.CreateUpdateEventDto
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.PublishEventDto
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.validation.EventAccessValidator
import com.example.bedanceapp.service.registration.OrganizerRegistrationService
import com.example.bedanceapp.service.registration.RegistrationRecalculateService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("OrganizerEventService Tests")
class OrganizerEventServiceTest {

    @Mock private lateinit var eventRepository: EventRepository
    @Mock private lateinit var eventAssembler: EventAssembler
    @Mock private lateinit var organizerRegistrationService: OrganizerRegistrationService
    @Mock private lateinit var registrationRecalculateService: RegistrationRecalculateService
    @Mock private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var createEventService: CreateEventService
    @Mock private lateinit var eventAccessValidator: EventAccessValidator

    private lateinit var service: OrganizerEventService

    private val organizerId = UUID.randomUUID()
    private val eventId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = OrganizerEventService(
            eventRepository,
            eventAssembler,
            organizerRegistrationService,
            registrationRecalculateService,
            eventRegistrationSettingsRepository,
            eventRegistrationRepository,
            createEventService,
            eventAccessValidator
        )
        whenever(eventRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createEventByOccurrence delegates to createEventService`() {
        val request = createRequest()
        val created = listOf(createEvent(status = EventStatus.DRAFT))
        whenever(createEventService.createSingleEventOrRecurringEvents(request, organizerId)).thenReturn(created)

        val result = service.createEventByOccurrence(request, organizerId)

        assertEquals(created, result)
        verify(createEventService).createSingleEventOrRecurringEvents(request, organizerId)
    }

    @Test
    fun `updateEvent recalculates capacity when changed and saves updated event`() {
        val existing = createEvent(status = EventStatus.PUBLISHED, maxAttendees = 10)
        val request = createRequest(maxAttendees = 12)
        val built = existing.copy(maxAttendees = 12)

        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId)).thenReturn(existing)
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(listOf(createRegistration()))
        whenever(eventAssembler.buildEventFromRequest(
            request,
            organizerId,
            request.basicInfo.date,
            existing.parentEventId,
            existing.status,
            eventId
        )).thenReturn(built)

        val result = service.updateEvent(eventId, request, organizerId)

        assertEquals(12, result.maxAttendees)
        verify(registrationRecalculateService).recalculate(eventId, 12)
        verify(eventRepository).save(built)
    }

    @Test
    fun `updateEvent does not recalculate when capacity stays unchanged`() {
        val existing = createEvent(status = EventStatus.PUBLISHED, maxAttendees = 10)
        val request = createRequest(maxAttendees = 10)
        val built = existing.copy(eventName = "Updated Name")

        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId)).thenReturn(existing)
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(listOf(createRegistration()))
        whenever(
            eventAssembler.buildEventFromRequest(
                request,
                organizerId,
                request.basicInfo.date,
                existing.parentEventId,
                existing.status,
                eventId
            )
        ).thenReturn(built)

        val result = service.updateEvent(eventId, request, organizerId)

        assertEquals("Updated Name", result.eventName)
        verify(registrationRecalculateService, never()).recalculate(any(), any())
        verify(eventRepository).save(built)
    }

    @Test
    fun `updateEvent throws when lowering capacity below confirmed attendees`() {
        val existing = createEvent(status = EventStatus.PUBLISHED, maxAttendees = 10)
        val request = createRequest(maxAttendees = 1)
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId)).thenReturn(existing)
        whenever(eventRegistrationRepository.findByEventId(eventId)).thenReturn(
            listOf(
                createRegistration(),
                createRegistration()
            )
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.updateEvent(eventId, request, organizerId)
        }

        assertEquals(
            "Cannot lower capacity to 1. You already have 2 confirmed attendees. Please cancel some attendees before lowering the limit.",
            exception.message
        )
        verify(eventRepository, never()).save(any())
    }

    @Test
    fun `publishEvent saves settings and publishes event for open mode`() {
        val draft = createEvent(status = EventStatus.DRAFT)
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)).thenReturn(draft)

        val result = service.publishEvent(eventId, organizerId)

        assertEquals(EventStatus.PUBLISHED, result.status)
        verify(eventRegistrationSettingsRepository).save(any())
        verify(organizerRegistrationService, never()).syncGoogleFormData(any(), any())
        verify(eventRepository).save(any())
    }

    @Test
    fun `publishEvent throws when google form mode does not include form id`() {
        val draft = createEvent(status = EventStatus.DRAFT)
        val request = PublishEventDto(
            registrationMode = RegistrationMode.GOOGLE_FORM,
            formId = null,
            requireApproval = false
        )
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)).thenReturn(draft)

        val exception = assertThrows<IllegalArgumentException> {
            service.publishEvent(eventId, organizerId, request)
        }

        assertEquals("Form ID must be provided when using GOOGLE_FORM registration mode.", exception.message)
        verify(eventRegistrationSettingsRepository, never()).save(any())
    }

    @Test
    fun `publishEvent wraps sync exception for google form mode`() {
        val draft = createEvent(status = EventStatus.DRAFT)
        val request = PublishEventDto(
            registrationMode = RegistrationMode.GOOGLE_FORM,
            formId = "form-1",
            requireApproval = false
        )
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)).thenReturn(draft)
        whenever(organizerRegistrationService.syncGoogleFormData(eventId, organizerId)).thenThrow(RuntimeException("timeout"))

        val exception = assertThrows<IllegalArgumentException> {
            service.publishEvent(eventId, organizerId, request)
        }

        assertEquals("Failed to fetch form structure from Google Forms: timeout", exception.message)
    }

    @Test
    fun `publishEvent throws when event date is in the past`() {
        val draftInPast = createEvent(
            status = EventStatus.DRAFT,
            eventDate = LocalDate.now().minusDays(1)
        )
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)).thenReturn(draftInPast)

        val exception = assertThrows<IllegalArgumentException> {
            service.publishEvent(eventId, organizerId)
        }

        assertEquals("Event date must be today or in the future.", exception.message)
        verify(eventRegistrationSettingsRepository, never()).save(any())
        verify(organizerRegistrationService, never()).syncGoogleFormData(any(), any())
        verify(eventRepository, never()).save(any())
    }

    @Test
    fun `cancelEvent changes status to cancelled`() {
        val event = createEvent(status = EventStatus.PUBLISHED)
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT, EventStatus.PUBLISHED)).thenReturn(event)

        val result = service.cancelEvent(eventId, organizerId)

        assertEquals(EventStatus.CANCELLED, result.status)
        verify(eventRepository).save(any())
    }

    @Test
    fun `deleteEvent removes draft event`() {
        val event = createEvent(status = EventStatus.DRAFT)
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)).thenReturn(event)

        service.deleteEvent(eventId, organizerId)

        verify(eventRepository).delete(event)
    }

    private fun createRequest(maxAttendees: Int? = null): CreateUpdateEventDto {
        return CreateUpdateEventDto(
            basicInfo = BasicInfoRequest(
                eventName = "Event",
                location = null,
                date = LocalDate.now().plusDays(1),
                time = LocalTime.NOON,
                endDate = null,
                isRecurring = false,
                recurrenceType = null,
                recurrenceEndDate = null,
                price = null,
                currency = null
            ),
            additionalDetails = com.example.bedanceapp.model.AdditionalDetailsRequest(
                maxAttendees = maxAttendees,
                facebookEventUrl = null
            ),
            description = "desc",
            coverImage = null,
            media = null
        )
    }

    private fun createEvent(
        status: EventStatus,
        maxAttendees: Int? = null,
        eventDate: LocalDate = LocalDate.now().plusDays(2)
    ): Event {
        return Event(
            id = eventId,
            organizerId = organizerId,
            organizer = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "p1"),
            eventName = "Event",
            description = "desc",
            eventDate = eventDate,
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = null,
            maxAttendees = maxAttendees,
            status = status,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createRegistration(): EventRegistration {
        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            userId = UUID.randomUUID(),
            user = null,
            status = RegistrationStatus.REGISTERED,
            roleId = null,
            role = null,
            email = "a@b.com",
            isAnonymous = false,
            responseId = null,
            formResponses = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            waitlistedAt = null
        )
    }
}


