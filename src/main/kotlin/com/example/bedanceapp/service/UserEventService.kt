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
    private val eventRegistrationStatsService: EventRegistrationStatsService,
    private val eventRegistrationManager: EventRegistrationManager
) {

    @Transactional(readOnly = true)
    fun getUserEvents(userId: UUID, filter: StatusFilter?): List<MyEvent> {
        // Get events organized by the user
        val organizedEvents = eventRepository.findByOrganizerId(userId)
        val organizedEventIds = organizedEvents.mapNotNull { it.id }

        // Get all registrations for the user
        val allRegistrations = eventRegistrationRepository.findByUserId(userId)
        // Separate registrations by status
        val registeredRegistrations = allRegistrations.filter { it.status == RegistrationStatus.REGISTERED }
        val waitlistedRegistrations = allRegistrations.filter { it.status == RegistrationStatus.WAITLISTED }
        val interestedRegistrations = allRegistrations.filter { it.status == RegistrationStatus.INTERESTED }

        // Filter based on the filter parameter (JOINING filter is not in enum but used in API)
        // We need to handle it as a special case if passed from frontend
        val eventIdsToInclude = when (filter) {
            StatusFilter.HOSTING -> {
                // Only events where user is organizer
                organizedEventIds
            }

            StatusFilter.JOINED -> {
                // Events where user is registered (not hosting)
                registeredRegistrations
                    .map { it.eventId }
                    .filter { it !in organizedEventIds }
            }

            StatusFilter.INTERESTED -> {
                // Events where user is interested (not registered, not hosting)
                interestedRegistrations
                    .map { it.eventId }
                    .filter { it !in organizedEventIds }
            }

            null -> {
                // All events: organized + registered + waitlisted + interested
                (organizedEventIds + allRegistrations.map { it.eventId }).distinct()
            }
        }

        // Build user status map for each event
        val userStatusMap = mutableMapOf<UUID, RsvpStatus>()

        // Mark organized events as HOSTING
        fillMap(userStatusMap, organizedEventIds, RsvpStatus.HOSTING)


        // Mark registered events as REGISTERED (only if not hosting)
        fillMap(userStatusMap, registeredRegistrations.map { it.eventId }, RsvpStatus.REGISTERED)

        // Mark waitlisted events as WAITLISTED (only if not hosting)
        fillMap(userStatusMap, waitlistedRegistrations.map { it.eventId }, RsvpStatus.WAITLISTED)


        // Mark interested events as INTERESTED (only if not hosting, not going, not waitlisted)
        fillMap(userStatusMap, interestedRegistrations.map { it.eventId }, RsvpStatus.INTERESTED)

        return getEventsByStatus(eventIdsToInclude, userStatusMap)
    }

    private fun getEventsByStatus(eventIdsToInclude: List<UUID>, userStatusMap: MutableMap<UUID, RsvpStatus>): List<MyEvent> {
        val result = mutableListOf<MyEvent>()
        // Fetch all events
        val events = eventRepository.findAllById(eventIdsToInclude)

        // Group events by parentEventId
        val eventsWithParent = events.filter { it.parentEventId != null }
        val eventsWithoutParent = events.filter { it.parentEventId == null }

        val groupedByParent = eventsWithParent.groupBy { it.parentEventId }

        // Add series events (events with parent)
        groupedByParent.forEach { (parentId, childEvents) ->
            if (parentId != null && childEvents.isNotEmpty()) {
                // Get the parent event information
                val parentEvent = eventParentRepository.findById(parentId).orElse(null)

                if (parentEvent != null) {
                    val sortedEvents = childEvents.sortedBy { it.eventDate }
                    val occurrences = sortedEvents.map { event ->
                        mapToSingleEventDTO(event, userStatusMap[event.id])
                    }

                    val seriesEvent = SeriesEventDTO(
                        id = parentId.toString(),
                        eventName = parentEvent.name,
                        organizer = mapToOrganizer(childEvents.first()),
                        overallStartDate = sortedEvents.first().eventDate.toString(),
                        overallEndDate = sortedEvents.last().eventDate.toString(),
                        occurrences = occurrences
                    )
                    result.add(seriesEvent)
                } else {
                    // If parent doesn't exist, add as individual events
                    childEvents.forEach { event ->
                        result.add(mapToSingleEventDTO(event, userStatusMap[event.id]))
                    }
                }
            }
        }

        // Add standalone events (events without parent)
        eventsWithoutParent.forEach { event ->
            // This is a standalone event
            result.add(mapToSingleEventDTO(event, userStatusMap[event.id]))
        }

        return result
    }

    private fun mapToSingleEventDTO(event: Event, userStatus: RsvpStatus?): SingleEventDTO {
        val registrations = event.id?.let { eventRegistrationRepository.findByEventIdAndStatus(it, RegistrationStatus.REGISTERED) } ?: emptyList()
        val stats = eventRegistrationManager.getRegistrationRolesCountsByEventId(registrations)
        val attendeeStats = AttendeeStats(
            going = RegistrationStats(
                total = stats.total,
                leaders = stats.leaders,
                followers = stats.followers,
                both = stats.both
            ),
            interested = eventRegistrationStatsService.getRegistrationCountByEventId(event.id, RegistrationStatus.INTERESTED)
        )

        return SingleEventDTO(
            id = event.id.toString(),
            eventName = event.eventName,
            organizer = mapToOrganizer(event),
            status = event.status,
            userStatus = userStatus,
            date = event.eventDate.toString(),
            time = event.eventTime.toString(),
            location = locationToDto(event.location),
            attendeeStats = attendeeStats
        )
    }

    private fun locationToDto(location: Location?): LocationRequest? {
        return location?.let {
            LocationRequest(
                name = it.name,
                street = it.street,
                city = it.city,
                country = it.country,
                postalCode = it.postalCode,
                houseNumber = it.houseNumber,
                state = it.state,
                county = it.county
            )
        }
    }

    private fun mapToOrganizer(event: Event): OrganizerDto {
        return OrganizerDto(
            userId = event.organizerId.toString(),
            firstName = event.organizer?.profile?.firstName,
            lastName = event.organizer?.profile?.lastName
        )
    }

    private fun fillMap(userStatusMap: MutableMap<UUID, RsvpStatus>, eventsIds: List<UUID>, status: RsvpStatus) {
        eventsIds.forEach { eventId ->
            userStatusMap[eventId] = status
        }
    }
}

