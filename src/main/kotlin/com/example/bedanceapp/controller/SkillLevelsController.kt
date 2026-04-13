package com.example.bedanceapp.controller

import com.example.bedanceapp.model.OrderedCodebookItem
import com.example.bedanceapp.model.toOrderedCodebook
import com.example.bedanceapp.service.SkillLevelService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/skill-levels")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class SkillLevelsController(
    private val skillLevelService: SkillLevelService
) {

    /**
     * Get all available skill levels
     * GET /api/skill-levels
     * @return List of codebook items representing skill levels
     */
    @GetMapping
    fun getAllSkillLevels(): ResponseEntity<List<OrderedCodebookItem>> {
        val levels = skillLevelService.findAll()
        return ResponseEntity.ok(levels.map { level -> level.toOrderedCodebook() })
    }
}
