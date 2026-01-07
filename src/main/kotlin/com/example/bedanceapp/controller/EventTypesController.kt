package com.example.bedanceapp.controller

import com.example.bedanceapp.model.EventType
import com.example.bedanceapp.service.EventTypeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/event-types")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class EventTypesController(
    private val eventTypeService: EventTypeService
) {

    @GetMapping
    fun getAllEventTypes(): ResponseEntity<List<EventTypeResponse>> {
        val types = eventTypeService.findAll()
        return ResponseEntity.ok(types.map { EventTypeResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getEventTypeById(@PathVariable id: UUID): ResponseEntity<EventTypeResponse> {
        val type = eventTypeService.findById(id)
        return ResponseEntity.ok(EventTypeResponse.from(type))
    }
}

data class EventTypeResponse(
    val id: UUID?,
    val name: String
) {
    companion object {
        fun from(eventType: EventType) = EventTypeResponse(
            id = eventType.id,
            name = eventType.name
        )
    }
}
