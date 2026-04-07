package com.example.bedanceapp.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MediaCleanupScheduler(
    private val mediaCleanupService: MediaCleanupService
) {
    private val logger = LoggerFactory.getLogger(MediaCleanupScheduler::class.java)

    @Scheduled(cron = "\${media.cleanup.cron:0 0 3 * * *}")
    fun cleanupOrphanedMedia() {
        logger.info("Starting orphaned media cleanup job")

        try {
            val deleted = mediaCleanupService.cleanupOrphanedMedia()
            logger.info("Completed orphaned media cleanup job. Deleted rows: {}", deleted)
        } catch (e: Exception) {
            logger.error("Error while running orphaned media cleanup job", e)
        }
    }
}

