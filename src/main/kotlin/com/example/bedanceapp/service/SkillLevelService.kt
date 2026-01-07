package com.example.bedanceapp.service

import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.repository.SkillLevelRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SkillLevelService(
    private val skillLevelRepository: SkillLevelRepository
) {
    fun findAll(): List<SkillLevel> = skillLevelRepository.findAll()

    fun findById(id: UUID): SkillLevel {
        return skillLevelRepository.findById(id)
            .orElseThrow { NoSuchElementException("SkillLevel not found with id: $id") }
    }
}

