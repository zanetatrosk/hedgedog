package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.BasicInfoRequest
import com.example.bedanceapp.model.CreateUpdateEventDto
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventParent
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.RecurrenceType
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventParentRepository
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
import kotlin.test.assertTrue

@DisplayName("CreateEventService Tests")
class CreateEventServiceTest {

    @Mock private lateinit var recurringEventGenerator: RecurringEventGenerator
    @Mock private lateinit var eventParentRepository: EventParentRepository
    @Mock private lateinit var eventAssembler: EventAssembler

    private lateinit var service: CreateEventService

    private val organizerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = CreateEventService(
            recurringEventGenerator,
            eventParentRepository,
            eventAssembler
        )
    }

    @Test
    fun `createSingleEventOrRecurringEvents returns single event when not recurring`() {
        val request = createRequest(isRecurring = false)
        val builtEvent = createEvent(date = request.basicInfo.date)
        whenever(eventAssembler.buildEventFromRequest(request, organizerId)).thenReturn(builtEvent)

        val result = service.createSingleEventOrRecurringEvents(request, organizerId)

        assertEquals(listOf(builtEvent), result)
        verify(recurringEventGenerator, never()).generateDates(any(), any(), any())
        verify(eventParentRepository, never()).save(any())
    }

    @Test
    fun `createSingleEventOrRecurringEvents creates recurring events and saves them`() {
        val request = createRequest(
            isRecurring = true,
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceEndDate = LocalDate.of(2026, 5, 15)
        )
        val generatedDates = listOf(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 8),
            LocalDate.of(2026, 5, 15)
        )
        val parentId = UUID.randomUUID()
        val parent = EventParent(id = parentId, name = request.basicInfo.eventName)

        whenever(recurringEventGenerator.generateDates(request.basicInfo.date, request.basicInfo.recurrenceEndDate!!, request.basicInfo.recurrenceType!!))
            .thenReturn(generatedDates)
        whenever(eventParentRepository.save(any())).thenReturn(parent)

        val builtEvents = generatedDates.map { date -> createEvent(date = date, parentEventId = parentId) }
        generatedDates.forEachIndexed { index, date ->
            whenever(eventAssembler.buildEventFromRequest(request, organizerId, date, parentId)).thenReturn(builtEvents[index])
        }

        val result = service.createSingleEventOrRecurringEvents(request, organizerId)

        assertEquals(3, result.size)
        assertTrue(result.all { it.parentEventId == parentId })
        verify(eventParentRepository).save(any())
        generatedDates.forEach { date ->
            verify(eventAssembler).buildEventFromRequest(request, organizerId, date, parentId)
        }
    }

    @Test
    fun `createSingleEventOrRecurringEvents throws when recurrence start is after end`() {
        val request = createRequest(
            isRecurring = true,
            date = LocalDate.of(2026, 6, 4),
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceEndDate = LocalDate.of(2026, 4, 27)
        )

        val exception = assertThrows<IllegalArgumentException> {
            service.createSingleEventOrRecurringEvents(request, organizerId)
        }

        assertEquals(
            "Start date (2026-06-04) must be before or equal to recurrence end date (2026-04-27)",
            exception.message
        )
        verify(recurringEventGenerator, never()).generateDates(any(), any(), any())
    }

    @Test
    fun `createSingleEventOrRecurringEvents throws when generated recurrences exceed max limit`() {
        val request = createRequest(
            isRecurring = true,
            recurrenceType = RecurrenceType.DAILY,
            recurrenceEndDate = LocalDate.of(2026, 5, 31)
        )
        val tooManyDates = (0..CreateEventService.MAX_RECURRING_EVENTS).map { LocalDate.of(2026, 5, 1).plusDays(it.toLong()) }

        whenever(recurringEventGenerator.generateDates(request.basicInfo.date, request.basicInfo.recurrenceEndDate!!, request.basicInfo.recurrenceType!!))
            .thenReturn(tooManyDates)

        val exception = assertThrows<IllegalArgumentException> {
            service.createSingleEventOrRecurringEvents(request, organizerId)
        }

        assertEquals(
            "You cannot generate more than 30 recurring events at once.",
            exception.message
        )
        verify(eventParentRepository, never()).save(any())
    }


    private fun createRequest(
        isRecurring: Boolean? = false,
        date: LocalDate = LocalDate.of(2026, 5, 1),
        recurrenceType: RecurrenceType? = null,
        recurrenceEndDate: LocalDate? = null
    ): CreateUpdateEventDto {
        return CreateUpdateEventDto(
            basicInfo = BasicInfoRequest(
                eventName = "Test Event",
                location = null,
                date = date,
                time = LocalTime.NOON,
                endDate = null,
                isRecurring = isRecurring,
                recurrenceType = recurrenceType,
                recurrenceEndDate = recurrenceEndDate,
                price = null,
                currency = null
            ),
            additionalDetails = null,
            description = "desc",
            coverImage = null,
            media = null
        )
    }

    private fun createEvent(
        id: UUID = UUID.randomUUID(),
        date: LocalDate,
        parentEventId: UUID? = null,
        organizer: User = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "p1")
    ): Event {
        return Event(
            id = id,
            parentEventId = parentEventId,
            organizerId = organizerId,
            organizer = organizer,
            eventName = "Test Event",
            description = "desc",
            eventDate = date,
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = null,
            maxAttendees = null,
            status = EventStatus.DRAFT,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}


