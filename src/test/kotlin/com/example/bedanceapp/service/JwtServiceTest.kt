package com.example.bedanceapp.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private lateinit var service: JwtService

    @BeforeEach
    fun setUp() {
        service = JwtService()
        setField("secret", "01234567890123456789012345678901")
        setField("expiration", 60_000L)
    }

    @Test
    fun `generateAccessToken creates valid token with claims`() {
        val userId = UUID.randomUUID()
        val email = "user@example.com"

        val token = service.generateAccessToken(userId, email)

        assertTrue(service.validateToken(token))
        assertEquals(userId, service.getUserIdFromToken(token))
        assertEquals(email, service.getEmailFromToken(token))
        assertEquals(60_000L, service.getExpirationTime())
    }

    @Test
    fun `validateToken returns false for malformed token`() {
        assertFalse(service.validateToken("not-a-jwt"))
    }

    private fun setField(name: String, value: Any) {
        val field = JwtService::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(service, value)
    }
}

