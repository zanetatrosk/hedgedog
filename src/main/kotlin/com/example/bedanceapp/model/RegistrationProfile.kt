package com.example.bedanceapp.model

data class RegistrationProfile(
    val name: String,
    val role: String,
    val avatar: EventMedia? = null,
    val linkToProfile: String
)
