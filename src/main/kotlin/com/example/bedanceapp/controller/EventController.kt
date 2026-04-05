package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CreateEventRequest
import com.example.bedanceapp.model.CreateEventResponse
import com.example.bedanceapp.model.EventDetailData
import com.example.bedanceapp.model.EventDto
import com.example.bedanceapp.model.PagedResponse
import com.example.bedanceapp.model.PublishEventRequest
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

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class EventController(
    private val eventService: EventService,
    private val organizerEventService: OrganizerEventService
) {

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
    ): ResponseEntity<PagedResponse<EventDto>> {
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

    @PostMapping
    fun createEvent(
        @Valid @RequestBody request: CreateEventRequest,
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

    @PutMapping("/{eventId}")
    fun updateEvent(
        @PathVariable eventId: UUID,
        @Valid @RequestBody request: CreateEventRequest,
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

    @GetMapping("/{id}")
    fun getEventById(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<EventDetailData> {
        val eventDetail = eventService.getEventDetailById(id, user?.id)
        return ResponseEntity.ok(eventDetail)
    }

    /**
     * Publish a draft event
     * PATCH /api/events/{eventId}/publish
     */
    @PatchMapping("/{eventId}/publish")
    fun publishEvent(
        @PathVariable eventId: UUID,
        @RequestBody request: PublishEventRequest,
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

/**
 * Response body for event status changes
 */
data class EventStatusResponse(
    val id: UUID?,
    val status: String?,
    val message: String
)


