package com.example.bedanceapp.service

import com.example.bedanceapp.model.*
import com.example.bedanceapp.repository.UserProfileRepository
import com.example.bedanceapp.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
class AuthenticationService(
    private val googleOAuth2Service: GoogleOAuth2Service,
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val profileRepository: UserProfileRepository
) {

    /**
     * Authenticate user with Google using Authorization Code Model
     * Based on: https://developers.google.com/identity/oauth2/web/guides/use-code-model
     *
     * @param code Authorization code received from Google Identity Services on frontend
     * @param redirectUri The redirect URI used in the authorization request
     * @return AuthenticationResponse with JWT tokens and user info
     */
    @Transactional
    fun authenticateWithGoogle(code: String, redirectUri: String): AuthenticationResponse {
        // Exchange authorization code for tokens (including ID token)
        // Use the redirect_uri provided by frontend (typically "postmessage" for Authorization Code Model)
        val tokenResponse = googleOAuth2Service.exchangeCodeForTokens(code, redirectUri)

        // Verify and extract user info from ID token (no additional API call needed)
        val idToken = googleOAuth2Service.verifyIdToken(tokenResponse.get("id_token") as String)
        val userInfo = googleOAuth2Service.getUserInfoFromIdToken(idToken)

        // Find or create user
        var user = userRepository.findByProviderAndProviderId("google", userInfo.id)

        if (user == null) {
            // Create new user
            user = User(
                email = userInfo.email,
                provider = "google",
                providerId = userInfo.id,
                googleAccessToken = tokenResponse.accessToken,
                googleRefreshToken = tokenResponse.refreshToken,
                googleTokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds),
                googleScopes = tokenResponse.scope,
                lastLoginAt = OffsetDateTime.now(),
            )
            user = userRepository.saveAndFlush(user)

            // Create profile with info from ID token
            val profile = UserProfile(
                user = user,
                firstName = userInfo.givenName,
                lastName = userInfo.familyName
            )
            profileRepository.saveAndFlush(profile)

        } else {
            // Update existing user's tokens
            val updatedUser = user.copy(
                googleAccessToken = tokenResponse.accessToken,
                googleRefreshToken = tokenResponse.refreshToken ?: user.googleRefreshToken,
                googleTokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds),
                googleScopes = tokenResponse.scope,
                lastLoginAt = OffsetDateTime.now()
            )
            user = userRepository.save(updatedUser)
        }

        // Generate JWT tokens for our application
        val accessToken = jwtService.generateAccessToken(user.id!!, user.email)
        val refreshToken = jwtService.generateRefreshToken(user.id!!, user.email)

        return AuthenticationResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.getExpirationTime(),
            user = UserDto(
                id = user.id!!,
                email = user.email,
                provider = user.provider,
                hasProfile = user.profile != null,
                grantedScopes = parseScopes(user.googleScopes)
            )
        )
    }

    @Transactional
    fun refreshToken(refreshTokenRequest: RefreshTokenRequest): AuthenticationResponse {
        val refreshToken = refreshTokenRequest.refreshToken

        if (!jwtService.validateToken(refreshToken)) {
            throw IllegalArgumentException("Invalid refresh token")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Generate new tokens
        val newAccessToken = jwtService.generateAccessToken(user.id!!, user.email)
        val newRefreshToken = jwtService.generateRefreshToken(user.id!!, user.email)

        return AuthenticationResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtService.getExpirationTime(),
            user = UserDto(
                id = user.id!!,
                email = user.email,
                provider = user.provider,
                hasProfile = user.profile != null,
                grantedScopes = parseScopes(user.googleScopes)
            )
        )
    }

    /**
     * Update user's scopes after incremental authorization
     * Uses the code model for incremental authorization
     */
    @Transactional
    fun updateUserScopes(userId: UUID, code: String, redirectUri: String) {
        val tokenResponse = googleOAuth2Service.exchangeCodeForTokens(code, redirectUri)

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Update user with new tokens and scopes
        val updatedUser = user.copy(
            googleAccessToken = tokenResponse.accessToken,
            googleRefreshToken = tokenResponse.refreshToken ?: user.googleRefreshToken,
            googleTokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds),
            googleScopes = mergeScopes(user.googleScopes, tokenResponse.scope)
        )

        userRepository.save(updatedUser)
    }

    private fun parseScopes(scopes: String?): List<String> {
        return scopes?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    }

    private fun mergeScopes(existingScopes: String?, newScopes: String?): String {
        val existing = parseScopes(existingScopes).toMutableSet()
        val new = parseScopes(newScopes)
        existing.addAll(new)
        return existing.joinToString(" ")
    }
}

