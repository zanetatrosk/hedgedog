package com.example.bedanceapp.service

import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.registration.RegistrationData
import com.example.bedanceapp.service.registration.RegistrationStrategyFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service for loading and formatting event registration data for display/export.
 * Uses strategy pattern to delegate to mode-specific implementations.
 */
@Service
class EventRegistrationDataService(
    private val eventRepository: EventRepository,
    private val eventService: EventService,
    private val registrationStrategyFactory: RegistrationStrategyFactory
) {

    /**
     * Get all statistics for an event including registration data
     */
    @Transactional(readOnly = true)
    fun getAllStatsByEvent(eventID: UUID): StatsResponse {
        val event = eventRepository.findById(eventID)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventID") }

        // Delegate to appropriate strategy based on registration mode
        val strategy = registrationStrategyFactory.getStrategy(event.registrationMode)
        val registrationData = strategy.getRegistrationData(event)

        return StatsResponse(
            eventName = event.eventName,
            date = event.eventDate.toString(),
            recurringDates = eventService.getUpcomingDates(event.parentEventId),
            registrationData = registrationData
        )
    }
}

data class StatsResponse(
    val eventName: String,
    val date: String,
    val recurringDates: List<RecurringDateInfo>,
    val registrationData: RegistrationData
)


