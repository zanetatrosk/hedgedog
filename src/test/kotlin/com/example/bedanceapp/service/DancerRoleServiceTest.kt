package com.example.bedanceapp.service

import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.repository.DancerRoleRepository
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

@DisplayName("DancerRoleService Tests")
class DancerRoleServiceTest {

    @Mock private lateinit var dancerRoleRepository: DancerRoleRepository

    private lateinit var service: DancerRoleService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = DancerRoleService(dancerRoleRepository)
    }

    @Test
    fun `findAll returns repository data`() {
        val list = listOf(DancerRole(id = UUID.randomUUID(), name = "Leader"))
        whenever(dancerRoleRepository.findAll()).thenReturn(list)

        assertEquals(list, service.findAll())
    }

    @Test
    fun `findById throws when missing`() {
        val id = UUID.randomUUID()
        whenever(dancerRoleRepository.findById(id)).thenReturn(Optional.empty())

        val ex = assertThrows<NoSuchElementException> { service.findById(id) }
        assertEquals("DancerRole not found with id: $id", ex.message)
    }
}

