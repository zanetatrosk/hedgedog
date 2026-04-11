package com.example.bedanceapp.service

import com.example.bedanceapp.model.DanceStyle
import com.example.bedanceapp.repository.DanceStyleRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("DanceStyleService Tests")
class DanceStyleServiceTest {

    @Mock private lateinit var danceStyleRepository: DanceStyleRepository

    private lateinit var service: DanceStyleService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = DanceStyleService(danceStyleRepository)
    }

    @Test
    fun `findAll returns repository data`() {
        val list = listOf(DanceStyle(id = UUID.randomUUID(), name = "Bachata"))
        whenever(danceStyleRepository.findAll()).thenReturn(list)

        assertEquals(list, service.findAll())
    }

    @Test
    fun `findById throws when missing`() {
        val id = UUID.randomUUID()
        whenever(danceStyleRepository.findById(id)).thenReturn(Optional.empty())

        val ex = assertThrows<NoSuchElementException> { service.findById(id) }
        assertEquals("DanceStyle not found with id: $id", ex.message)
    }
}

