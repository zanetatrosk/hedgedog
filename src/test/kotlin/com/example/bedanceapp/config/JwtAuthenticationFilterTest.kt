package com.example.bedanceapp.config

import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.JwtService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@DisplayName("JwtAuthenticationFilter Tests")
class JwtAuthenticationFilterTest {

    private val jwtService = mock<JwtService>()
    private val userRepository = mock<UserRepository>()
    private val filter = JwtAuthenticationFilter(jwtService, userRepository)

    @Test
    fun `returns 401 when bearer token is invalid`() {
        val request = MockHttpServletRequest().apply {
            addHeader("Authorization", "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()
        var chainInvoked = false

        whenever(jwtService.validateToken("invalid-token")).thenReturn(false)

        filter.doFilter(request, response) { _, _ -> chainInvoked = true }

        assertEquals(401, response.status)
        assertFalse(chainInvoked)
    }
}


