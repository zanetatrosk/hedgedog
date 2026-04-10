package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.event.CreateEventService
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
    private val createEventService: CreateEventService,
    private val registrationStrategyFactory: RegistrationStrategyFactory,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {

    /**
     * Get all statistics for an event including registration data
     */
    @Transactional
    fun getAllRegistrationsByEvent(eventID: UUID): StatsResponse {
        val event = eventRepository.findById(eventID)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventID") }

        // Get registration settings to determine mode
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventID)
        val registrationMode = registrationSettings?.registrationMode ?: RegistrationMode.OPEN

        // Delegate to appropriate strategy based on registration mode
        val strategy = registrationStrategyFactory.getStrategy(registrationMode)
        val registrationData = strategy.getRegistrationData(event)

        return StatsResponse(
            eventId = event.id!!,
            eventName = event.eventName,
            date = event.eventDate.toString(),
            recurringDates = createEventService.getUpcomingDates(event.parentEventId),
            registrationData = registrationData,
            registrationMode = registrationMode
        )
    }
}

data class StatsResponse(
    val eventId: UUID,
    val eventName: String,
    val date: String,
    val recurringDates: List<RecurringDateInfo>,
    val registrationData: RegistrationData,
    val registrationMode: RegistrationMode,
)


