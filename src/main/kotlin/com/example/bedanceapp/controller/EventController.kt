package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CreateEventRequest
import com.example.bedanceapp.model.CreateEventResponse
import com.example.bedanceapp.model.EventDto
import com.example.bedanceapp.service.EventService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class EventController(private val eventService: EventService) {

    @GetMapping
    fun getEvents(): List<EventDto> {
        return eventService.getAllPublishedEvents()
    }

    @PostMapping
    fun createEvent(
        @RequestBody request: CreateEventRequest,
        @RequestHeader("X-User-Id") organizerId: UUID  // TODO: Replace with actual authentication
    ): ResponseEntity<CreateEventResponse> {
        val events = eventService.createEventByOccurance(request, organizerId).map { it.id }
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

//    @GetMapping("/{id}")
//    fun getEventById(@PathVariable id: UUID): EventDto {
//        return eventService.getAllEvent().firstOrNull { it.id == id.toString() }
//    }
}
