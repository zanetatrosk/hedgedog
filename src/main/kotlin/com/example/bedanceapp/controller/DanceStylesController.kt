package com.example.bedanceapp.controller

import com.example.bedanceapp.model.DanceStyle
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
    fun getAllDanceStyles(): ResponseEntity<List<DanceStyleResponse>> {
        val styles = danceStyleService.findAll()
        return ResponseEntity.ok(styles.map { DanceStyleResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getDanceStyleById(@PathVariable id: UUID): ResponseEntity<DanceStyleResponse> {
        val style = danceStyleService.findById(id)
        return ResponseEntity.ok(DanceStyleResponse.from(style))
    }
}

data class DanceStyleResponse(
    val id: UUID?,
    val name: String
) {
    companion object {
        fun from(danceStyle: DanceStyle) = DanceStyleResponse(
            id = danceStyle.id,
            name = danceStyle.name
        )
    }
}
