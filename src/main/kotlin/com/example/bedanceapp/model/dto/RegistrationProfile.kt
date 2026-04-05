package com.example.bedanceapp.model.dto

import com.example.bedanceapp.model.EventMedia

data class RegistrationProfile(
    val name: String,
    val role: String,
    val avatar: EventMedia? = null,
    val linkToProfile: String
)