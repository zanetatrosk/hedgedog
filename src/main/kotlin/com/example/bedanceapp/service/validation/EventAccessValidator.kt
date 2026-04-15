package com.example.bedanceapp.service.validation

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.repository.EventRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EventAccessValidator(
    private val eventRepository: EventRepository
) {
    fun requireOwnedEvent(eventId: UUID, organizerId: UUID, vararg allowedStatuses: EventStatus): Event {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        require(event.organizerId == organizerId) {
            "Only the event organizer can manage this event"
        }

        if (allowedStatuses.isNotEmpty()) {
            require(event.status in allowedStatuses) {
                "Action not allowed for events with status: ${event.status}. Required: ${allowedStatuses.joinToString()}"
            }
        }

        return event
    }
}


