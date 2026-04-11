package com.example.bedanceapp.service.registration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("JsonExtensions Tests")
class JsonExtensionsTest {

    private val objectMapper = ObjectMapper()

    data class TestPayload(
        val name: String,
        val count: Int
    )

    @Test
    fun `toObject returns null for null or blank input`() {
        val nullResult = (null as String?).toObject(objectMapper, TestPayload::class.java)
        val blankResult = "   ".toObject(objectMapper, TestPayload::class.java)

        assertNull(nullResult)
        assertNull(blankResult)
    }

    @Test
    fun `toObject parses valid json`() {
        val json = "{\"name\":\"abc\",\"count\":3}"

        val parsed = json.toObject(objectMapper, TestPayload::class.java)

        assertEquals(TestPayload(name = "abc", count = 3), parsed)
    }

    @Test
    fun `toObject returns null for invalid json`() {
        val parsed = "{invalid}".toObject(objectMapper, TestPayload::class.java)

        assertNull(parsed)
    }
}

