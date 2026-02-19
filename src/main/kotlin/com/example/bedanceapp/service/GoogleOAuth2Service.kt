package com.example.bedanceapp.service

import com.google.api.client.auth.oauth2.TokenResponse
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse
import com.google.api.client.http.GenericUrl
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.oauth2.Oauth2
import com.google.api.services.oauth2.model.Userinfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.StringReader
import kotlin.collections.set

@Service
class GoogleOAuth2Service {

    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private lateinit var clientId: String

    @Value("\${spring.security.oauth2.client.registration.google.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${spring.security.oauth2.client.registration.google.redirect-uri}")
    private lateinit var redirectUri: String

    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    // Base scopes for authentication
    private val baseScopes = listOf(
        "openid",
        "https://www.googleapis.com/auth/userinfo.email",
        "https://www.googleapis.com/auth/userinfo.profile"
    )

    // Additional scopes for Google Forms (for incremental authorization)
    val formsScopes = listOf(
        "https://www.googleapis.com/auth/forms.body.readonly",
        "https://www.googleapis.com/auth/forms.responses.readonly"
    )

    private fun getFlow(scopes: List<String>): GoogleAuthorizationCodeFlow {
        val clientSecretsJson = """
            {
                "installed": {
                    "client_id": "$clientId",
                    "client_secret": "$clientSecret"
                }
            }
        """.trimIndent()

        val clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            StringReader(clientSecretsJson)
        )

        return GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            jsonFactory,
            clientSecrets,
            scopes
        ).setAccessType("offline")
            .build()
    }

    fun getAuthorizationUrl(scopes: List<String> = baseScopes, state: String? = null): String {
        val flow = getFlow(scopes)
        val url = flow.newAuthorizationUrl()
            .setRedirectUri(redirectUri.replace("{baseUrl}", "http://localhost:8080"))

        if (state != null) {
            url.state = state
        }
        url.set("include_granted_scopes", "true")  // Always include

        return url.build()
    }

    fun exchangeCodeForTokens(code: String, redirectUri: String): GoogleTokenResponse {
        val flow = getFlow(baseScopes)
        return flow.newTokenRequest(code)
            .setRedirectUri(redirectUri)
            .execute() as GoogleTokenResponse
    }

    fun getUserInfo(accessToken: String): Userinfo {
        val credential = com.google.api.client.googleapis.auth.oauth2.GoogleCredential()
            .setAccessToken(accessToken)

        val oauth2 = Oauth2.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("be-dance-app")
            .build()

        return oauth2.userinfo().get().execute()
    }

    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val clientSecretsJson = """
            {
                "installed": {
                    "client_id": "$clientId",
                    "client_secret": "$clientSecret"
                }
            }
        """.trimIndent()

        val clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            StringReader(clientSecretsJson)
        )

        return com.google.api.client.auth.oauth2.RefreshTokenRequest(
            httpTransport,
            jsonFactory,
            GenericUrl("https://oauth2.googleapis.com/token"),
            refreshToken
        ).setClientAuthentication(
            com.google.api.client.http.BasicAuthentication(
                clientSecrets.details.clientId,
                clientSecrets.details.clientSecret
            )
        ).execute()
    }

    fun getIncrementalAuthUrl(userId: String, additionalScopes: List<String>): String {
        // For incremental authorization, we pass the user hint and additional scopes
        return getAuthorizationUrl(
            scopes = additionalScopes,
            state = "incremental_$userId"
        ) + "&login_hint=$userId"
    }
}

