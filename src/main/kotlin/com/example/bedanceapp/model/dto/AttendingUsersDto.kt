package com.example.bedanceapp.model

import com.example.bedanceapp.model.dto.RegistrationProfile

data class AttendingUsersDto(
    val role: String,
    val count: Int,
    val attending: List<RegistrationProfile>
)

