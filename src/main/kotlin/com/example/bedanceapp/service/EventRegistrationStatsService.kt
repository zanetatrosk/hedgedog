package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.repository.EventRegistrationRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service responsible for event registration statistics and analytics
 * Handles counting and aggregating registration data
 */
@Service
class EventRegistrationStatsService(
    private val eventRegistrationRepository: EventRegistrationRepository
) {

    fun getRegistrationCountByEventId(eventId: UUID?, state: RegistrationStatus): Int {
        if (eventId == null) {
            return 0
        }
        val eventRegistrations = eventRegistrationRepository.findByEventIdAndStatus(eventId, state)
        return eventRegistrations.size
    }
}

