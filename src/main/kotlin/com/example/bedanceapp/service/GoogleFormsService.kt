package com.example.bedanceapp.service

import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.UserRepository
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.forms.v1.Forms
import com.google.api.services.forms.v1.model.Form
import com.google.api.services.forms.v1.model.ListFormResponsesResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

/**
 * Service for interacting with Google Forms API
 * This requires the user to have granted forms scopes through incremental authorization
 */
@Service
class GoogleFormsService(
    private val userRepository: UserRepository,
    private val googleOAuth2Service: GoogleOAuth2Service
) {
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    /**
     * Check if user has granted Google Forms permissions
     */
    fun hasFormsAccess(user: User): Boolean {
        val scopes = user.googleScopes?.split(" ") ?: emptyList()
        return googleOAuth2Service.formsScopes.any { requiredScope ->
            scopes.contains(requiredScope)
        }
    }

    private fun getFormsService(user: User): Forms {
        val accessToken = if (isTokenExpired(user)) {
            refreshUserToken(user)
        } else {
            user.googleAccessToken!!
        }

        val credential = GoogleCredential().setAccessToken(accessToken)

        return Forms.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("be-dance-app")
            .build()
    }


    fun getForm(user: User, formId: String): Form {
        if (!hasFormsAccess(user)) {
            throw IllegalStateException("User has not granted Google Forms access")
        }

        val formsService = getFormsService(user)
        return formsService.forms().get(formId).execute()
    }

    fun getFormResponses(user: User, formId: String): ListFormResponsesResponse {
        if (!hasFormsAccess(user)) {
            throw IllegalStateException("User has not granted Google Forms access")
        }

        val formsService = getFormsService(user)
        return formsService.forms().responses().list(formId).execute()
    }

    private fun isTokenExpired(user: User): Boolean {
        return user.googleTokenExpiry?.isBefore(OffsetDateTime.now()) ?: true
    }

    private fun refreshUserToken(user: User): String {
        if (user.googleRefreshToken == null) {
            throw IllegalStateException("No refresh token available")
        }

        val tokenResponse = googleOAuth2Service.refreshAccessToken(user.googleRefreshToken!!)

        val updatedUser = user.copy(
            googleAccessToken = tokenResponse.accessToken,
            googleTokenExpiry = OffsetDateTime.now().plusSeconds(tokenResponse.expiresInSeconds)
        )
        userRepository.save(updatedUser)

        return tokenResponse.accessToken
    }
}
