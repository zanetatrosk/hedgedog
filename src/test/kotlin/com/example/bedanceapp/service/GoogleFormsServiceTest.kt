package com.example.bedanceapp.service

import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("GoogleFormsService Tests")
class GoogleFormsServiceTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var googleOAuth2Service: GoogleOAuth2Service

    private lateinit var service: GoogleFormsService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = GoogleFormsService(userRepository, googleOAuth2Service)
        whenever(googleOAuth2Service.formsScopes).thenReturn(
            listOf(
                GoogleOAuth2Service.SCOPE_FORMS_BODY,
                GoogleOAuth2Service.SCOPE_FORMS_RESPONSES
            )
        )
    }

    @Test
    fun `hasFormsAccess returns true when required scope exists`() {
        val user = User(
            email = "user@example.com",
            provider = "google",
            providerId = "google-1",
            googleScopes = "openid email https://www.googleapis.com/auth/forms.body.readonly"
        )

        assertTrue(service.hasFormsAccess(user))
    }

    @Test
    fun `hasFormsAccess returns false when user has no forms scopes`() {
        val user = User(
            email = "user@example.com",
            provider = "google",
            providerId = "google-1",
            googleScopes = "openid email profile"
        )

        assertFalse(service.hasFormsAccess(user))
    }

    @Test
    fun `getForm throws when forms access is missing`() {
        val user = User(
            email = "user@example.com",
            provider = "google",
            providerId = "google-1",
            googleScopes = "openid email profile"
        )

        val ex = assertThrows<IllegalStateException> {
            service.getForm(user, "form-id")
        }

        assertEquals("User has not granted Google Forms access", ex.message)
    }

    @Test
    fun `getFormResponses throws when forms access is missing`() {
        val user = User(
            email = "user@example.com",
            provider = "google",
            providerId = "google-1",
            googleScopes = "openid email profile"
        )

        val ex = assertThrows<IllegalStateException> {
            service.getFormResponses(user, "form-id")
        }

        assertEquals("User has not granted Google Forms access", ex.message)
    }
}


