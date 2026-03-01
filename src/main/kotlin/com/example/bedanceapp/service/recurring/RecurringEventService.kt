package com.example.bedanceapp.service.recurring

import com.example.bedanceapp.model.*
import com.example.bedanceapp.repository.EventParentRepository
import com.example.bedanceapp.repository.EventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

/**
 * Service responsible for handling recurring event operations.
 * This service encapsulates all logic related to creating and managing recurring events.
 */
@Service
class RecurringEventService(
    private val recurringEventGenerator: RecurringEventGenerator,
    private val eventParentRepository: EventParentRepository,
    private val eventRepository: EventRepository
) {

    companion object {
        /**
         * Maximum number of recurring events that can be generated in a single series.
         */
        const val MAX_RECURRING_EVENTS = 30
    }

    /**
     * Creates a series of recurring events based on the provided request.
     * If the event is not recurring, creates a single event.
     *
     * @param request The event creation request
     * @param organizerId The ID of the user creating the events
     * @param createEventFn Function to create individual events (injected to avoid circular dependency)
     * @return List of created events
     */
    @Transactional
    fun createRecurringEvents(
        request: CreateEventRequest,
        organizerId: UUID,
        createEventFn: (CreateEventRequest, UUID, LocalDate?, UUID?) -> Event
    ): List<Event> {
        // If not recurring, create a single event
        if (request.basicInfo.isRecurring != true) {
            return listOf(createEventFn(request, organizerId, null, null))
        }

        // Validate recurring event requirements
        validateRecurringEventRequest(request)

        val startDate = LocalDate.parse(request.basicInfo.date)

        // Use recurrenceEndDate if provided, otherwise fall back to endDate
        // This is for generating the recurrence dates, not for storing
        val recurrenceEndDateString = if (!request.basicInfo.recurrenceEndDate.isNullOrBlank()) {
            request.basicInfo.recurrenceEndDate
        } else {
            throw IllegalArgumentException("Recurrence end date is required for recurring events")
        }
        val recurrenceEndDate = LocalDate.parse(recurrenceEndDateString)
        val recurrenceType = request.basicInfo.recurrenceType!!

        // Generate dates based on recurrence type
        val dates = recurringEventGenerator.generateDates(startDate, recurrenceEndDate, recurrenceType)

        if( dates.size > MAX_RECURRING_EVENTS) {
            throw IllegalArgumentException("You cannot generate more then $MAX_RECURRING_EVENTS recurring events")
        }

        // Create parent event for grouping (don't store recurrence end date)
        val parent = eventParentRepository.save(
            EventParent(name = request.basicInfo.eventName)
        )

        // Create individual events for each date
        val events = mutableListOf<Event>()
        for (date in dates) {
            val event = createEventFn(request, organizerId, date, parent.id)
            events.add(event)
        }

        return events
    }

    /**
     * Validates that a recurring event request has all required fields.
     */
    private fun validateRecurringEventRequest(request: CreateEventRequest) {
        if (request.basicInfo.recurrenceEndDate.isNullOrBlank()) {
            throw IllegalArgumentException("End date is required for recurring events")
        }

        if (request.basicInfo.recurrenceType == null) {
            throw IllegalArgumentException("Recurrence type is required for recurring events")
        }

        val startDate = LocalDate.parse(request.basicInfo.date)
        val endDate = LocalDate.parse(request.basicInfo.recurrenceEndDate)

        if (startDate.isAfter(endDate)) {
            throw IllegalArgumentException("Start date must be before or equal to end date")
        }
    }

    /**
     * Retrieves all upcoming dates for events that share the same parent (recurring series).
     *
     * @param parentEventId The ID of the parent event
     * @return List of recurring date information sorted by date
     */
    fun getUpcomingDates(parentEventId: UUID?): List<RecurringDateInfo> {
        if (parentEventId == null) {
            return emptyList()
        }

        val siblingEvents = eventRepository.findByParentEventId(parentEventId)
        return siblingEvents
            .map { sibling ->
                RecurringDateInfo(
                    date = sibling.eventDate.toString(),
                    id = sibling.id.toString()
                )
            }
            .sortedBy { it.date }
    }
}

/**
 * Data class representing recurrence information for an event.
 */
data class RecurrenceInfo(
    val parentId: String,
    val seriesName: String,
    val totalOccurrences: Int,
    val upcomingDates: List<RecurringDateInfo>
)

