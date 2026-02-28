package com.example.bedanceapp.model

/**
 * Unified token request supporting multiple grant types
 * Based on OAuth 2.0 specification
 */
data class TokenRequest(
    val grantType: String,  // "authorization_code" (for login/incremental) or "refresh_token"
    val code: String? = null,  // Authorization code from Google (required if grant_type is authorization_code)
    val redirectUri: String? = null,  // Redirect URI used in auth request (required if grant_type is authorization_code)
    val refreshToken: String? = null  // JWT refresh token (required if grant_type is refresh_token)
)
