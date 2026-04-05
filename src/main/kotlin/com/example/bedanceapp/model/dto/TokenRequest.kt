package com.example.bedanceapp.model.dto

/**
 * Token request supporting Google OAuth2 authorization code flow.
 */
data class TokenRequest(
    val grantType: String,  // Must be "authorization_code"
    val code: String? = null,  // Authorization code from Google (required if grant_type is authorization_code)
    val redirectUri: String? = null  // Redirect URI used in auth request (required if grant_type is authorization_code)
)
