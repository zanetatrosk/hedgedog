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
     * Get aggregated statistics for all events in a course (parent event)
     * GET /api/parent-events/{parentEventId}/stats
     */
    @GetMapping("/{parentEventId}/stats")
    fun getParentEventStats(@PathVariable parentEventId: UUID): ResponseEntity<ParentEventStatsResponse> {
        val stats = parentEventRegistrationService.getParentEventStats(parentEventId)
        return ResponseEntity.ok(stats)
    }

    /**
     * Get user's RSVPs for all events in a course (parent event)
     * GET /api/parent-events/{parentEventId}/my-rsvp
     */
    @GetMapping("/{parentEventId}/my-rsvp")
    fun getMyRsvpForCourse(
        @PathVariable parentEventId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<ParentEventRsvpResponse> {
        val rsvps = parentEventRegistrationService.getUserRsvpsForParentEvent(parentEventId, userId)
        return ResponseEntity.ok(rsvps)
    }

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

    /**
     * Cancel user's RSVP for ALL events in a course (parent event)
     * DELETE /api/parent-events/{parentEventId}/my-rsvp
     */
    @DeleteMapping("/{parentEventId}/my-rsvp")
    fun cancelRsvpForAllEventsInCourse(
        @PathVariable parentEventId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Map<String, String>> {
        val cancelledCount = parentEventRegistrationService.cancelAllRegistrations(parentEventId, userId)

        return if (cancelledCount > 0) {
            ResponseEntity.ok(mapOf(
                "message" to "Successfully cancelled $cancelledCount RSVP(s) for the course",
                "cancelledCount" to cancelledCount.toString()
            ))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "No RSVPs found for this course"))
        }
    }

    /**
     * Update registration status for a specific event in the course (organizer only)
     * PATCH /api/parent-events/{parentEventId}/events/{eventId}/registrations/{registrationId}
     */
    @PatchMapping("/{parentEventId}/events/{eventId}/registrations/{registrationId}")
    fun updateRegistrationStatusInCourse(
        @PathVariable parentEventId: UUID,
        @PathVariable eventId: UUID,
        @PathVariable registrationId: UUID,
        @RequestHeader("X-User-Id") organizerId: UUID,
        @RequestBody request: OrganizerRegistrationActionRequest
    ): ResponseEntity<EventRegistration> {
        val updated = parentEventRegistrationService.updateRegistrationStatus(
            parentEventId = parentEventId,
            eventId = eventId,
            registrationId = registrationId,
            organizerId = organizerId,
            action = request.action
        )
        return ResponseEntity.ok(updated)
    }

    /**
     * Sync Google Form data for all events in the course (organizer only)
     * POST /api/parent-events/{parentEventId}/sync-registrations
     */
    @PostMapping("/{parentEventId}/sync-registrations")
    fun syncAllGoogleFormData(
        @PathVariable parentEventId: UUID,
        @RequestHeader("X-User-Id") organizerId: UUID
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val syncedCount = parentEventRegistrationService.syncAllGoogleFormData(parentEventId, organizerId)
            ResponseEntity.ok(mapOf(
                "message" to "Successfully synced Google Form data for all events in the course",
                "parentEventId" to parentEventId.toString(),
                "syncedEventsCount" to syncedCount
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(mapOf("message" to (e.message ?: "Failed to sync form data")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "An error occurred while syncing form data"))
        }
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

