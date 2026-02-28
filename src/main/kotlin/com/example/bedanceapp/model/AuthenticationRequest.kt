package com.example.bedanceapp.model

/**
 * Authentication request for Authorization Code Model
 * Based on: https://developers.google.com/identity/oauth2/web/guides/use-code-model
 */
data class AuthenticationRequest(
    val code: String,  // Authorization code from Google Identity Services
    val redirectUri: String  // Redirect URI used in the authorization request (e.g., "postmessage" for code model or actual URI)
)
