package com.example.bedanceapp.controller

import com.example.bedanceapp.model.*
import com.example.bedanceapp.service.user.UserEventService
import com.example.bedanceapp.service.user.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class UserController(
    private val userEventService: UserEventService,
    private val userService: UserService
) {

    @GetMapping("/{userId}/events")
    fun getUserEvents(
        @PathVariable userId: UUID,
        @RequestParam(required = false) filter: StatusFilter?
    ): ResponseEntity<List<MyEvent>> {
        val events = userEventService.getUserEvents(userId, filter)
        return ResponseEntity.ok(events)
    }

    /**
     * Get user profile data
     * GET /api/users/{userId}
     */
    @GetMapping("/{userId}")
    fun getUserProfile(
        @PathVariable userId: UUID
    ): ResponseEntity<ProfileData> {
        val profileData = userService.getProfileData(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(profileData)
    }

    /**
     * Update user profile data
     * PUT /api/users/{userId}
     */
    @PutMapping("/{userId}")
    fun updateUserProfile(
        @PathVariable userId: UUID,
        @RequestBody request: ProfileData
    ): ResponseEntity<ProfileData> {
        return try {
            val profileData = userService.updateProfileData(userId, request)
            ResponseEntity.ok(profileData)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}

