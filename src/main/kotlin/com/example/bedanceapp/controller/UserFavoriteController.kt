package com.example.bedanceapp.controller

import com.example.bedanceapp.model.UserFavorite
import com.example.bedanceapp.service.UserFavoriteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/favorites")
class UserFavoriteController(
    private val userFavoriteService: UserFavoriteService
) {
    /**
     * Add a user's interest in an event (favorite it)
     * POST /api/favorites
     */
    @PostMapping
    fun addFavorite(
        @RequestBody request: AddFavoriteRequest
    ): ResponseEntity<UserFavorite> {
        val favorite = userFavoriteService.addUserInterest(request.userId, request.eventId)
        return ResponseEntity.status(HttpStatus.CREATED).body(favorite)
    }

    /**
     * Remove a user's interest in an event (unfavorite it)
     * DELETE /api/favorites
     */
    @DeleteMapping
    fun removeFavorite(
        @RequestParam userId: UUID,
        @RequestParam eventId: UUID
    ): ResponseEntity<Map<String, String>> {
        val removed = userFavoriteService.removeUserInterest(userId, eventId)
        return if (removed) {
            ResponseEntity.ok(mapOf("message" to "Favorite removed successfully"))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all favorites for a user
     * GET /api/favorites/users/{userId}
     */
    @GetMapping("/users/{userId}")
    fun getUserFavorites(@PathVariable userId: UUID): ResponseEntity<List<UserFavorite>> {
        val favorites = userFavoriteService.getUserFavorites(userId)
        return ResponseEntity.ok(favorites)
    }

    /**
     * Get all users interested in an event
     * GET /api/favorites/events/{eventId}/users
     */
    @GetMapping("/events/{eventId}/users")
    fun getInterestedUsers(@PathVariable eventId: UUID): ResponseEntity<List<UserFavorite>> {
        val favorites = userFavoriteService.getInterestedUsers(eventId)
        return ResponseEntity.ok(favorites)
    }
}

data class AddFavoriteRequest(
    val userId: UUID,
    val eventId: UUID
)

