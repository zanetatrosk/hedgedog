package com.example.bedanceapp.service

import com.example.bedanceapp.repository.MediaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Service
class MediaCleanupService(
    private val mediaRepository: MediaRepository,
    private val mediaStorageService: MediaStorageService,
    private val transactionTemplate: TransactionTemplate
) {
    private val logger = LoggerFactory.getLogger(MediaCleanupService::class.java)

    fun cleanupOrphanedMedia(): Int {
        val orphanedMedia = mediaRepository.findOrphanedMedia()
        if (orphanedMedia.isEmpty()) {
            logger.debug("No orphaned media found")
            return 0
        }

        val deletableIds = mutableListOf<UUID>()

        orphanedMedia.forEach { media ->
            try {
                mediaStorageService.delete(media.filePath)
                media.id?.let { deletableIds.add(it) }
            } catch (e: Exception) {
                // Keep DB row when storage deletion fails so the job can retry later.
                logger.warn("Failed to delete media object {} for media {}", media.filePath, media.id, e)
            }
        }

        if (deletableIds.isEmpty()) {
            return 0
        }
        transactionTemplate.executeWithoutResult {
            mediaRepository.deleteAllByIdInBatch(deletableIds)
        }
        return deletableIds.size
    }

}

