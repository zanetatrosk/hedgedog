package com.example.bedanceapp.controller

import com.example.bedanceapp.model.EventRegistrationActionResultDto
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.model.RegistrationAction
import com.example.bedanceapp.model.RegistrationActionRequest
import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.User
import com.example.bedanceapp.service.registration.AttendeeRegistrationService
import com.example.bedanceapp.service.registration.EventRegistrationDataService
import com.example.bedanceapp.service.registration.EventRegistrationQueryService
import com.example.bedanceapp.service.registration.OrganizerRegistrationService
import com.example.bedanceapp.service.registration.StatsResponse
import com.example.bedanceapp.service.mapping.EventRegistrationMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Registration Controller
 * 
 * Handles event registration operations including RSVP management, registration approvals,
 * organizer registration statistics, and Google Forms synchronization.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class RegistrationController(
    private val attendeeRegistrationService: AttendeeRegistrationService,
    private val organizerRegistrationService: OrganizerRegistrationService,
    private val eventRegistrationDataService: EventRegistrationDataService,
    private val eventRegistrationQueryService: EventRegistrationQueryService,
    private val eventRegistrationMapper: EventRegistrationMapper
) {

    /**
     * Get statistics for an event including registration data
     * GET /api/events/{id}/stats
     * 
     * Returns comprehensive registration statistics for the event and the list of registrations for organizer.
     * 
     * @param id The ID of the event
     * @return Statistics response with registration data
     */
    @GetMapping("/{id}/stats")
    fun getAllEventRegistrations(@PathVariable id: UUID, @AuthenticationPrincipal user: User): ResponseEntity<StatsResponse> {
        val userId = requireNotNull(user.id) { "Authenticated user ID is missing" }
        val stats = eventRegistrationDataService.getAllRegistrationsByEvent(id, userId)
        return ResponseEntity.ok(stats)
    }

    /**
     * Get registrations that are in state registered for an event
     * GET /api/events/{id}/registrations
     * 
     * @param id The ID of the event
     * @return List of approved event registrations
     */
    @GetMapping("/{id}/registrations")
    fun getApprovedRegistrations(@PathVariable id: UUID): ResponseEntity<List<EventRegistrationDto>> {
        val registrations = eventRegistrationQueryService.getAllApprovedRegistrations(id)
        return ResponseEntity.ok(registrations)
    }

    /**
     * Create or update user's RSVP for an event
     * PUT /api/events/{eventId}/registrations
     * 
     * Allows an authenticated user to register for an event with a specific status
     * (interested or registered) and other information related to registration. Creates a new registration or updates
     * an existing one.
     * 
     * @param eventId The ID of the event
     * @param user Currently authenticated user
     * @param request Registration details including status and optional role
     * @return Created or updated event registration
     */
    @PutMapping("/{eventId}/registrations")
    fun createOrUpdateMyRsvp(
        @PathVariable eventId: UUID,
        @AuthenticationPrincipal user: User,
        @RequestBody request: RegisterEventRequest
    ): ResponseEntity<EventRegistrationActionResultDto> {
        val userId = requireNotNull(user.id) { "Authenticated user ID is missing" }
        val registration = attendeeRegistrationService.registerUserForEvent(
            eventId = eventId,
            userId = userId,
            status = request.status,
            roleId = request.roleId,
            isAnonymous = request.isAnonymous,
        )
        return ResponseEntity.ok(eventRegistrationMapper.toActionResult(registration))
    }

    /**
     * Remove user registration for an event (only for registrations that have a state interested)
     * DELETE /api/events/{eventId}/registrations/{registrationId}
     * 
     * @param eventId The ID of the event
     * @param registrationId The ID of the registration to delete
     * @param user Currently authenticated user
     * @return Success message response
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
     * @param eventId The ID of the event
     * @param user Currently authenticated user (organizer)
     * @return Success message with event ID
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
     * Handles both organizer actions (approve/reject) and user cancellations.
     * Organizers can approve or reject pending registrations.
     * Users can cancel their own registrations.
     * 
     * @param eventId The ID of the event
     * @param user Currently authenticated user
     * @param request Action request specifying approve, reject, or cancel with the regitsrations ids to be updated
     * @return Updated event registration
     */
    @PatchMapping("/{eventId}/registrations")
    fun updateRegistrationStatus(
        @PathVariable eventId: UUID,
        @AuthenticationPrincipal user: User,
        @RequestBody request: RegistrationActionRequest
    ): ResponseEntity<List<EventRegistrationActionResultDto>> {
        val userId = requireNotNull(user.id) { "Authenticated user ID is missing" }

        val updated = when (request.action) {
            RegistrationAction.APPROVE, RegistrationAction.REJECT ->
                request.registrations.map { registrationId ->
                    organizerRegistrationService.updateRegistrationStatus(eventId, registrationId, userId, request.action)
                }

            RegistrationAction.CANCEL ->
                request.registrations.map { registrationId ->
                    attendeeRegistrationService.cancelRegistration(eventId, userId, registrationId)
                }
        }

        return ResponseEntity.ok(eventRegistrationMapper.toActionResultList(updated))
    }
}

data class RegisterEventRequest(
    val status: RegistrationStatus,  // interested, registered
    val roleId: UUID? = null,  // Optional role (e.g., Leader, Follower)
    val isAnonymous: Boolean = false,  // Whether registration should be anonymous
)



