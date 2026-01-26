package com.example.bedanceapp.repository

import com.example.bedanceapp.model.UserMedia
import com.example.bedanceapp.model.UserMediaId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserMediaRepository : JpaRepository<UserMedia, UserMediaId> {
    fun findByUserId(userId: UUID): List<UserMedia>
    fun deleteByUserId(userId: UUID)
}

