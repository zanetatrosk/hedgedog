package com.example.bedanceapp.model

import java.time.LocalDateTime
import java.util.UUID

data class EventRegistrationWithUserDto(
    val id: UUID?,
    val eventId: UUID,
    val userId: UUID,
    val status: String,
    val role: DancerRoleDto?,
    val paid: Boolean,
    val userProfile: UserProfileDto?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class DancerRoleDto(
    val id: UUID?,
    val name: String
)

data class UserProfileDto(
    val userId: UUID,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val bio: String?,
    val city: String?,
    val country: String?
)

