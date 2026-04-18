package com.example.bedanceapp.service.user

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.*
import com.example.bedanceapp.model.StatusFilter
import com.example.bedanceapp.repository.EventParentRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.mapping.EventMapper
import com.example.bedanceapp.service.mapping.toOrganizerDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class UserEventService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRepository: EventRepository,
    private val eventParentRepository: EventParentRepository,
    private val eventMapper: EventMapper
) {

    @Transactional(readOnly = true)
    fun getUserEventsPaginated(
        userId: UUID,
        filter: StatusFilter?,
        timeline: EventTimeline?,
        page: Int,
        size: Int
    ): PagedResponse<MyEvent> {
        require(size > 0) { "Size must be greater than 0" }
        val today = LocalDate.now()

        // 1. Fetch raw data
        val registrations = eventRegistrationRepository.findByUserId(userId)
        val organizedIds = eventRepository.findByOrganizerId(userId).mapNotNull { it.id }.toSet()
        val userStatusMap = registrations.associateBy { it.eventId }

        // 2. Filter IDs
        val eventIdsToInclude = when (filter) {
            StatusFilter.HOSTING -> organizedIds.toList()
            StatusFilter.JOINED -> registrations.filter { it.status != RegistrationStatus.INTERESTED && it.eventId !in organizedIds }.map { it.eventId }
            StatusFilter.INTERESTED -> registrations.filter { it.status == RegistrationStatus.INTERESTED && it.eventId !in organizedIds }.map { it.eventId }
            null -> (organizedIds + registrations.map { it.eventId }).distinct()
        }

        // 3. Fetch full objects and group
        val events = eventRepository.findAllById(eventIdsToInclude)
        val allItems = processAndSortEvents(events, userStatusMap, timeline, today)

        // 4. Paginate
        return createPagedResponse(allItems, page.coerceAtLeast(0), size)
    }

    private fun processAndSortEvents(
        events: List<Event>,
        statusMap: Map<UUID, EventRegistration>,
        timeline: EventTimeline?,
        today: LocalDate
    ): List<MyEvent> {
        val (seriesCandidates, standaloneEvents) = events.partition { it.parentEventId != null }

        // Batch fetch parents to avoid N+1
        val parentIds = seriesCandidates.mapNotNull { it.parentEventId }.distinct()
        val parentsMap = eventParentRepository.findAllById(parentIds).associateBy { it.id }

        val childComparator = compareBy<Event> { it.eventDate }.thenBy { it.eventTime }

        // Process Series
        val seriesResults = seriesCandidates
            .groupBy { it.parentEventId!! }
            .mapNotNull { (parentId, children) ->
                val parent = parentsMap[parentId] ?: return@mapNotNull null // Handle orphans by returning null
                val sortedChildren = children.sortedWith(childComparator)

                if (!matchesTimeline(sortedChildren.last().eventDate, null, timeline, today)) return@mapNotNull null

                val anchor = seriesSortAnchor(sortedChildren, timeline, today)
                val dto = SeriesEventDto(
                    id = parentId.toString(),
                    eventName = parent.name,
                    organizer = sortedChildren.first().toOrganizerDto(),
                    overallStartDate = sortedChildren.first().eventDate.toString(),
                    overallEndDate = sortedChildren.last().eventDate.toString(),
                    occurrences = sortedChildren.map { eventMapper.toSingleEventDto(it, statusMap[it.id]) }
                )
                SortableMyEvent(dto, anchor.eventDate, anchor.eventTime)
            }

        // Process Standalone (and Orphans)
        val standaloneResults = (standaloneEvents + findOrphans(seriesCandidates, parentsMap)).filter {
            matchesTimeline(it.eventDate, it.endDate, timeline, today)
        }.map {
            SortableMyEvent(eventMapper.toSingleEventDto(it, statusMap[it.id]), it.eventDate, it.eventTime)
        }

        // Final Sorting
        val finalComparator = compareBy<SortableMyEvent> { it.sortDate }.thenBy { it.sortTime }
        val sortedList = (seriesResults + standaloneResults).let {
            if (timeline == EventTimeline.PAST) it.sortedWith(finalComparator.reversed()) else it.sortedWith(finalComparator)
        }

        return sortedList.map { it.payload }
    }

    private fun findOrphans(seriesEvents: List<Event>, parentsMap: Map<UUID?, EventParent>) =
        seriesEvents.filter { !parentsMap.containsKey(it.parentEventId) }

    private fun matchesTimeline(date: LocalDate, endDate: LocalDate?, timeline: EventTimeline?, today: LocalDate) =
        when (timeline) {
            EventTimeline.UPCOMING -> (endDate ?: date) >= today   // includes ongoing
            EventTimeline.PAST -> (endDate ?: date) < today        // only fully finished
            null -> true
        }

    private fun seriesSortAnchor(children: List<Event>, timeline: EventTimeline?, today: LocalDate) = when (timeline) {
        EventTimeline.PAST -> children.last() // Anchor by most recent occurrence in the past
        EventTimeline.UPCOMING -> children.firstOrNull { it.eventDate >= today } ?: children.first()
        null -> children.first()
    }

    private fun <T> createPagedResponse(items: List<T>, page: Int, size: Int): PagedResponse<T> {
        val totalElements = items.size.toLong()
        val totalPages = if (size == 0) 0 else Math.ceil(totalElements.toDouble() / size).toInt()
        val fromIndex = (page * size).coerceAtMost(items.size)
        val toIndex = (fromIndex + size).coerceAtMost(items.size)

        return PagedResponse(
            content = items.subList(fromIndex, toIndex),
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            isLast = page >= totalPages - 1 || totalPages == 0
        )
    }

    private data class SortableMyEvent(val payload: MyEvent, val sortDate: LocalDate, val sortTime: LocalTime)
}
