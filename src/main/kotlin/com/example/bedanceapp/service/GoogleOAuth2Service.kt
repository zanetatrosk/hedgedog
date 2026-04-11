package com.example.bedanceapp.service

import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Google OAuth2 Service using the Authorization Code Model
 * Based on: https://developers.google.com/identity/oauth2/web/guides/use-code-model
 *
 * This service handles:
 * - Exchanging authorization codes for tokens
 * - Verifying ID tokens
 * - Refreshing access tokens
 * - Managing OAuth2 scopes
 */
@Service
class GoogleOAuth2Service {

    @Value("\${spring.security.oauth2.client.registration.google.client-id:}")
    private lateinit var clientId: String

    @Value("\${spring.security.oauth2.client.registration.google.client-secret:}")
    private lateinit var clientSecret: String


    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    // Scopes configuration
    companion object {
        const val SCOPE_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
        const val SCOPE_PROFILE = "https://www.googleapis.com/auth/userinfo.profile"
        const val SCOPE_OPENID = "openid"
        const val SCOPE_FORMS_BODY = "https://www.googleapis.com/auth/forms.body.readonly"
        const val SCOPE_FORMS_RESPONSES = "https://www.googleapis.com/auth/forms.responses.readonly"
    }

    val baseScopes = listOf(SCOPE_OPENID, SCOPE_EMAIL, SCOPE_PROFILE)
    val formsScopes = listOf(SCOPE_FORMS_BODY, SCOPE_FORMS_RESPONSES)

    /**
     * Exchange authorization code for access token and refresh token
     * Uses the code model: https://developers.google.com/identity/oauth2/web/guides/use-code-model
     *
     * @param code Authorization code received from Google Identity Services
     * @param redirectUri The redirect URI to use (use "postmessage" for code model)
     * @return TokenResponse containing access_token, refresh_token, id_token, etc.
     */
    fun exchangeCodeForTokens(code: String, redirectUri: String): TokenResponse {
        requireGoogleCredentials()
        return GoogleAuthorizationCodeTokenRequest(
            httpTransport,
            jsonFactory,
            "https://oauth2.googleapis.com/token",
            clientId,
            clientSecret,
            code,
            redirectUri
        ).execute()
    }

    /**
     * Verify and decode Google ID token
     *
     * @param idTokenString The ID token string from Google
     * @return GoogleIdToken containing user information
     */
    fun verifyIdToken(idTokenString: String): GoogleIdToken {
        requireGoogleCredentials()
        val verifier = GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
            .setAudience(listOf(clientId))
            .build()

        val idToken = verifier.verify(idTokenString)
            ?: throw IllegalArgumentException("Invalid ID token")

        return idToken
    }

    /**
     * Extract user information from ID token payload
     */
    fun getUserInfoFromIdToken(idToken: GoogleIdToken): UserInfoFromToken {
        val payload = idToken.payload
        return UserInfoFromToken(
            id = payload.subject,
            email = payload.email,
            emailVerified = payload.emailVerified,
            name = payload["name"] as? String,
            givenName = payload["given_name"] as? String,
            familyName = payload["family_name"] as? String,
            picture = payload["picture"] as? String
        )
    }

    /**
     * Refresh access token using refresh token
     *
     * @param refreshToken The refresh token
     * @return TokenResponse with new access token
     */
    fun refreshAccessToken(refreshToken: String): TokenResponse {
        requireGoogleCredentials()
        return com.google.api.client.auth.oauth2.RefreshTokenRequest(
            httpTransport,
            jsonFactory,
            com.google.api.client.http.GenericUrl("https://oauth2.googleapis.com/token"),
            refreshToken
        ).setClientAuthentication(
            com.google.api.client.http.BasicAuthentication(clientId, clientSecret)
        ).execute()
    }

    private fun requireGoogleCredentials() {
        require(clientId.isNotBlank() && clientId != "replace-with-google-client-id") {
            "Google OAuth client ID is not configured"
        }
        require(clientSecret.isNotBlank() && clientSecret != "replace-with-google-client-secret") {
            "Google OAuth client secret is not configured"
        }
    }
}

/**
 * User information extracted from ID token
 */
data class UserInfoFromToken(
    val id: String,
    val email: String,
    val emailVerified: Boolean,
    val name: String?,
    val givenName: String?,
    val familyName: String?,
    val picture: String?
)

