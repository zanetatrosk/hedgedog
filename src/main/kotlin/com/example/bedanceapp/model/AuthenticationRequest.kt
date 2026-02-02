package com.example.bedanceapp.model

data class AuthenticationRequest(
    val code: String,
    val redirectUri: String
)
