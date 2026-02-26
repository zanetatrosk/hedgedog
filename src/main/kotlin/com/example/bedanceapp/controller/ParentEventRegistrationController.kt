package com.example.bedanceapp.controller

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.service.ParentEventRegistrationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/parent-events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class ParentEventRegistrationController(
    private val parentEventRegistrationService: ParentEventRegistrationService
) {

    /**
     * Register user for ALL events in a course (parent event)
     * PUT /api/parent-events/{parentEventId}/my-rsvp
     * This will create/update registrations for all child events
     */
    @PutMapping("/{parentEventId}/my-rsvp")
    fun registerForAllEventsInCourse(
        @PathVariable parentEventId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: RegisterEventRequest
    ): ResponseEntity<ParentEventRsvpResponse> {
        val registrations = parentEventRegistrationService.registerUserForAllEvents(
            parentEventId = parentEventId,
            userId = userId,
            status = request.status,
            roleId = request.roleId,
            email = request.email
        )
        return ResponseEntity.ok(registrations)
    }
}

/**
 * Response containing user's RSVPs for all events in a parent event (course)
 */
data class ParentEventRsvpResponse(
    val parentEventId: UUID,
    val parentEventName: String,
    val registrations: List<EventRegistrationWithEventInfo>
)

/**
 * Event registration with basic event info
 */
data class EventRegistrationWithEventInfo(
    val eventId: UUID,
    val eventName: String,
    val eventDate: String,
    val registration: EventRegistration?
)

/**
 * Aggregated statistics for all events in a parent event (course)
 */
data class ParentEventStatsResponse(
    val parentEventId: UUID,
    val parentEventName: String,
    val totalEvents: Int,
    val eventStats: List<EventStatsInfo>
)

/**
 * Statistics for a single event within a parent event
 */
data class EventStatsInfo(
    val eventId: UUID,
    val eventName: String,
    val eventDate: String,
    val totalRegistrations: Int,
    val goingCount: Int,
    val interestedCount: Int,
    val waitlistedCount: Int,
    val pendingCount: Int
)

