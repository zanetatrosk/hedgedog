package com.example.bedanceapp.service

import com.example.bedanceapp.controller.*
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.repository.EventParentRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service for managing registrations at the parent event (course) level
 * Handles operations that apply to all events within a course
 */
@Service
class ParentEventRegistrationService(
    private val eventRepository: EventRepository,
    private val eventParentRepository: EventParentRepository,
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRegistrationManager: EventRegistrationManager,
    private val eventService: EventService,
) {

    /**
     * Get aggregated statistics for all events in a parent event (course)
     */
    @Transactional(readOnly = true)
    fun getParentEventStats(parentEventId: UUID): ParentEventStatsResponse {
        // Validate parent event exists
        val parentEvent = eventParentRepository.findById(parentEventId)
            .orElseThrow { IllegalArgumentException("Parent event not found with id: $parentEventId") }

        // Get all child events
        val childEvents = eventRepository.findByParentEventId(parentEventId)
            .sortedBy { it.eventDate }

        // Calculate stats for each event
        val eventStats = childEvents.map { event ->
            val eventId = event.id!!
            val registrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)

            EventStatsInfo(
                eventId = eventId,
                eventName = event.eventName,
                eventDate = event.eventDate.toString(),
                totalRegistrations = registrations.size,
                goingCount = registrations.count { it.status == RegistrationStatus.GOING },
                interestedCount = registrations.count { it.status == RegistrationStatus.INTERESTED },
                waitlistedCount = registrations.count { it.status == RegistrationStatus.WAITLISTED },
                pendingCount = registrations.count { it.status == RegistrationStatus.PENDING }
            )
        }

        return ParentEventStatsResponse(
            parentEventId = parentEventId,
            parentEventName = parentEvent.name,
            totalEvents = childEvents.size,
            eventStats = eventStats
        )
    }

    /**
     * Get user's RSVPs for all events in a parent event (course)
     */
    @Transactional(readOnly = true)
    fun getUserRsvpsForParentEvent(parentEventId: UUID, userId: UUID): ParentEventRsvpResponse {
        // Validate parent event exists
        val parentEvent = eventParentRepository.findById(parentEventId)
            .orElseThrow { IllegalArgumentException("Parent event not found with id: $parentEventId") }

        // Get all child events
        val childEvents = eventRepository.findByParentEventId(parentEventId)
            .sortedBy { it.eventDate }

        // Get user's registration for each event
        val registrations = childEvents.map { event ->
            val eventId = event.id!!
            val registration = eventRegistrationManager.getLastRegistrationByEventIdAndUserId(eventId, userId)

            EventRegistrationWithEventInfo(
                eventId = eventId,
                eventName = event.eventName,
                eventDate = event.eventDate.toString(),
                registration = registration
            )
        }

        return ParentEventRsvpResponse(
            parentEventId = parentEventId,
            parentEventName = parentEvent.name,
            registrations = registrations
        )
    }

    /**
     * Register user for all events in a parent event (course)
     * Creates/updates registrations for each child event
     */
    @Transactional
    fun registerUserForAllEvents(
        parentEventId: UUID,
        userId: UUID,
        status: RegistrationStatus,
        roleId: UUID?,
        email: String?
    ): ParentEventRsvpResponse {
        // Validate parent event exists
        val parentEvent = eventParentRepository.findById(parentEventId)
            .orElseThrow { IllegalArgumentException("Parent event not found with id: $parentEventId") }

        // Get all child events
        val childEvents = eventRepository.findByParentEventId(parentEventId)
            .sortedBy { it.eventDate }

        if (childEvents.isEmpty()) {
            throw IllegalArgumentException("No events found for parent event: $parentEventId")
        }

        // Register user for each event
        val registrations = childEvents.map { event ->
            val registration = try {
                eventRegistrationManager.registerUserForEvent(
                    eventId = event.id!!,
                    userId = userId,
                    status = status,
                    roleId = roleId,
                    email = email
                )
            } catch (e: IllegalArgumentException) {
                // If user is organizer of any event, skip that event
                null
            }

            EventRegistrationWithEventInfo(
                eventId = event.id!!,
                eventName = event.eventName,
                eventDate = event.eventDate.toString(),
                registration = registration
            )
        }

        return ParentEventRsvpResponse(
            parentEventId = parentEventId,
            parentEventName = parentEvent.name,
            registrations = registrations
        )
    }

    /**
     * Cancel all registrations for a user across all events in a parent event (course)
     */
    @Transactional
    fun cancelAllRegistrations(parentEventId: UUID, userId: UUID): Int {
        // Validate parent event exists
        val parentEvent = eventParentRepository.findById(parentEventId)
            .orElseThrow { IllegalArgumentException("Parent event not found with id: $parentEventId") }

        // Get all child events
        val childEvents = eventRepository.findByParentEventId(parentEventId)

        var cancelledCount = 0

        // Cancel registration for each event
        childEvents.forEach { event ->
            val eventId = event.id!!
            val registration = eventRegistrationManager.getLastRegistrationByEventIdAndUserId(eventId, userId)
            if (registration != null && registration.status != RegistrationStatus.CANCELLED) {
                try {
                    eventRegistrationManager.cancelRegistration(eventId, userId, registration.id!!)
                    cancelledCount++
                } catch (e: Exception) {
                    // Continue with other events if one fails
                }
            }
        }

        return cancelledCount
    }

    /**
     * Update registration status for a specific event in the course (organizer only)
     */
    @Transactional
    fun updateRegistrationStatus(
        parentEventId: UUID,
        eventId: UUID,
        registrationId: UUID,
        organizerId: UUID,
        action: OrganizerAction
    ): EventRegistration {
        // Validate parent event exists
        eventParentRepository.findById(parentEventId)
            .orElseThrow { IllegalArgumentException("Parent event not found with id: $parentEventId") }

        // Validate event belongs to parent
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        if (event.parentEventId != parentEventId) {
            throw IllegalArgumentException("Event $eventId does not belong to parent event $parentEventId")
        }

        // Assert user is organizer
        eventRegistrationManager.assertOrganizer(eventId, organizerId)

        // Perform the action
        return eventRegistrationManager.handleOrganizerAction(registrationId, action)
    }

    /**
     * Sync Google Form data for all events in the course
     */
    @Transactional
    fun syncAllGoogleFormData(parentEventId: UUID, organizerId: UUID): Int {
        // Validate parent event exists
        eventParentRepository.findById(parentEventId)
            .orElseThrow { IllegalArgumentException("Parent event not found with id: $parentEventId") }

        // Get all child events
        val childEvents = eventRepository.findByParentEventId(parentEventId)

        if (childEvents.isEmpty()) {
            throw IllegalArgumentException("No events found for parent event: $parentEventId")
        }

        // Verify organizer for all events (must be organizer of all)
        childEvents.forEach { event ->
            eventRegistrationManager.assertOrganizer(event.id!!, organizerId)
        }

        var syncedCount = 0

        // Sync each event
        childEvents.forEach { event ->
            try {
                eventRegistrationManager.syncGoogleFormData(event.id!!)
                syncedCount++
            } catch (e: Exception) {
                // Continue with other events if one fails
            }
        }

        return syncedCount
    }
}

