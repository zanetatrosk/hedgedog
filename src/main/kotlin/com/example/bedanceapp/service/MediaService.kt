package com.example.bedanceapp.service

import com.example.bedanceapp.config.UrlConfig
import com.example.bedanceapp.model.EventMedia
import com.example.bedanceapp.model.Media
import com.example.bedanceapp.repository.MediaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.util.UUID

@Service
class MediaService(
    private val repository: MediaRepository,
    private val storage: MediaStorageService, private val urlConfig: UrlConfig
) {

    fun getMedia(id: UUID): Pair<Media, InputStream> {
        val media = repository.findById(id)
            .orElseThrow { RuntimeException("Media not found for id: $id") }

        val stream = storage.get(media.filePath)
        return media to stream
    }

    open fun mapToDTO(media: Media?): EventMedia? {
        if(media == null) return null;
        return EventMedia(
            type = media.mediaType,
            url = "${urlConfig.baseUrl}/api/media/${media.id}",
            id = media.id!!
        )
    }

    fun upload(file: MultipartFile, ownerId: UUID): EventMedia? {
        val objectKey = UUID.randomUUID().toString()

        try {
            storage.upload(
                objectKey,
                file.inputStream,
                file.size,
                file.contentType ?: "application/octet-stream"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val media = repository.save(
            Media(
                mediaType = if (file.contentType?.startsWith("video") == true) "video" else "image",
                filePath = objectKey,
                ownerId = ownerId
            )
        )

        return mapToDTO(media)
    }

    fun deleteMedia(mediaId: UUID, ownerId: UUID) {
        val media = repository.findByIdAndOwnerId(mediaId, ownerId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found")

        try {
            storage.delete(media.filePath)
            repository.delete(media)
        } catch (e: ResponseStatusException) {
            throw e
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete media", e)
        }
    }
}
