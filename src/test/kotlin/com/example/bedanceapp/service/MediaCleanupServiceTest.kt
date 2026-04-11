package com.example.bedanceapp.service

import com.example.bedanceapp.model.Media
import com.example.bedanceapp.repository.MediaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("MediaCleanupService Tests")
class MediaCleanupServiceTest {

    @Mock private lateinit var mediaRepository: MediaRepository
    @Mock private lateinit var mediaStorageService: MediaStorageService
    @Mock private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var service: MediaCleanupService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = MediaCleanupService(mediaRepository, mediaStorageService, transactionTemplate)
    }

    @Test
    fun `cleanupOrphanedMedia returns zero when no orphaned media`() {
        whenever(mediaRepository.findOrphanedMedia()).thenReturn(emptyList())

        val deleted = service.cleanupOrphanedMedia()

        assertEquals(0, deleted)
    }

    @Test
    fun `cleanupOrphanedMedia deletes rows for successfully removed objects`() {
        val first = Media(id = UUID.randomUUID(), mediaType = "image", filePath = "a.jpg")
        val second = Media(id = UUID.randomUUID(), mediaType = "image", filePath = "b.jpg")
        whenever(mediaRepository.findOrphanedMedia()).thenReturn(listOf(first, second))
        doAnswer {
            val callback = it.arguments[0] as java.util.function.Consumer<org.springframework.transaction.TransactionStatus>
            callback.accept(org.mockito.kotlin.mock())
            null
        }.whenever(transactionTemplate).executeWithoutResult(any())

        val deleted = service.cleanupOrphanedMedia()

        assertEquals(2, deleted)
        verify(mediaStorageService).delete("a.jpg")
        verify(mediaStorageService).delete("b.jpg")
        verify(mediaRepository).deleteAllByIdInBatch(listOf(first.id!!, second.id!!))
    }
}

