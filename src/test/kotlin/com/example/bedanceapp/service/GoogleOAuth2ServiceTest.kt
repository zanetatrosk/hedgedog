package com.example.bedanceapp.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DisplayName("GoogleOAuth2Service Tests")
class GoogleOAuth2ServiceTest {

    private lateinit var service: GoogleOAuth2Service

    @Mock private lateinit var idToken: GoogleIdToken
    @Mock private lateinit var payload: GoogleIdToken.Payload

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = GoogleOAuth2Service()
    }

    @Test
    fun `scope collections contain expected values`() {
        assertTrue(service.baseScopes.contains(GoogleOAuth2Service.SCOPE_OPENID))
        assertTrue(service.baseScopes.contains(GoogleOAuth2Service.SCOPE_EMAIL))
        assertTrue(service.baseScopes.contains(GoogleOAuth2Service.SCOPE_PROFILE))
        assertTrue(service.formsScopes.contains(GoogleOAuth2Service.SCOPE_FORMS_BODY))
        assertTrue(service.formsScopes.contains(GoogleOAuth2Service.SCOPE_FORMS_RESPONSES))
    }

    @Test
    fun `getUserInfoFromIdToken maps payload fields`() {
        whenever(idToken.payload).thenReturn(payload)
        whenever(payload.subject).thenReturn("google-123")
        whenever(payload.email).thenReturn("user@example.com")
        whenever(payload.emailVerified).thenReturn(true)
        whenever(payload.get("name")).thenReturn("Test User")
        whenever(payload.get("given_name")).thenReturn("Test")
        whenever(payload.get("family_name")).thenReturn("User")
        whenever(payload.get("picture")).thenReturn("https://example.com/p.png")

        val info = service.getUserInfoFromIdToken(idToken)

        assertEquals("google-123", info.id)
        assertEquals("user@example.com", info.email)
        assertEquals(true, info.emailVerified)
        assertEquals("Test User", info.name)
        assertEquals("Test", info.givenName)
        assertEquals("User", info.familyName)
        assertEquals("https://example.com/p.png", info.picture)
    }
}

