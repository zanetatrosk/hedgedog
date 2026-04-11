package com.example.bedanceapp.service

import com.example.bedanceapp.model.EventType
import com.example.bedanceapp.repository.EventTypeRepository
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

@DisplayName("EventTypeService Tests")
class EventTypeServiceTest {

    @Mock private lateinit var eventTypeRepository: EventTypeRepository

    private lateinit var service: EventTypeService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = EventTypeService(eventTypeRepository)
    }

    @Test
    fun `findAll returns repository data`() {
        val list = listOf(EventType(id = UUID.randomUUID(), name = "Workshop"))
        whenever(eventTypeRepository.findAll()).thenReturn(list)

        assertEquals(list, service.findAll())
    }

    @Test
    fun `findById throws when missing`() {
        val id = UUID.randomUUID()
        whenever(eventTypeRepository.findById(id)).thenReturn(Optional.empty())

        val ex = assertThrows<NoSuchElementException> { service.findById(id) }
        assertEquals("EventType not found with id: $id", ex.message)
    }
}

