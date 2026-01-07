package com.example.bedanceapp.service

import com.example.bedanceapp.model.DanceStyle
import com.example.bedanceapp.repository.DanceStyleRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DanceStyleService(
    private val danceStyleRepository: DanceStyleRepository
) {
    fun findAll(): List<DanceStyle> = danceStyleRepository.findAll()

    fun findById(id: UUID): DanceStyle {
        return danceStyleRepository.findById(id)
            .orElseThrow { NoSuchElementException("DanceStyle not found with id: $id") }
    }
}
