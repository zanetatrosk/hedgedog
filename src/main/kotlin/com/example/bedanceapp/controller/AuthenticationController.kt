package com.example.bedanceapp.controller

import com.example.bedanceapp.model.*
import com.example.bedanceapp.service.AuthenticationService
import com.example.bedanceapp.service.GoogleOAuth2Service
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

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
     * Exchange Google authorization code for JWT tokens
     * Frontend should send the authorization code obtained from Google Identity Services
     *
     * @param request Contains the authorization code from Google and redirectUri used
     * @return JWT tokens and user information
     */
    @PostMapping("/google/login")
    fun authenticateWithGoogle(@RequestBody request: AuthenticationRequest): ResponseEntity<AuthenticationResponse> {
        val response = authenticationService.authenticateWithGoogle(request.code, request.redirectUri)
        return ResponseEntity.ok(response)
    }

    /**
     * Refresh JWT access token using refresh token
     */
    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequest): ResponseEntity<AuthenticationResponse> {
        val response = authenticationService.refreshToken(request)
        return ResponseEntity.ok(response)
    }

    /**
     * Get current authenticated user info
     */
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal user: User): ResponseEntity<UserDto> {
        val userDto = UserDto(
            id = user.id!!,
            email = user.email,
            provider = user.provider,
            hasProfile = user.profile != null,
            grantedScopes = user.googleScopes?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        )
        return ResponseEntity.ok(userDto)
    }

    /**
     * Get available scope groups for incremental authorization
     * Frontend can use this to know which scopes to request
     */
    @GetMapping("/google/available-scopes")
    fun getAvailableScopes(): ResponseEntity<Map<String, List<String>>> {
        return ResponseEntity.ok(mapOf(
            "base" to googleOAuth2Service.baseScopes,
            "forms" to googleOAuth2Service.formsScopes
        ))
    }

    /**
     * Handle incremental authorization
     * Updates user's granted scopes with additional permissions
     *
     * @param request Contains the authorization code from incremental auth flow and redirectUri
     */
    @PostMapping("/google/incremental-auth")
    fun handleIncrementalAuth(
        @AuthenticationPrincipal user: User,
        @RequestBody request: AuthenticationRequest
    ): ResponseEntity<Map<String, String>> {
        authenticationService.updateUserScopes(user.id!!, request.code, request.redirectUri)
        return ResponseEntity.ok(mapOf("message" to "Scopes updated successfully"))
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
            } catch (e: Exception) {
                // Ignore revocation errors
            }
        }

        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}

