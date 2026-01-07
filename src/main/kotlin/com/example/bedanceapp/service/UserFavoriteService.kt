package com.example.bedanceapp.service

import com.example.bedanceapp.model.UserFavorite
import com.example.bedanceapp.model.UserFavoriteId
import com.example.bedanceapp.repository.UserFavoriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserFavoriteService(
    private val userFavoriteRepository: UserFavoriteRepository
) {

    /**
     * Save a new user interest (favorite) for an event
     */
    @Transactional
    fun addUserInterest(userId: UUID, eventId: UUID): UserFavorite {
        // Check if already exists
        if (userFavoriteRepository.existsByIdUserIdAndIdEventId(userId, eventId)) {
            throw IllegalStateException("User $userId has already favorited event $eventId")
        }

        val favoriteId = UserFavoriteId(userId = userId, eventId = eventId)
        val userFavorite = UserFavorite(id = favoriteId)
        return userFavoriteRepository.save(userFavorite)
    }

    /**
     * Remove user interest (unfavorite) for an event
     */
    @Transactional
    fun removeUserInterest(userId: UUID, eventId: UUID): Boolean {
        val deleted = userFavoriteRepository.deleteByIdUserIdAndIdEventId(userId, eventId)
        return deleted > 0
    }

    /**
     * Get all favorites for a user
     */
    @Transactional(readOnly = true)
    fun getUserFavorites(userId: UUID): List<UserFavorite> {
        return userFavoriteRepository.findByIdUserId(userId)
    }

    /**
     * Get all users interested in an event
     */
    @Transactional(readOnly = true)
    fun getInterestedUsers(eventId: UUID): List<UserFavorite> {
        return userFavoriteRepository.findByIdEventId(eventId)
    }
}

