package com.example.bedanceapp.repository

import com.example.bedanceapp.model.UserFavorite
import com.example.bedanceapp.model.UserFavoriteId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserFavoriteRepository : JpaRepository<UserFavorite, UserFavoriteId> {

    /**
     * Find all favorites for a specific user
     */
    fun findByIdUserId(userId: UUID): List<UserFavorite>

    /**
     * Find all users who favorited a specific event
     */
    fun findByIdEventId(eventId: UUID): List<UserFavorite>

    /**
     * Check if a user has favorited an event
     */
    fun existsByIdUserIdAndIdEventId(userId: UUID, eventId: UUID): Boolean

    /**
     * Count how many users are interested in (favorited) a specific event
     */
    @Query("SELECT COUNT(uf) FROM UserFavorite uf WHERE uf.id.eventId = :eventId")
    fun countInterestedUsersByEventId(@Param("eventId") eventId: UUID): Int

    /**
     * Delete a favorite by user and event
     */
    fun deleteByIdUserIdAndIdEventId(userId: UUID, eventId: UUID): Int
}

