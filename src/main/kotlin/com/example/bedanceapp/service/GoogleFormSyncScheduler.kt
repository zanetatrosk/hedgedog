package com.example.bedanceapp.service

import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.registration.GoogleFormSyncService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GoogleFormSyncScheduler(
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    private val googleFormSyncService: GoogleFormSyncService
) {
    private val logger = LoggerFactory.getLogger(GoogleFormSyncScheduler::class.java)

    /**
     * Sync Google Form data for all events using Google Forms
     * Runs every hour (3,600,000 milliseconds)
     * No user authentication required - uses stored organizer tokens
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    fun syncAllGoogleForms() {
        logger.info("Starting scheduled Google Form sync")

        try {
            // Find all events with GOOGLE_FORM registration mode that have a formId
            val googleFormEvents = eventRegistrationSettingsRepository.findAll()
                .filter { it.registrationMode == RegistrationMode.GOOGLE_FORM && !it.formId.isNullOrBlank() && it.event?.status == EventStatus.PUBLISHED }

            logger.info("Found ${googleFormEvents.size} events with Google Forms to sync")

            var successCount = 0
            var failureCount = 0

            googleFormEvents.forEach { settings ->
                try {
                    logger.debug("Syncing form data for event: {}", settings.eventId)
                    settings.event?.let { googleFormSyncService.syncRegistrationData(it.id!!, it.organizerId, it.maxAttendees) }
                    successCount++
                    logger.debug("Successfully synced event: {}", settings.eventId)
                } catch (e: Exception) {
                    failureCount++
                    logger.error("Failed to sync event ${settings.eventId}: ${e.message}", e)
                    // Continue with other events even if one fails
                }
            }

            logger.info("Completed Google Form sync. Success: $successCount, Failures: $failureCount")
        } catch (e: Exception) {
            logger.error("Error during scheduled Google Form sync", e)
        }
    }
}

