package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CreateEventRequest
import com.example.bedanceapp.model.CreateEventResponse
import com.example.bedanceapp.model.EventDetailData
import com.example.bedanceapp.model.EventDto
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.PagedResponse
import com.example.bedanceapp.service.EventService
import com.example.bedanceapp.service.EventRegistrationService
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
    private val eventRegistrationService: EventRegistrationService
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
        @RequestParam(required = false) eventTypes: List<UUID>?
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
            eventTypeIds = eventTypes
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
            paid = request.paid ?: false
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

}

/**
 * Request body for event registration
 */
data class RegisterEventRequest(
    val status: String,  // interested, going, waitlisted
    val roleId: UUID? = null,  // Leader, Follower, Both
    val paid: Boolean? = false
)

