package com.example.bedanceapp.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("MediaCleanupScheduler Tests")
class MediaCleanupSchedulerTest {

    @Mock private lateinit var mediaCleanupService: MediaCleanupService

    private lateinit var scheduler: MediaCleanupScheduler

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        scheduler = MediaCleanupScheduler(mediaCleanupService)
    }

    @Test
    fun `cleanupOrphanedMedia delegates to cleanup service`() {
        whenever(mediaCleanupService.cleanupOrphanedMedia()).thenReturn(3)

        scheduler.cleanupOrphanedMedia()

        verify(mediaCleanupService).cleanupOrphanedMedia()
    }

    @Test
    fun `cleanupOrphanedMedia swallows cleanup exceptions`() {
        whenever(mediaCleanupService.cleanupOrphanedMedia()).thenThrow(RuntimeException("storage down"))

        scheduler.cleanupOrphanedMedia()

        verify(mediaCleanupService).cleanupOrphanedMedia()
    }
}

