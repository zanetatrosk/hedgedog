package com.example.bedanceapp.controller

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.service.EventRegistrationDataService
import com.example.bedanceapp.service.EventRegistrationManager
import com.example.bedanceapp.service.StatsResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class RegistrationController(
    private val eventRegistrationManager: EventRegistrationManager,
    private val eventRegistrationDataService: EventRegistrationDataService
) {

    /**
     * Get statistics for an event including registration data
     * GET /api/events/{id}/stats
     */
    @GetMapping("/{id}/stats")
    fun getEventStats(@PathVariable id: UUID): ResponseEntity<StatsResponse> {
        val stats = eventRegistrationDataService.getAllStatsByEvent(id)
        return ResponseEntity.ok(stats)
    }

    /**
     * Get user's RSVP for an event
     * GET /api/events/{eventId}/my-rsvp
     */
    @GetMapping("/{eventId}/my-rsvp")
    fun getMyRsvp(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<EventRegistration> {
        val registration = eventRegistrationManager.getLastRegistrationByEventIdAndUserId(eventId, userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(registration)
    }

    /**
     * Create or update user's RSVP for an event
     * PUT /api/events/{eventId}/my-rsvp
     * Valid statuses: interested, going, waitlisted
     */
    @PutMapping("/{eventId}/my-rsvp")
    fun createOrUpdateMyRsvp(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: RegisterEventRequest
    ): ResponseEntity<EventRegistration> {
        val registration = eventRegistrationManager.registerUserForEvent(
            eventId = eventId,
            userId = userId,
            status = request.status,
            roleId = request.roleId,
            email = request.email
        )
        return ResponseEntity.ok(registration)
    }

    /**
     * Cancel/remove user's RSVP for an event
     * DELETE /api/events/{eventId}/my-rsvp
     */
    @DeleteMapping("/{eventId}/my-rsvp")
    fun deleteMyRsvp(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Map<String, String>> {
        val registration = eventRegistrationManager.getLastRegistrationByEventIdAndUserId(eventId, userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "No RSVP found for this event"))

        val cancelled = eventRegistrationManager.cancelRegistration(eventId, userId, registration.id!!)
        return if (cancelled) {
            ResponseEntity.ok(mapOf("message" to "RSVP cancelled successfully"))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "No RSVP found for this event"))
        }
    }

    /**
     * Sync event's Google Form registration data
     * POST /api/events/{eventId}/sync-form
     * This fetches the latest form structure from Google Forms and updates the cached structure
     */
    @PostMapping("/{eventId}/sync-registrations")
    fun syncGoogleFormData(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") organizerId: UUID
    ): ResponseEntity<Map<String, String>> {
        return try {
            eventRegistrationManager.syncGoogleFormData(eventId)
            ResponseEntity.ok(mapOf(
                "message" to "Google Form data synced successfully",
                "eventId" to eventId.toString()
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(mapOf("message" to (e.message ?: "Failed to sync form data")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "An error occurred while syncing form data"))
        }
    }

    /**
     * Update registration status (organizer only)
     * PATCH /api/events/{eventId}/registrations/{registrationId}
     */
    @PatchMapping("/{eventId}/registrations/{registrationId}")
    fun updateRegistrationStatus(
        @PathVariable eventId: UUID,
        @PathVariable registrationId: UUID,
        @RequestHeader("X-User-Id") organizerId: UUID,
        @RequestBody request: OrganizerRegistrationActionRequest
    ): ResponseEntity<EventRegistration> {

        eventRegistrationManager.assertOrganizer(eventId, organizerId)

        val updated = eventRegistrationManager
            .handleOrganizerAction(
                registrationId = registrationId,
                action = request.action
            )

        return ResponseEntity.ok(updated)
    }
}

/**
 * Request body for event registration
 */
data class RegisterEventRequest(
    val status: RegistrationStatus,  // interested, going, waitlisted
    val roleId: UUID? = null,  // Leader, Follower, Both
    val email: String? = null  // Optional - will use user's email from profile if not provided
)

enum class RegistrationStatus {
    GOING,
    INTERESTED,
    WAITLISTED,
    CANCELLED,
    REJECTED,
    PENDING
}

data class OrganizerRegistrationActionRequest(
    val action: OrganizerAction
)

enum class OrganizerAction {
    APPROVE,
    REJECT
}
