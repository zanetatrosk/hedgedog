package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CodebookItem
import com.example.bedanceapp.model.toCodebook
import com.example.bedanceapp.model.toCodebookList
import com.example.bedanceapp.service.DanceStyleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/dance-styles")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class DanceStylesController(
    private val danceStyleService: DanceStyleService
) {

    @GetMapping
    fun getAllDanceStyles(): ResponseEntity<List<CodebookItem>> {
        val styles = danceStyleService.findAll()
        return ResponseEntity.ok(styles.toCodebookList())
    }
}