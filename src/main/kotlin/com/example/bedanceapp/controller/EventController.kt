package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CreateUpdateEventDto
import com.example.bedanceapp.model.CreateEventResponse
import com.example.bedanceapp.model.EventDetailDto
import com.example.bedanceapp.model.EventSummaryDto
import com.example.bedanceapp.model.PagedResponse
import com.example.bedanceapp.model.PublishEventDto
import com.example.bedanceapp.model.User
import com.example.bedanceapp.service.event.EventService
import com.example.bedanceapp.service.event.OrganizerEventService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Event Controller
 * 
 * Handles event CRUD operations, publishing, cancellation, and retrieval.
 * Supports event filtering by name, location, dance styles, and event types.
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class EventController(
    private val eventService: EventService,
    private val organizerEventService: OrganizerEventService
) {

    /**
     * Get all published events with optional filters and pagination
     * GET /api/events
     * 
     * @param user Currently authenticated user (optional, for personalization)
     * @param page Page number for pagination (default: 0)
     * @param size Number of items per page (default: 10)
     * @param eventName Optional filter by event name
     * @param city Optional filter by city
     * @param country Optional filter by country
     * @param danceStyles Optional list of dance style IDs to filter by
     * @param eventTypes Optional list of event type IDs to filter by
     * @param includeCancelled Whether to include cancelled events (default: true)
     * @return Paginated list of published events sorted by date and time
     */
    @GetMapping
    fun getEvents(
        @AuthenticationPrincipal user: User? = null,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) eventName: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) danceStyles: List<UUID>?,
        @RequestParam(required = false) eventTypes: List<UUID>?,
        @RequestParam(defaultValue = "true") includeCancelled: Boolean
    ): ResponseEntity<PagedResponse<EventSummaryDto>> {
        val sort = Sort.by(Sort.Order.asc("eventDate"), Sort.Order.asc("eventTime"))
        val pageable = PageRequest.of(page, size, sort)
        val eventsPage = eventService.getAllPublishedEventsPaginated(
            userId = user?.id,
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

    /**
     * Create a new event with specified occurrences
     * POST /api/events
     * 
     * Creates one or more events based on the occurrence pattern specified in the request.
     * Only authenticated users can create events.
     * Events are created in DRAFT status and must be published separately.
     * 
     * @param request Event details including occurrence pattern
     * @param user Currently authenticated user (organizer)
     * @return Response with created event IDs and success message
     * @throws ResponseEntity 401 if user is not authenticated
     */
    @PostMapping
    fun createEvent(
        @Valid @RequestBody request: CreateUpdateEventDto,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<CreateEventResponse> {
        val organizerId = requireAuthenticatedUserId(user)
        val events = organizerEventService.createEventByOccurrence(request, organizerId).map { it.id }
        return ResponseEntity.status(HttpStatus.CREATED).body(
            CreateEventResponse(
                events = events,
                message = "Events created successfully"
            )

        )
    }

    /**
     * Update an existing event
     * PUT /api/events/{eventId}
     * 
     * @param eventId The ID of the event to update
     * @param request Updated event details
     * @param user Currently authenticated user (organizer)
     * @return Response with updated event ID and success message
     * @throws ResponseEntity 401 if user is not authenticated
     * @throws ResponseEntity 403 if user is not the event organizer
     */
    @PutMapping("/{eventId}")
    fun updateEvent(
        @PathVariable eventId: UUID,
        @Valid @RequestBody request: CreateUpdateEventDto,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<CreateEventResponse> {
        val organizerId = requireAuthenticatedUserId(user)
        val event = organizerEventService.updateEvent(eventId, request, organizerId)
        return ResponseEntity.ok(
            CreateEventResponse(
                events = listOf(event.id),
                message = "Event updated successfully"
            )
        )
    }

    /**
     * Get detailed information about a specific event
     * GET /api/events/{id}
     *
     * @param id The ID of the event
     * @param user Currently authenticated user (optional, for personalization)
     * @return Event details with all available information
     */
    @GetMapping("/{id}")
    fun getEventById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<EventDetailDto> {
        val eventDetail = eventService.getEventDetailById(id, user?.id)
        return ResponseEntity.ok(eventDetail)
    }

    /**
     * Publish a draft event
     * PATCH /api/events/{eventId}/publish
     * 
     * Transitions an event from DRAFT status to PUBLISHED status, making it visible
     * to all users. Only the event organizer can publish their events.
     * 
     * @param eventId The ID of the event to publish
     * @param request Publishing request with additional metadata
     * @param user Currently authenticated user (organizer)
     * @return Event status response confirming publication
     */
    @PatchMapping("/{eventId}/publish")
    fun publishEvent(
        @PathVariable eventId: UUID,
        @RequestBody request: PublishEventDto,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<EventStatusResponse> {
        return try {
            val organizerId = requireAuthenticatedUserId(user)
            val event = organizerEventService.publishEvent(eventId, organizerId, request)
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
     * 
     * Marks an event as CANCELLED, preventing new registrations but keeping the
     * event record and historical data.
     * Only the event organizer can cancel their events.
     * 
     * @param eventId The ID of the event to cancel
     * @param user Currently authenticated user (organizer)
     * @return Event status response confirming cancellation
     */
    @PatchMapping("/{eventId}/cancel")
    fun cancelEvent(
        @PathVariable eventId: UUID,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<EventStatusResponse> {
        return try {
            val organizerId = requireAuthenticatedUserId(user)
            val event = organizerEventService.cancelEvent(eventId, organizerId)
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
     * 
     * Permanently removes a draft event from the system. Once deleted, the event
     * cannot be recovered. Only draft events can be deleted.
     * Only the event organizer can delete their events.
     * 
     * @param eventId The ID of the event to delete
     * @param user Currently authenticated user (organizer)
     * @return No content response on success
     */
    @DeleteMapping("/{eventId}")
    fun deleteEvent(
        @PathVariable eventId: UUID,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<Map<String, String>> {
        return try {
            val organizerId = requireAuthenticatedUserId(user)
            organizerEventService.deleteEvent(eventId, organizerId)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(mapOf("message" to (e.message ?: "Cannot delete event")))
        }
    }

    private fun requireAuthenticatedUserId(user: User?): UUID {
        return user?.id ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")
    }
}

data class EventStatusResponse(
    val id: UUID?,
    val status: String?,
    val message: String
)


