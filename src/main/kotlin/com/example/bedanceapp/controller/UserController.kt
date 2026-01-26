package com.example.bedanceapp.controller

import com.example.bedanceapp.model.MyEvent
import com.example.bedanceapp.model.RsvpStatus
import com.example.bedanceapp.model.StatusFilter
import com.example.bedanceapp.service.UserEventService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class UserController(
    private val userEventService: UserEventService
) {

    @GetMapping("/{userId}/events")
    fun getUserEvents(
        @PathVariable userId: UUID,
        @RequestParam(required = false) filter: StatusFilter?
    ): ResponseEntity<List<MyEvent>> {
        val events = userEventService.getUserEvents(userId, filter)
        return ResponseEntity.ok(events)
    }
}

