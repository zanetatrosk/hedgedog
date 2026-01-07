package com.example.bedanceapp.model

data class AttendingUsersDTO(
    val role: String,
    val count: Int,
    val attending: List<RegistrationProfile>
)
