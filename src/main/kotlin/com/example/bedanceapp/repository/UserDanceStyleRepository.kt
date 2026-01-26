package com.example.bedanceapp.repository

import com.example.bedanceapp.model.UserDanceStyle
import com.example.bedanceapp.model.UserDanceStyleId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserDanceStyleRepository : JpaRepository<UserDanceStyle, UserDanceStyleId> {
    fun findByUserId(userId: UUID): List<UserDanceStyle>
    fun deleteByUserId(userId: UUID)
}

