package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CreateEventRequest
import com.example.bedanceapp.model.CreateEventResponse
import com.example.bedanceapp.model.EventDetailData
import com.example.bedanceapp.model.EventDto
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.PagedResponse
import com.example.bedanceapp.model.PublishEventRequest
import com.example.bedanceapp.service.EventRegistrationDataService
import com.example.bedanceapp.service.EventService
import com.example.bedanceapp.service.EventRegistrationService
import com.example.bedanceapp.service.StatsResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class EventController(
    private val eventService: EventService,
    private val eventRegistrationService: EventRegistrationService,
    private val eventRegistrationDataService: EventRegistrationDataService
) {

    @GetMapping
    fun getEvents(
        @RequestHeader("X-User-Id") userId: UUID? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) eventName: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) danceStyles: List<UUID>?,
        @RequestParam(required = false) eventTypes: List<UUID>?,
        @RequestParam(defaultValue = "true") includeCancelled: Boolean
    ): ResponseEntity<PagedResponse<EventDto>> {
        val sort = Sort.by(Sort.Order.asc("eventDate"), Sort.Order.asc("eventTime"))
        val pageable = PageRequest.of(page, size, sort)
        val eventsPage = eventService.getAllPublishedEventsPaginated(
            userId = userId,
            pageable = pageable,
            eventName = eventName,
            city = city,
            country = country,
            danceStyleIds = danceStyles,
            eventTypeIds = eventTypes,
            includeCancelled = includeCancelled
        )

        val response = PagedResponse(
            content = eventsPage.content,
            page = eventsPage.number,
            size = eventsPage.size,
            totalElements = eventsPage.totalElements,
            totalPages = eventsPage.totalPages,
            isLast = eventsPage.isLast
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping
    fun createEvent(
        @RequestBody request: CreateEventRequest,
        @RequestHeader("X-User-Id") organizerId: UUID  // TODO: Replace with actual authentication
    ): ResponseEntity<CreateEventResponse> {
        val events = eventService.createEventByOccurrence(request, organizerId).map { it.id }
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateEventResponse(
                events = events,
                message = "Events created successfully"
            )

        )
    }

    @PutMapping("/{eventId}")
    fun updateEvent(
        @PathVariable eventId: UUID,
        @RequestBody request: CreateEventRequest,
        @RequestHeader("X-User-Id") organizerId: UUID  // TODO: Replace with actual authentication
    ): ResponseEntity<CreateEventResponse> {
        val event = eventService.updateEvent(eventId, request, organizerId)
        return ResponseEntity.ok(
            CreateEventResponse(
                events = listOf(event.id),
                message = "Event updated successfully"
            )
        )
    }

    @GetMapping("/{id}")
    fun getEventById(
        @PathVariable id: UUID,
        @RequestHeader("X-User-Id", required = false) userId: UUID?
    ): ResponseEntity<EventDetailData> {
        val eventDetail = eventService.getEventDetailById(id, userId)
        return ResponseEntity.ok(eventDetail)
    }

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
            eventService.syncGoogleFormData(eventId)
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
     * Get user's RSVP for an event
     * GET /api/events/{eventId}/my-rsvp
     */
    @GetMapping("/{eventId}/my-rsvp")
    fun getMyRsvp(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") userId: UUID
    ): ResponseEntity<EventRegistration> {
        val registration = eventRegistrationService.getLastRegistrationByEventIdAndUserId(eventId, userId)
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
        val registration = eventRegistrationService.registerUserForEvent(
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
        val cancelled = eventRegistrationService.cancelRegistration(eventId, userId)
        return if (cancelled) {
            ResponseEntity.ok(mapOf("message" to "RSVP cancelled successfully"))
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "No RSVP found for this event"))
        }
    }

    /**
     * Publish a draft event
     * PATCH /api/events/{eventId}/publish
     */
    @PatchMapping("/{eventId}/publish")
    fun publishEvent(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") organizerId: UUID,
        @RequestBody request: PublishEventRequest,
    ): ResponseEntity<EventStatusResponse> {
        return try {
            val event = eventService.publishEvent(eventId, organizerId, request)
            ResponseEntity.ok(
                EventStatusResponse(
                    id = event.id,
                    status = event.status.name,
                    message = "Event published successfully"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                EventStatusResponse(
                    id = eventId,
                    status = null,
                    message = e.message ?: "Cannot publish event"
                )
            )
        }
    }

    /**
     * Cancel a published event (soft delete)
     * PATCH /api/events/{eventId}/cancel
     */
    @PatchMapping("/{eventId}/cancel")
    fun cancelEvent(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") organizerId: UUID
    ): ResponseEntity<EventStatusResponse> {
        return try {
            val event = eventService.cancelEvent(eventId, organizerId)
            ResponseEntity.ok(
                EventStatusResponse(
                    id = event.id,
                    status = event.status.name,
                    message = "Event cancelled successfully"
                )
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(
                EventStatusResponse(
                    id = eventId,
                    status = null,
                    message = e.message ?: "Cannot cancel event"
                )
            )
        }
    }

    /**
     * Delete a draft event (hard delete)
     * DELETE /api/events/{eventId}
     */
    @DeleteMapping("/{eventId}")
    fun deleteEvent(
        @PathVariable eventId: UUID,
        @RequestHeader("X-User-Id") organizerId: UUID
    ): ResponseEntity<Map<String, String>> {
        return try {
            eventService.deleteEvent(eventId, organizerId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(mapOf("message" to (e.message ?: "Cannot delete event")))
        }
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

/**
 * Response body for event status changes
 */
data class EventStatusResponse(
    val id: UUID?,
    val status: String?,
    val message: String
)

enum class RegistrationStatus {
    GOING,
    INTERESTED,
    WAITLISTED,
    CANCELLED,
    REJECTED,
    PENDING
}

