package com.example.bedanceapp.model

import java.util.UUID

data class AuthResponse(
    val accessToken: String,
    val expiresIn: Long,
    val user: UserDto
)

data class UserDto(
    val id: UUID,
    val email: String,
    val provider: String,
    val hasProfile: Boolean,
    val grantedScopes: List<String>
)

