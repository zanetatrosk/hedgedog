package com.example.bedanceapp.controller

import com.example.bedanceapp.config.UrlConfig
import com.example.bedanceapp.model.EventMedia
import com.example.bedanceapp.service.MediaService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
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
     * Upload image or video
     */
    @PostMapping("/upload")
    fun uploadMedia(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<EventMedia> {

        // 🔐 auth can be checked inside service
        val media = mediaService.upload(file) ?: return ResponseEntity.badRequest().build()

        return ResponseEntity.ok(
            media
        )
    }

    /**
     * Stream image / video (protected)
     */
    @GetMapping("/{id}")
    fun getMedia(
        @PathVariable id: UUID,
        response: HttpServletResponse
    ) {
        // 🔐 auth / authorization inside service
        val (_, stream) = mediaService.getMedia(id)

        response.setHeader("Cache-Control", "private, max-age=3600")
        response.setHeader("Accept-Ranges", "bytes")

        stream.use { input ->
            input.copyTo(response.outputStream)
        }
    }
}
