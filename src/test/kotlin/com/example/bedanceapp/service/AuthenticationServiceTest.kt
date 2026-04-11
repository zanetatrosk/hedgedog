package com.example.bedanceapp.service

import com.example.bedanceapp.model.AuthResponse
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.model.dto.TokenRequest
import com.example.bedanceapp.repository.UserProfileRepository
import com.example.bedanceapp.repository.UserRepository
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock private lateinit var googleOAuth2Service: GoogleOAuth2Service
    @Mock private lateinit var jwtService: JwtService
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var profileRepository: UserProfileRepository

    private lateinit var service: AuthenticationService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = AuthenticationService(googleOAuth2Service, jwtService, userRepository, profileRepository)
    }

    @Test
    fun `processToken throws for unsupported grant type`() {
        val ex = assertThrows<IllegalArgumentException> {
            service.processToken(TokenRequest(grantType = "refresh_token"))
        }

        assertEquals("Invalid grant_type. Must be 'authorization_code'", ex.message)
    }

    @Test
    fun `processToken requires code for authorization_code`() {
        val ex = assertThrows<IllegalArgumentException> {
            service.processToken(TokenRequest(grantType = "authorization_code", code = null, redirectUri = "postmessage"))
        }

        assertEquals("code is required for authorization_code grant type", ex.message)
    }

    @Test
    fun `processToken logs in existing user and returns auth response`() {
        val tokenResponse = TokenResponse()
            .setAccessToken("google-access")
            .setRefreshToken("google-refresh")
            .setExpiresInSeconds(3600)
            .setScope("openid email profile")
            .set("id_token", "id-token")

        val googleIdToken = org.mockito.kotlin.mock<GoogleIdToken>()
        val userInfo = UserInfoFromToken(
            id = "google-123",
            email = "user@example.com",
            emailVerified = true,
            name = "User Name",
            givenName = "User",
            familyName = "Name",
            picture = null
        )
        val existing = User(id = UUID.randomUUID(), email = "user@example.com", provider = "google", providerId = "google-123")
        val updated = existing.copy(googleScopes = "openid email profile")

        whenever(googleOAuth2Service.exchangeCodeForTokens("code-1", "postmessage")).thenReturn(tokenResponse)
        whenever(googleOAuth2Service.verifyIdToken("id-token")).thenReturn(googleIdToken)
        whenever(googleOAuth2Service.getUserInfoFromIdToken(googleIdToken)).thenReturn(userInfo)
        whenever(userRepository.findByProviderAndProviderId("google", "google-123")).thenReturn(existing)
        whenever(userRepository.save(any())).thenReturn(updated)
        whenever(jwtService.generateAccessToken(updated.id!!, updated.email)).thenReturn("app-jwt")
        whenever(jwtService.getExpirationTime()).thenReturn(900_000L)

        val response: AuthResponse = service.processToken(
            TokenRequest(grantType = "authorization_code", code = "code-1", redirectUri = "postmessage")
        )

        assertEquals("app-jwt", response.accessToken)
        assertEquals(900_000L, response.expiresIn)
        assertEquals(updated.id, response.user.id)
        assertEquals(listOf("openid", "email", "profile"), response.user.grantedScopes)
    }

    @Test
    fun `processToken creates new user when provider user is missing`() {
        val tokenResponse = TokenResponse()
            .setAccessToken("google-access")
            .setRefreshToken("google-refresh")
            .setExpiresInSeconds(3600)
            .setScope("openid email")
            .set("id_token", "id-token")
        val googleIdToken = org.mockito.kotlin.mock<GoogleIdToken>()
        val userInfo = UserInfoFromToken("google-123", "new@example.com", true, "New User", "New", "User", null)
        val savedUser = User(
            id = UUID.randomUUID(),
            email = "new@example.com",
            provider = "google",
            providerId = "google-123",
            googleScopes = "openid email"
        )

        whenever(googleOAuth2Service.exchangeCodeForTokens("code-2", "postmessage")).thenReturn(tokenResponse)
        whenever(googleOAuth2Service.verifyIdToken("id-token")).thenReturn(googleIdToken)
        whenever(googleOAuth2Service.getUserInfoFromIdToken(googleIdToken)).thenReturn(userInfo)
        whenever(userRepository.findByProviderAndProviderId("google", "google-123")).thenReturn(null)
        whenever(userRepository.saveAndFlush(any())).thenReturn(savedUser)
        whenever(profileRepository.saveAndFlush(any<UserProfile>())).thenAnswer { it.arguments[0] }
        whenever(jwtService.generateAccessToken(savedUser.id!!, savedUser.email)).thenReturn("jwt")
        whenever(jwtService.getExpirationTime()).thenReturn(1_000L)

        val response = service.processToken(
            TokenRequest(grantType = "authorization_code", code = "code-2", redirectUri = "postmessage")
        )

        assertEquals(savedUser.id, response.user.id)
        assertEquals(listOf("openid", "email"), response.user.grantedScopes)
    }
}

