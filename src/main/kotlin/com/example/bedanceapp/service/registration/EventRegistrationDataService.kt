package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.validation.EventAccessValidator
import com.example.bedanceapp.service.event.EventService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service for loading and formatting event registration data for display/export.
 * Uses strategy pattern to delegate to mode-specific implementations.
 */
@Service
class EventRegistrationDataService(
    private val eventAccessValidator: EventAccessValidator,
    private val eventService: EventService,
    private val registrationStrategyFactory: RegistrationStrategyFactory,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {

    @Transactional
    fun getAllRegistrationsByEvent(eventID: UUID, userId: UUID): StatsResponse {
        val event = eventAccessValidator.requireOwnedEvent(eventID, userId)

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
            recurringDates = eventService.getUpcomingDates(event.parentEventId, true),
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


