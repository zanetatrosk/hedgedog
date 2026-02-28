package com.example.bedanceapp.controller

import com.example.bedanceapp.model.*
import com.example.bedanceapp.service.AuthenticationService
import com.example.bedanceapp.service.GoogleOAuth2Service
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * Authentication Controller using Authorization Code Model
 * Based on: https://developers.google.com/identity/oauth2/web/guides/use-code-model
 *
 * Frontend uses Google Identity Services (GIS) library to obtain authorization code,
 * then sends the code to these endpoints for token exchange.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class AuthenticationController(
    private val authenticationService: AuthenticationService,
    private val googleOAuth2Service: GoogleOAuth2Service
) {

    /**
     * Unified token endpoint for all authentication flows
     *
     * Supports:
     * - authorization_code: Initial login or incremental authorization
     * - refresh_token: Refresh JWT access tokens
     *
     * @param request TokenRequest with grant_type and appropriate parameters
     * @param user Current authenticated user (optional, required for incremental auth)
     * @return AuthenticationResponse with JWT tokens and user info
     */
    @PostMapping("/token")
    fun token(
        @RequestBody request: TokenRequest,
        @AuthenticationPrincipal user: User?
    ): ResponseEntity<AuthenticationResponse> {
        val response = authenticationService.processToken(request, user)
        return ResponseEntity.ok(response)
    }

    /**
     * Get current authenticated user info
     */
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal user: User): ResponseEntity<UserDto> {
        return ResponseEntity.ok(UserDto(
            id = user.id!!,
            email = user.email,
            provider = user.provider,
            hasProfile = user.profile != null,
            grantedScopes = user.googleScopes?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        ))
    }


    /**
     * Logout endpoint
     * Optionally revokes Google tokens if requested
     */
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal user: User?,
        @RequestParam(required = false, defaultValue = "false") revokeGoogle: Boolean
    ): ResponseEntity<Map<String, String>> {
        // Optionally revoke Google access token
        if (revokeGoogle && user?.googleAccessToken != null) {
            try {
                googleOAuth2Service.revokeToken(user.googleAccessToken!!)
            } catch (_: Exception) {
                // Ignore revocation errors
            }
        }

        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}

