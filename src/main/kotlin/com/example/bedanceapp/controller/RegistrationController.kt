package com.example.bedanceapp.controller

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.model.User
import com.example.bedanceapp.service.registration.AttendeeRegistrationService
import com.example.bedanceapp.service.registration.EventRegistrationDataService
import com.example.bedanceapp.service.registration.EventRegistrationQueryService
import com.example.bedanceapp.service.registration.OrganizerRegistrationService
import com.example.bedanceapp.service.registration.StatsResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class RegistrationController(
    private val attendeeRegistrationService: AttendeeRegistrationService,
    private val organizerRegistrationService: OrganizerRegistrationService,
    private val eventRegistrationDataService: EventRegistrationDataService,
    private val eventRegistrationQueryService: EventRegistrationQueryService
) {

    /**
     * Get statistics for an event including registration data
     * GET /api/events/{id}/stats
     */
    @GetMapping("/{id}/stats")
    fun getAllEventRegistrations(@PathVariable id: UUID): ResponseEntity<StatsResponse> {
        val stats = eventRegistrationDataService.getAllRegistrationsByEvent(id)
        return ResponseEntity.ok(stats)
    }

    /**
     * Get registrations that are approved for an event
     * GET /api/events/{id}/registrations
     */
    @GetMapping("/{id}/registrations")
    fun getApprovedRegistrations(@PathVariable id: UUID): ResponseEntity<List<EventRegistrationDto>> {
        val registrations = eventRegistrationQueryService.getAllApprovedRegistrations(id);
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
        @AuthenticationPrincipal user: User,
        @RequestBody request: RegisterEventRequest
    ): ResponseEntity<EventRegistration> {
        val userId = requireNotNull(user.id) { "Authenticated user ID is missing" }
        val registration = attendeeRegistrationService.registerUserForEvent(
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
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Map<String, String>> {
        val userId = requireNotNull(user.id) { "Authenticated user ID is missing" }
        attendeeRegistrationService.deleteRegistrationByRegistrationId(eventId, userId, registrationId)
        return ResponseEntity.ok(mapOf("message" to "Registration deleted successfully"))
    }

    /**
     * Manually sync event's Google Form registration data
     * POST /api/events/{eventId}/registrations/synchronization
     *
     * Note: Google Forms are automatically synced every 10 minutes.
     * This endpoint is available for manual/immediate sync if needed.
     *
     * This fetches the latest form structure from Google Forms and updates the cached structure
     */
    @PostMapping("/{eventId}/registrations/synchronization")
    fun syncGoogleFormData(
        @PathVariable eventId: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Map<String, String>> {
        return try {
            val organizerId = requireNotNull(user.id) { "Authenticated user ID is missing" }
            organizerRegistrationService.syncGoogleFormData(eventId, organizerId)
            ResponseEntity.ok(mapOf(
                "message" to "Google Form data synced successfully",
                "eventId" to eventId.toString()
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(mapOf("message" to (e.message ?: "Failed to sync form data")))
        } catch (_: Exception) {
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
        @AuthenticationPrincipal user: User,
        @RequestBody request: RegistrationActionRequest
    ): ResponseEntity<EventRegistration> {
        val userId = requireNotNull(user.id) { "Authenticated user ID is missing" }
        val updated = when (request.action) {
            RegistrationAction.APPROVE, RegistrationAction.REJECT ->
                organizerRegistrationService.updateRegistrationStatus(eventId, registrationId, userId, request.action)

            RegistrationAction.CANCEL ->
                attendeeRegistrationService.cancelRegistration(eventId, userId, registrationId)
        }

        return ResponseEntity.ok(updated)
    }
}

/**
 * Request body for event registration
 */
data class RegisterEventRequest(
    val status: RegistrationStatus,  // interested, registered, waitlisted
    val roleId: UUID? = null,  // Leader, Follower
    val email: String? = null,  // Optional - will use user's email from profile if not provided
    val isAnonymous: Boolean = false,
)

enum class RegistrationStatus {
    REGISTERED,
    INTERESTED,
    WAITLISTED,
    CANCELLED,
    REJECTED,
    PENDING;

    fun canTransitionTo(target: RegistrationStatus): Boolean {
        if (this == target) return true

        val allowedTargets = when (this) {
            INTERESTED -> setOf(PENDING, REGISTERED, WAITLISTED)
            PENDING -> setOf(REGISTERED, WAITLISTED, REJECTED, CANCELLED)
            WAITLISTED -> setOf(REGISTERED, REJECTED, CANCELLED)
            REGISTERED -> setOf(REJECTED, CANCELLED)
            CANCELLED -> setOf(INTERESTED, PENDING, REGISTERED, WAITLISTED)
            REJECTED -> emptySet()
        }

        return target in allowedTargets
    }

    fun requireTransitionTo(target: RegistrationStatus) {
        require(canTransitionTo(target)) {
            "Transition from $this to $target is not allowed"
        }
    }
}

data class RegistrationActionRequest(
    val action: RegistrationAction
)

enum class RegistrationAction {
    APPROVE,    // Organizer action: approve pending registration
    REJECT,     // Organizer action: reject registration
    CANCEL      // User action: cancel their own registration
}

