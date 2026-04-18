package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.*
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.mapping.EventMapper
import com.example.bedanceapp.specification.EventSpecification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val eventMapper: EventMapper
) {

    @Transactional(readOnly = true)
    fun getAllPublishedEventsPaginated(
        userId: UUID? = null,
        pageable: Pageable,
        eventName: String? = null,
        city: String? = null,
        country: String? = null,
        danceStyleIds: List<UUID>? = null,
        eventTypeIds: List<UUID>? = null,
        includeCancelled: Boolean = true
    ): Page<EventSummaryDto> {
        val specification = EventSpecification.buildSpecificationForPublicEvents(
            includeCancelled = includeCancelled,
            eventName = eventName,
            city = city,
            country = country,
            danceStyleIds = danceStyleIds,
            eventTypeIds = eventTypeIds
        )
        val eventsPage = eventRepository.findAll(specification, pageable)
        return eventsPage.map { event -> eventMapper.toDto(event, userId) }
    }

    @Transactional(readOnly = true)
    fun getEventDetailById(eventId: UUID, userId: UUID? = null): EventDetailDto {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }
        val isUserOrganizer = event.organizerId == userId
        require(event.status != EventStatus.DRAFT || isUserOrganizer){
            "Event not found with id: $eventId"
        }
        val parentEventId = event.parentEventId
        val recurringDates = getUpcomingDates(parentEventId, isUserOrganizer)
        return eventMapper.toDetailData(event, userId, recurringDates)
    }

    @Transactional(readOnly = true)
    fun getUpcomingDates(parentEventId: UUID?, isUserOrganizer: Boolean): List<RecurringDateInfo> {
        return parentEventId?.let { id ->
            eventRepository.findByParentEventId(id)
                .filter {isUserOrganizer || it.status != EventStatus.DRAFT }
                .map { RecurringDateInfo(date = it.eventDate.toString(), id = it.id.toString()) }
                .sortedBy { it.date }
        } ?: emptyList()
    }
}
