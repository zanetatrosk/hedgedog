package com.example.bedanceapp.controller

import com.example.bedanceapp.model.EventMedia
import com.example.bedanceapp.model.User
import com.example.bedanceapp.service.MediaService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
@RequestMapping("/api/media")
class MediaController(
    private val mediaService: MediaService
) {

    /**
     * Upload image or video file
     * POST /api/media
     * 
     * @param user Currently authenticated user (media owner)
     * @param file The multipart file to upload (image or video)
     * @return Uploaded media metadata including file ID and details
     */
    @PostMapping
    fun uploadMedia(
        @AuthenticationPrincipal user: User,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<EventMedia> {
        val ownerId = user.id ?: return ResponseEntity.status(403).build()
        val media = mediaService.upload(file, ownerId) ?: return ResponseEntity.badRequest().build()

        return ResponseEntity.ok(
            media
        )
    }

    /**
     * Stream/download image or video
     * GET /api/media/{id}
     * 
     * @param id The ID of the media file to retrieve
     * @param response HttpServletResponse for streaming the file content
     */
    @GetMapping("/{id}")
    fun getMedia(
        @PathVariable id: UUID,
        response: HttpServletResponse
    ) {
        val (_, stream) = mediaService.getMedia(id)

        response.setHeader("Cache-Control", "private, max-age=3600")
        response.setHeader("Accept-Ranges", "bytes")

        stream.use { input ->
            input.copyTo(response.outputStream)
        }
    }

    /**
     * Delete media file
     * DELETE /api/media/{id}
     * 
     * Permanently removes a media file. Only the owner (user who uploaded the file)
     * can delete it. Once deleted, the media file cannot be recovered.
     * 
     * @param id The ID of the media file to delete
     * @param user Currently authenticated user (must be the media owner)
     * @return No content response on successful deletion
     */
    @DeleteMapping("/{id}")
    fun deleteMedia(
        @PathVariable id: UUID,
        @AuthenticationPrincipal user: User
    ): ResponseEntity<Void> {
        val ownerId = user.id ?: return ResponseEntity.status(403).build()
        mediaService.deleteMedia(id, ownerId)
        return ResponseEntity.noContent().build()
    }
}
