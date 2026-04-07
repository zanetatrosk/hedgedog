package com.example.bedanceapp.model

data class EventRegistrationDto(
    val registrationId: String,
    val user: RegistrationUserDto?,
    val level: String? = null,
    val role: String? = null
)

data class RegistrationUserDto(
    val userId: String,
    val name: String,
    val avatar: EventMedia? = null
)

