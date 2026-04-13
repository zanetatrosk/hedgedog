package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CodebookItem
import com.example.bedanceapp.model.toCodebookList
import com.example.bedanceapp.service.DanceStyleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dance-styles")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class DanceStylesController(
    private val danceStyleService: DanceStyleService
) {

    /**
     * Get all available dance styles
     * GET /api/dance-styles
     *
     * @return List of codebook items representing dance styles
     */
    @GetMapping
    fun getAllDanceStyles(): ResponseEntity<List<CodebookItem>> {
        val styles = danceStyleService.findAll()
        return ResponseEntity.ok(styles.toCodebookList())
    }
}