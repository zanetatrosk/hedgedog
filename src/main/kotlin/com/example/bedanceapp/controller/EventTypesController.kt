package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CodebookItem
import com.example.bedanceapp.model.toCodebook
import com.example.bedanceapp.model.toCodebookList
import com.example.bedanceapp.service.EventTypeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/event-types")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class EventTypesController(
    private val eventTypeService: EventTypeService
) {

    @GetMapping
    fun getAllEventTypes(): ResponseEntity<List<CodebookItem>> {
        val types = eventTypeService.findAll()
        return ResponseEntity.ok(types.toCodebookList())
    }
}