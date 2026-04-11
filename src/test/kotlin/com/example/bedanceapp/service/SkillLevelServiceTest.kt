package com.example.bedanceapp.service

import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.repository.SkillLevelRepository
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

@DisplayName("SkillLevelService Tests")
class SkillLevelServiceTest {

    @Mock private lateinit var skillLevelRepository: SkillLevelRepository

    private lateinit var service: SkillLevelService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = SkillLevelService(skillLevelRepository)
    }

    @Test
    fun `findAll returns repository data`() {
        val list = listOf(SkillLevel(id = UUID.randomUUID(), name = "Beginner", levelOrder = 1))
        whenever(skillLevelRepository.findAll()).thenReturn(list)

        assertEquals(list, service.findAll())
    }

    @Test
    fun `findById throws when missing`() {
        val id = UUID.randomUUID()
        whenever(skillLevelRepository.findById(id)).thenReturn(Optional.empty())

        val ex = assertThrows<NoSuchElementException> { service.findById(id) }
        assertEquals("SkillLevel not found with id: $id", ex.message)
    }
}

