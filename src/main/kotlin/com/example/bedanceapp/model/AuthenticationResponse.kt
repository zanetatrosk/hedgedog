package com.example.bedanceapp.model

import java.util.UUID

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String,
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

