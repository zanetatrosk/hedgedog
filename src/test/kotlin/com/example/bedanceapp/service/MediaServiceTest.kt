package com.example.bedanceapp.service

import com.example.bedanceapp.config.UrlConfig
import com.example.bedanceapp.model.Media
import com.example.bedanceapp.repository.MediaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayInputStream
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DisplayName("MediaService Tests")
class MediaServiceTest {

    @Mock private lateinit var repository: MediaRepository
    @Mock private lateinit var storage: MediaStorageService

    private lateinit var service: MediaService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val urlConfig = UrlConfig().apply { baseUrl = "http://localhost:8080" }
        service = MediaService(repository, storage, urlConfig)
    }

    @Test
    fun `mapToDTO returns null for null media`() {
        assertNull(service.mapToDTO(null))
    }

    @Test
    fun `getMedia returns media with storage stream`() {
        val id = UUID.randomUUID()
        val media = Media(id = id, mediaType = "image", filePath = "obj")
        val stream = ByteArrayInputStream("x".toByteArray())
        whenever(repository.findById(id)).thenReturn(Optional.of(media))
        whenever(storage.get("obj")).thenReturn(stream)

        val result = service.getMedia(id)

        assertEquals(media, result.first)
        assertEquals(stream, result.second)
    }

    @Test
    fun `upload returns dto when storage and save succeed`() {
        val ownerId = UUID.randomUUID()
        val multipart = MockMultipartFile("file", "a.jpg", "image/jpeg", "abc".toByteArray())
        whenever(repository.save(any())).thenAnswer { invocation ->
            val m = invocation.arguments[0] as Media
            m.copy(id = UUID.randomUUID())
        }

        val dto = service.upload(multipart, ownerId)

        assertNotNull(dto)
        assertEquals("image", dto.type)
        verify(storage).upload(any(), any(), any(), any())
    }

    @Test
    fun `upload returns null when storage fails`() {
        val ownerId = UUID.randomUUID()
        val multipart = MockMultipartFile("file", "a.jpg", "image/jpeg", "abc".toByteArray())
        whenever(storage.upload(any(), any(), any(), any())).thenThrow(RuntimeException("down"))

        val dto = service.upload(multipart, ownerId)

        assertNull(dto)
    }

    @Test
    fun `deleteMedia throws not found when owner media mismatch`() {
        val mediaId = UUID.randomUUID()
        whenever(repository.findByIdAndOwnerId(mediaId, UUID.randomUUID())).thenReturn(null)

        val ex = assertThrows<ResponseStatusException> {
            service.deleteMedia(mediaId, UUID.randomUUID())
        }

        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    @Test
    fun `deleteMedia wraps storage exception as internal server error`() {
        val mediaId = UUID.randomUUID()
        val ownerId = UUID.randomUUID()
        val media = Media(id = mediaId, mediaType = "image", filePath = "obj", ownerId = ownerId)
        whenever(repository.findByIdAndOwnerId(mediaId, ownerId)).thenReturn(media)
        whenever(storage.delete("obj")).thenThrow(RuntimeException("failed"))

        val ex = assertThrows<ResponseStatusException> {
            service.deleteMedia(mediaId, ownerId)
        }

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.statusCode)
    }
}

