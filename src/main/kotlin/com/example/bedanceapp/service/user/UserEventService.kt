package com.example.bedanceapp.service.user

import com.example.bedanceapp.controller.RegistrationStatus
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

    private data class SortableMyEvent(
        val payload: MyEvent,
        val sortDate: LocalDate,
        val sortTime: LocalTime
    )

    @Transactional(readOnly = true)
    fun getUserEventsPaginated(
        userId: UUID,
        filter: StatusFilter?,
        timeline: EventTimeline?,
        page: Int,
        size: Int
    ): PagedResponse<MyEvent> {
        require(size > 0) { "Size must be greater than 0" }

        val safePage = page.coerceAtLeast(0)
        val organizedEvents = eventRepository.findByOrganizerId(userId)
        val organizedIds = organizedEvents.mapNotNull { it.id }.toSet()
        val registrations = eventRegistrationRepository.findByUserId(userId)
        val userStatusMap = buildUserStatusMap(registrations)

        // Determine which event IDs to include based on the filter
        val eventIdsToInclude = when (filter) {
            StatusFilter.HOSTING -> organizedIds.toList()
            StatusFilter.JOINED -> registrations.filter { it.status != RegistrationStatus.INTERESTED && it.eventId !in organizedIds }.map { it.eventId }
            StatusFilter.INTERESTED -> registrations.filter { it.status == RegistrationStatus.INTERESTED && it.eventId !in organizedIds }.map { it.eventId }
            null -> (organizedIds + registrations.map { it.eventId }).distinct()
        }

        // Fetch and filter by timeline
        val events = eventRepository.findAllById(eventIdsToInclude)
        val timelineFiltered = filterByTimeline(events, timeline)
        val allItems = groupEventsBySeries(timelineFiltered, userStatusMap, timeline)

        val totalElements = allItems.size.toLong()
        val fromIndex = (safePage * size).coerceAtMost(allItems.size)
        val toIndex = (fromIndex + size).coerceAtMost(allItems.size)
        val pagedContent = if (fromIndex < toIndex) allItems.subList(fromIndex, toIndex) else emptyList()
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()
        val isLast = totalPages == 0 || safePage >= totalPages - 1

        return PagedResponse(
            content = pagedContent,
            page = safePage,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            isLast = isLast
        )
    }

    private fun filterByTimeline(events: Iterable<Event>, timeline: EventTimeline?): List<Event> {
        if (timeline == null) return events.sortedWith(compareBy<Event> { it.eventDate }.thenBy { it.eventTime })

        val today = LocalDate.now()
        val comparator = compareBy<Event> { it.eventDate }.thenBy { it.eventTime }

        return when (timeline) {
            EventTimeline.UPCOMING ->
                events
                    .filter { it.eventDate >= today }
                    .sortedWith(comparator)
            EventTimeline.PAST ->
                events
                    .filter { it.eventDate < today }
                    .sortedWith(comparator.reversed())
        }
    }


    private fun buildUserStatusMap(registrations: List<EventRegistration>): Map<UUID, EventRegistration> {
        val statusMap = mutableMapOf<UUID, EventRegistration>()

        registrations.forEach { reg ->
            statusMap[reg.eventId] = reg
        }

        return statusMap
    }

    private fun groupEventsBySeries(
        events: Iterable<Event>,
        statusMap: Map<UUID, EventRegistration>,
        timeline: EventTimeline?
    ): List<MyEvent> {
        val (seriesEvents, standaloneEvents) = events.partition { it.parentEventId != null }
        val groupedSeries = seriesEvents.groupBy { it.parentEventId!! }
        val parentIds = groupedSeries.keys
        val parentsById = eventParentRepository.findAllById(parentIds)
            .mapNotNull { parent -> parent.id?.let { id -> id to parent } }
            .toMap()
        val childComparator = compareBy<Event> { it.eventDate }.thenBy { it.eventTime }

        val seriesResults = groupedSeries.mapNotNull { (parentId, children) ->
            val parent = parentsById[parentId] ?: return@mapNotNull null
            val sortedChildren = children.sortedWith(childComparator)

            val seriesDto = SeriesEventDto(
                id = parentId.toString(),
                eventName = parent.name,
                organizer = sortedChildren.first().toOrganizerDto(),
                overallStartDate = sortedChildren.first().eventDate.toString(),
                overallEndDate = sortedChildren.last().eventDate.toString(),
                occurrences = sortedChildren.map { eventMapper.toSingleEventDto(it, statusMap[it.id]?.status) }
            )

            val anchor = if (timeline == EventTimeline.PAST) sortedChildren.last() else sortedChildren.first()
            SortableMyEvent(
                payload = seriesDto,
                sortDate = anchor.eventDate,
                sortTime = anchor.eventTime
            )
        }

        val orphanedChildren = groupedSeries.filterKeys { !parentsById.containsKey(it) }
            .flatMap { it.value }

        val standaloneResults = (standaloneEvents + orphanedChildren).map {
            SortableMyEvent(
                payload = eventMapper.toSingleEventDto(it, statusMap[it.id]?.status),
                sortDate = it.eventDate,
                sortTime = it.eventTime
            )
        }

        val comparator = compareBy<SortableMyEvent> { it.sortDate }.thenBy { it.sortTime }
        val sorted = if (timeline == EventTimeline.PAST) {
            (seriesResults + standaloneResults).sortedWith(comparator.reversed())
        } else {
            (seriesResults + standaloneResults).sortedWith(comparator)
        }

        return sorted.map { it.payload }
    }
}
