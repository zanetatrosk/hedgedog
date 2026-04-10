package com.example.bedanceapp.service.event

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
class CreateEventService(
    private val recurringEventGenerator: RecurringEventGenerator,
    private val eventParentRepository: EventParentRepository,
    private val eventRepository: EventRepository,
    private val eventAssembler: EventAssembler
) {

    companion object {
        const val MAX_RECURRING_EVENTS = 30
    }

    @Transactional
    fun createSingleEventOrRecurringEvents(
        request: CreateUpdateEventDto,
        organizerId: UUID
    ): List<Event> {
        // 1. Guard Clause: If not recurring, keep it simple
        if (request.basicInfo.isRecurring != true) {
            return listOf(eventAssembler.buildEventFromRequest(request, organizerId))
        }

        // 2. Validation
        val basicInfo = request.basicInfo
        validateRecurrence(basicInfo)

        // 3. Generate Dates
        val dates = recurringEventGenerator.generateDates(
            basicInfo.date,
            basicInfo.recurrenceEndDate!!,
            basicInfo.recurrenceType!!
        )

        require(dates.size <= MAX_RECURRING_EVENTS) {
            "You cannot generate more than $MAX_RECURRING_EVENTS recurring events at once."
        }

        // 4. Create grouping parent
        val parent = eventParentRepository.save(EventParent(name = basicInfo.eventName))

        // 5. Create individual events using functional map
        return eventRepository.saveAll(dates.map { date ->
            eventAssembler.buildEventFromRequest(request, organizerId, date, parent.id)
        })
    }

    private fun validateRecurrence(info: BasicInfoRequest) {
        requireNotNull(info.recurrenceEndDate) { "Recurrence end date is required for recurring events" }
        requireNotNull(info.recurrenceType) { "Recurrence type is required for recurring events" }

        require(!info.date.isAfter(info.recurrenceEndDate)) {
            "Start date (${info.date}) must be before or equal to recurrence end date (${info.recurrenceEndDate})"
        }
    }


    @Transactional(readOnly = true)
    fun getUpcomingDates(parentEventId: UUID?): List<RecurringDateInfo> {
        return parentEventId?.let { id ->
            eventRepository.findByParentEventId(id)
                .map { RecurringDateInfo(date = it.eventDate.toString(), id = it.id.toString()) }
                .sortedBy { it.date }
        } ?: emptyList()
    }
}

