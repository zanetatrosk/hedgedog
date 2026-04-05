package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.*
import com.example.bedanceapp.model.StatusFilter
import com.example.bedanceapp.repository.EventParentRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.get

@Service
class UserEventService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRepository: EventRepository,
    private val eventParentRepository: EventParentRepository,
    private val eventMapper: EventMapper
) {

    @Transactional(readOnly = true)
    fun getUserEvents(userId: UUID, filter: StatusFilter?): List<MyEvent> {
        val organizedEvents = eventRepository.findByOrganizerId(userId)
        val organizedIds = organizedEvents.mapNotNull { it.id }.toSet()
        val registrations = eventRegistrationRepository.findByUserId(userId)

        // 1. Build a map of RSVP statuses for the user
        val userStatusMap = buildUserStatusMap(organizedIds, registrations)

        // 2. Determine which event IDs to include based on the filter
        val eventIdsToInclude = when (filter) {
            StatusFilter.HOSTING -> organizedIds.toList()
            StatusFilter.JOINED -> registrations.filter { it.status == RegistrationStatus.REGISTERED && it.eventId !in organizedIds }.map { it.eventId }
            StatusFilter.INTERESTED -> registrations.filter { it.status == RegistrationStatus.INTERESTED && it.eventId !in organizedIds }.map { it.eventId }
            null -> (organizedIds + registrations.map { it.eventId }).distinct()
        }

        // 3. Fetch and group events
        val events = eventRepository.findAllById(eventIdsToInclude)
        return groupEventsBySeries(events, userStatusMap)
    }

    private fun buildUserStatusMap(organizedIds: Set<UUID>, registrations: List<EventRegistration>): Map<UUID, RsvpStatus> {
        val statusMap = mutableMapOf<UUID, RsvpStatus>()

        // Priority order: Hosting > Registered > Waitlisted > Interested
        // We iterate in reverse priority so higher priorities overwrite lower ones
        registrations.forEach { reg ->
            statusMap[reg.eventId] = when (reg.status) {
                RegistrationStatus.REGISTERED -> RsvpStatus.REGISTERED
                RegistrationStatus.WAITLISTED -> RsvpStatus.WAITLISTED
                RegistrationStatus.INTERESTED -> RsvpStatus.INTERESTED
                else -> statusMap[reg.eventId] // Keep existing if status doesn't match
            }!!
        }

        organizedIds.forEach { statusMap[it] = RsvpStatus.HOSTING }

        return statusMap
    }

    private fun groupEventsBySeries(events: Iterable<Event>, statusMap: Map<UUID, RsvpStatus>): List<MyEvent> {
        val (seriesEvents, standaloneEvents) = events.partition { it.parentEventId != null }
        val groupedSeries = seriesEvents.groupBy { it.parentEventId!! }

        val seriesResults = groupedSeries.mapNotNull { (parentId, children) ->
            val parent = eventParentRepository.findById(parentId).orElse(null) ?: return@mapNotNull null
            val sortedChildren = children.sortedBy { it.eventDate }

            SeriesEventDTO(
                id = parentId.toString(),
                eventName = parent.name,
                organizer = sortedChildren.first().toOrganizerDto(),
                overallStartDate = sortedChildren.first().eventDate.toString(),
                overallEndDate = sortedChildren.last().eventDate.toString(),
                occurrences = sortedChildren.map { eventMapper.toSingleEventDTO(it, statusMap[it.id]) }
            )
        }

        val orphanedChildren = groupedSeries.filter { eventParentRepository.findById(it.key).isEmpty }
            .flatMap { it.value }

        val standaloneResults = (standaloneEvents + orphanedChildren).map {
            eventMapper.toSingleEventDTO(it, statusMap[it.id])
        }

        return seriesResults + standaloneResults
    }
}
