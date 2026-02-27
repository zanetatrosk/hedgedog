package com.example.bedanceapp.controller

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.service.EventRegistrationDataService
import com.example.bedanceapp.service.EventRegistrationManager
import com.example.bedanceapp.service.OrganizerAction
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
     * Get registration data
     * GET /api/events/{id}/registrations
     */
    @GetMapping("/{id}/registrations")
    fun getApprovedRegistrations(@PathVariable id: UUID): ResponseEntity<List<EventRegistrationDto>> {
        val registrations = eventRegistrationManager.getAllApprovedRegistrations(id);
        return ResponseEntity.ok(registrations)
    }

    /**
     * Create or update user's RSVP for an event
     * PUT /api/events/{eventId}/my-rsvp
     * Valid statuses: interested, registered, waitlisted
     */
    @PutMapping("/{eventId}/registrations")
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
            email = request.email,
            isAnonymous = request.isAnonymous,
        )
        return ResponseEntity.ok(registration)
    }

    /**
     * Cancel/remove user's RSVP for an event
     * DELETE /api/events/{eventId}/my-rsvp
     */
    @DeleteMapping("/{eventId}/registrations/{registrationId}")
    fun deleteMyRsvp(
        @PathVariable eventId: UUID,
        @PathVariable registrationId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<Map<String, String>> {
        eventRegistrationManager.deleteRegistrationByRegistrationId(registrationId)
        return ResponseEntity.ok(mapOf("message" to "Registration deleted successfully"))
    }

    /**
     * Sync event's Google Form registration data
     * POST /api/events/{eventId}/sync-form
     * This fetches the latest form structure from Google Forms and updates the cached structure
     */
    @PostMapping("/{eventId}/registrations/synchronization")
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
     * Update registration status
     * PATCH /api/events/{eventId}/registrations/{registrationId}
     *
     * Handles both:
     * - Organizer actions (approve/reject)
     * - User cancellations
     */
    @PatchMapping("/{eventId}/registrations/{registrationId}")
    fun updateRegistrationStatus(
        @PathVariable eventId: UUID,
        @PathVariable registrationId: UUID,
        @RequestHeader("X-User-Id") userId: UUID,
        @RequestBody request: RegistrationActionRequest
    ): ResponseEntity<EventRegistration> {

        val updated = when (request.action) {
            RegistrationAction.APPROVE, RegistrationAction.REJECT -> {
                // Organizer actions - verify organizer permission
                eventRegistrationManager.assertOrganizer(eventId, userId)
                val organizerAction = when (request.action) {
                    RegistrationAction.APPROVE -> OrganizerAction.APPROVE
                    RegistrationAction.REJECT -> OrganizerAction.REJECT
                    else -> throw IllegalArgumentException("Invalid organizer action")
                }
                eventRegistrationManager.handleOrganizerAction(
                    registrationId = registrationId,
                    action = organizerAction
                )
            }
            RegistrationAction.CANCEL -> {
                // User cancellation - verify user owns the registration
                eventRegistrationManager.cancelRegistration(
                    eventId = eventId,
                    userId = userId,
                    registrationId = registrationId
                )
                // Return the cancelled registration
                eventRegistrationManager.getLastRegistrationByEventIdAndUserId(eventId, userId)
                    ?: throw IllegalStateException("Registration not found after cancellation")
            }
        }

        return ResponseEntity.ok(updated)
    }
}

/**
 * Request body for event registration
 */
data class RegisterEventRequest(
    val status: RegistrationStatus,  // interested, registered, waitlisted
    val roleId: UUID? = null,  // Leader, Follower, Both
    val email: String? = null,  // Optional - will use user's email from profile if not provided
    val isAnonymous: Boolean = false,
)

enum class RegistrationStatus {
    REGISTERED,
    INTERESTED,
    WAITLISTED,
    CANCELLED,
    REJECTED,
    PENDING
}

data class RegistrationActionRequest(
    val action: RegistrationAction
)

enum class RegistrationAction {
    APPROVE,    // Organizer action: approve pending registration
    REJECT,     // Organizer action: reject registration
    CANCEL      // User action: cancel their own registration
}

