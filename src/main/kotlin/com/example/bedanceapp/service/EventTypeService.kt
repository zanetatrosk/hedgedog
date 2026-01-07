package com.example.bedanceapp.service

import com.example.bedanceapp.model.EventType
import com.example.bedanceapp.repository.EventTypeRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EventTypeService(
    private val eventTypeRepository: EventTypeRepository
) {
    fun findAll(): List<EventType> = eventTypeRepository.findAll()

    fun findById(id: UUID): EventType {
        return eventTypeRepository.findById(id)
            .orElseThrow { NoSuchElementException("EventType not found with id: $id") }
    }
}

