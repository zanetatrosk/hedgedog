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
     * Unified token processing that handles:
     * - authorization_code: Initial login or incremental authorization
     * - refresh_token: Refresh JWT tokens
     *
     * @param request TokenRequest with grant_type and appropriate parameters
     * @param currentUser Current authenticated user (required for incremental auth, optional for others)
     * @return AuthenticationResponse with JWT tokens and user info
     */
    @Transactional
    fun processToken(request: TokenRequest, currentUser: User? = null): AuthenticationResponse {
        return when (request.grantType) {
            "authorization_code" -> {
                require(request.code != null) { "code is required for authorization_code grant type" }
                require(request.redirectUri != null) { "redirectUri is required for authorization_code grant type" }

                handleAuthorizationCode(request.code, request.redirectUri, currentUser)
            }
            "refresh_token" -> {
                require(request.refreshToken != null) { "refreshToken is required for refresh_token grant type" }

                handleRefreshToken(request.refreshToken)
            }
            else -> throw IllegalArgumentException("Invalid grant_type. Must be 'authorization_code' or 'refresh_token'")
        }
    }

    /**
     * Handle authorization_code grant type
     * Supports both initial login and incremental authorization
     */
    private fun handleAuthorizationCode(code: String, redirectUri: String, currentUser: User?): AuthenticationResponse {
        // Exchange authorization code for tokens (including ID token)
        val tokenResponse = googleOAuth2Service.exchangeCodeForTokens(code, redirectUri)

        // Verify and extract user info from ID token
        val idToken = googleOAuth2Service.verifyIdToken(tokenResponse.get("id_token") as String)
        val userInfo = googleOAuth2Service.getUserInfoFromIdToken(idToken)

        // Find or create user
        var user = userRepository.findByProviderAndProviderId("google", userInfo.id)

        if (user == null) {
            // Create new user (initial login)
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
            // Update existing user's tokens and scopes
            val isIncrementalAuth = currentUser != null && currentUser.id == user.id
            val updatedScopes = if (isIncrementalAuth) {
                // Merge scopes for incremental authorization
                mergeScopes(user.googleScopes, tokenResponse.scope)
            } else {
                // Replace scopes for regular login
                tokenResponse.scope
            }

            val updatedUser = user.copy(
                googleAccessToken = tokenResponse.accessToken,
                googleRefreshToken = tokenResponse.refreshToken ?: user.googleRefreshToken,
                googleTokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds),
                googleScopes = updatedScopes,
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

    /**
     * Handle refresh_token grant type
     */
    private fun handleRefreshToken(refreshToken: String): AuthenticationResponse {
        if (!jwtService.validateToken(refreshToken)) {
            throw IllegalArgumentException("Invalid refresh token")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Generate new JWT tokens
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

    // Legacy methods for backward compatibility

    /**
     * @deprecated Use processToken with TokenRequest instead
     */
    @Deprecated("Use processToken instead")
    @Transactional
    fun authenticateWithGoogle(code: String, redirectUri: String): AuthenticationResponse {
        return processToken(TokenRequest(grantType = "authorization_code", code = code, redirectUri = redirectUri))
    }

    /**
     * @deprecated Use processToken with TokenRequest instead
     */
    @Deprecated("Use processToken instead")
    @Transactional
    fun refreshToken(refreshTokenRequest: RefreshTokenRequest): AuthenticationResponse {
        return processToken(TokenRequest(grantType = "refresh_token", refreshToken = refreshTokenRequest.refreshToken))
    }

    /**
     * @deprecated Use processToken with TokenRequest instead
     */
    @Deprecated("Use processToken instead")
    @Transactional
    fun updateUserScopes(userId: UUID, code: String, redirectUri: String) {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        processToken(TokenRequest(grantType = "authorization_code", code = code, redirectUri = redirectUri), user)
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

