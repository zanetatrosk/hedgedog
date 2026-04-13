package com.example.bedanceapp.controller

import com.example.bedanceapp.model.*
import com.example.bedanceapp.service.user.UserEventService
import com.example.bedanceapp.service.user.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * User Controller
 * 
 * Handles user profile management and user event queries
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class UserController(
    private val userEventService: UserEventService,
    private val userService: UserService
) {

    /**
     * Get paginated list of events for a user
     * GET /api/users/{userId}/events
     * 
     * @param userId The ID of the user
     * @param filter Optional status filter for events (e.g., registered, interested)
     * @param timeline Optional timeline filter (e.g., upcoming, past)
     * @param page Page number for pagination (default: 0)
     * @param size Number of items per page (default: 10)
     * @return Paginated response containing user's events
     */
    @GetMapping("/{userId}/events")
    fun getUserEvents(
        @PathVariable userId: UUID,
        @RequestParam(required = false) filter: StatusFilter?,
        @RequestParam(required = false) timeline: EventTimeline?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<PagedResponse<MyEvent>> {
        val pagedResponse = userEventService.getUserEventsPaginated(userId, filter, timeline, page, size)
        return ResponseEntity.ok(pagedResponse)
    }

    /**
     * Get user profile data
     * GET /api/users/{userId}
     * 
     * @param userId The ID of the user
     * @return User profile data or 404 if user not found
     */
    @GetMapping("/{userId}")
    fun getUserProfile(
        @PathVariable userId: UUID
    ): ResponseEntity<UserProfileDto> {
        val profileData = userService.getProfileData(userId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        return ResponseEntity.ok(profileData)
    }

    /**
     * Update user profile data
     * PUT /api/users/{userId}
     * 
     * @param userId The ID of the user
     * @param request Updated user profile data
     * @return Updated user profile data
     */
    @PutMapping("/{userId}")
    fun updateUserProfile(
        @PathVariable userId: UUID,
        @RequestBody request: UserProfileDto
    ): ResponseEntity<UserProfileDto> {
        return try {
            val profileData = userService.updateProfileData(userId, request)
            ResponseEntity.ok(profileData)
        } catch (_: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        } catch (_: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}

