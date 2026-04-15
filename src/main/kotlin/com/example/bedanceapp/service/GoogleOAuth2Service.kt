package com.example.bedanceapp.service

import com.google.api.client.auth.oauth2.RefreshTokenRequest
import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.BasicAuthentication
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Google OAuth2 Service using the Authorization Code Model
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

    fun verifyIdToken(idTokenString: String): GoogleIdToken {
        requireGoogleCredentials()
        val verifier = GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
            .setAudience(listOf(clientId))
            .build()

        val idToken = verifier.verify(idTokenString)
            ?: throw IllegalArgumentException("Invalid ID token")

        return idToken
    }

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

    fun refreshAccessToken(refreshToken: String): TokenResponse {
        requireGoogleCredentials()
        return RefreshTokenRequest(
            httpTransport,
            jsonFactory,
            GenericUrl("https://oauth2.googleapis.com/token"),
            refreshToken
        ).setClientAuthentication(
            BasicAuthentication(clientId, clientSecret)
        ).execute()
    }

    private fun requireGoogleCredentials() {
        require(clientId.isNotBlank()) {
            "Google OAuth client ID is not configured"
        }
        require(clientSecret.isNotBlank()) {
            "Google OAuth client secret is not configured"
        }
    }
}

data class UserInfoFromToken(
    val id: String,
    val email: String,
    val emailVerified: Boolean,
    val name: String?,
    val givenName: String?,
    val familyName: String?,
    val picture: String?
)

