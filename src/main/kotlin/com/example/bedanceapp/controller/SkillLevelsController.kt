package com.example.bedanceapp.controller

import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.service.SkillLevelService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/skill-levels")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class SkillLevelsController(
    private val skillLevelService: SkillLevelService
) {

    @GetMapping
    fun getAllSkillLevels(): ResponseEntity<List<SkillLevelResponse>> {
        val levels = skillLevelService.findAll()
        return ResponseEntity.ok(levels.map { SkillLevelResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getSkillLevelById(@PathVariable id: UUID): ResponseEntity<SkillLevelResponse> {
        val level = skillLevelService.findById(id)
        return ResponseEntity.ok(SkillLevelResponse.from(level))
    }
}

data class SkillLevelResponse(
    val id: UUID?,
    val name: String,
    val levelOrder: Int
) {
    companion object {
        fun from(skillLevel: SkillLevel) = SkillLevelResponse(
            id = skillLevel.id,
            name = skillLevel.name,
            levelOrder = skillLevel.levelOrder
        )
    }
}
