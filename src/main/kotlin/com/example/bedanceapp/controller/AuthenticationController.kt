package com.example.bedanceapp.controller

import com.example.bedanceapp.model.*
import com.example.bedanceapp.service.AuthenticationService
import com.example.bedanceapp.service.GoogleOAuth2Service
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class AuthenticationController(
    private val authenticationService: AuthenticationService,
    private val googleOAuth2Service: GoogleOAuth2Service
) {

    /**
     * Get Google OAuth2 authorization URL for initial sign-in
     * Frontend should redirect user to this URL
     */
    @GetMapping("/google/login")
    fun getGoogleAuthUrl(): ResponseEntity<GoogleAuthUrlResponse> {
        val response = authenticationService.getAuthorizationUrl()
        return ResponseEntity.ok(response)
    }

    /**
     * Handle Google OAuth2 callback - receives code from Google's redirect
     * This is the redirect_uri registered in Google Console
     */
    @GetMapping("/google/callback")
    fun googleCallbackGet(
        @RequestParam code: String,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?
    ): ResponseEntity<Void> {
        if (error != null) {
            // User denied permission
            return ResponseEntity.status(302)
                .header("Location", "http://localhost:3000/auth/callback?error=$error")
                .build()
        }

        return try {
            // Exchange code for tokens using the backend redirect URI
            val response = authenticationService.authenticateWithGoogle(
                code = code,
                redirectUri = "http://localhost:8080/api/auth/google/callback"
            )

            // Redirect to frontend with all the tokens
            val frontendUrl = "http://localhost:3000/auth/callback" +
                    "?accessToken=${response.accessToken}" +
                    "&refreshToken=${response.refreshToken}" +
                    "&expiresIn=${response.expiresIn}"

            ResponseEntity.status(302)
                .header("Location", frontendUrl)
                .build()

        } catch (e: Exception) {
            ResponseEntity.status(302)
                .header("Location", "http://localhost:3000/auth/callback?error=${e.message}")
                .build()
        }
    }


    /**
     * Refresh access token using refresh token
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
     * Get authorization URL for incremental authorization (e.g., for Google Forms access)
     * This will request additional scopes while preserving existing ones
     */
    @GetMapping("/google/incremental-auth-url")
    fun getIncrementalAuthUrl(
        @AuthenticationPrincipal user: User,
        @RequestParam scopes: String
    ): ResponseEntity<GoogleAuthUrlResponse> {
        val scopeList = when (scopes) {
            "forms" -> googleOAuth2Service.formsScopes
            else -> throw IllegalArgumentException("Unknown scope group: $scopes")
        }

        val response = authenticationService.getIncrementalAuthUrl(user.id!!, scopeList)
        return ResponseEntity.ok(response)
    }

    /**
     * Handle incremental authorization callback
     * Updates user's granted scopes with additional permissions
     */
    @PostMapping("/google/incremental-callback")
    fun handleIncrementalAuth(
        @AuthenticationPrincipal user: User,
        @RequestBody request: AuthenticationRequest
    ): ResponseEntity<Map<String, String>> {
        authenticationService.updateUserScopes(user.id!!, request.code, request.redirectUri)
        return ResponseEntity.ok(mapOf("message" to "Scopes updated successfully"))
    }

    /**
     * Logout endpoint (client should discard tokens)
     */
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "Logged out successfully"))
    }
}

