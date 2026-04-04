package com.example.bedanceapp.service

import com.example.bedanceapp.model.*
import com.example.bedanceapp.repository.UserProfileRepository
import com.example.bedanceapp.repository.UserRepository
import com.google.api.client.auth.oauth2.TokenResponse
import com.nimbusds.openid.connect.sdk.claims.UserInfo
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
     * Token processing for Google OAuth2 authorization_code flow.
     *
     * @param request TokenRequest with grant_type and appropriate parameters
     * @param currentUser Current authenticated user (required for incremental auth, optional for others)
     * @return AuthenticationResponse with JWT access token and user info
     */
    @Transactional
    fun processToken(request: TokenRequest, currentUser: User? = null): AuthenticationResponse {
        return when (request.grantType) {
            "authorization_code" -> {
                require(request.code != null) { "code is required for authorization_code grant type" }
                require(request.redirectUri != null) { "redirectUri is required for authorization_code grant type" }

                handleAuthorizationCode(request.code, request.redirectUri, currentUser)
            }
            else -> throw IllegalArgumentException("Invalid grant_type. Must be 'authorization_code'")
        }
    }

    private fun loginNewUser(userInfo: UserInfoFromToken, tokenResponse: TokenResponse): User {
        val user = User(
            email = userInfo.email,
            provider = "google",
            providerId = userInfo.id,
            googleAccessToken = tokenResponse.accessToken,
            googleRefreshToken = tokenResponse.refreshToken,
            googleTokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds),
            googleScopes = tokenResponse.scope,
            lastLoginAt = OffsetDateTime.now(),
        )
        val savedUser = userRepository.saveAndFlush(user)

        // Create profile with info from ID token
        val profile = UserProfile(
            user = savedUser,
            firstName = userInfo.givenName,
            lastName = userInfo.familyName
        )
        profileRepository.saveAndFlush(profile)

        return savedUser
    }

    private fun loginExistingUser(user: User, tokenResponse: TokenResponse): User {
        val updatedUser = user.copy(
            googleAccessToken = tokenResponse.accessToken,
            googleRefreshToken = tokenResponse.refreshToken ?: user.googleRefreshToken,
            googleTokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds),
            googleScopes = tokenResponse.scope,
            lastLoginAt = OffsetDateTime.now()
        )
        return userRepository.save(updatedUser)
    }

    private fun handleAuthorizationCode(code: String, redirectUri: String, currentUser: User?): AuthenticationResponse {
        // Exchange authorization code for tokens (including ID token)
        val tokenResponse = googleOAuth2Service.exchangeCodeForTokens(code, redirectUri)

        // Verify and extract user info from ID token
        val idToken = googleOAuth2Service.verifyIdToken(tokenResponse.get("id_token") as String)
        val userInfo = googleOAuth2Service.getUserInfoFromIdToken(idToken)

        // Find or create user
        val existingUser = userRepository.findByProviderAndProviderId("google", userInfo.id)

        val user = if (existingUser != null) {
            loginExistingUser(existingUser, tokenResponse)
        } else {
            loginNewUser(userInfo, tokenResponse)
        }

        // Generate JWT access token for our application
        val accessToken = jwtService.generateAccessToken(user.id!!, user.email)

        return AuthenticationResponse(
            accessToken = accessToken,
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

    private fun parseScopes(scopes: String?): List<String> {
        return scopes?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
    }

}

