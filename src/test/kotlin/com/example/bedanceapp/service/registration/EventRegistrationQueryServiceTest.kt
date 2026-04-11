package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.service.mapping.EventRegistrationMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("EventRegistrationQueryService Tests")
class EventRegistrationQueryServiceTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var eventRegistrationMapper: EventRegistrationMapper
    @Mock private lateinit var dancerRoleRepository: DancerRoleRepository

    private lateinit var service: EventRegistrationQueryService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = EventRegistrationQueryService(
            eventRegistrationRepository,
            eventRegistrationMapper,
            dancerRoleRepository
        )
    }

    @Test
    fun `getRegistrationCountByEventId returns 0 when event id is null`() {
        val count = service.getRegistrationCountByEventId(null, RegistrationStatus.REGISTERED)

        assertEquals(0, count)
        verify(eventRegistrationRepository, never()).findByEventIdAndStatus(any(), any())
    }

    @Test
    fun `getRegistrationCountByEventId returns count from repository`() {
        val eventId = UUID.randomUUID()
        whenever(eventRegistrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED))
            .thenReturn(listOf(createRegistration(), createRegistration()))

        val count = service.getRegistrationCountByEventId(eventId, RegistrationStatus.REGISTERED)

        assertEquals(2, count)
    }

    @Test
    fun `getLastRegistrationByEventIdAndUserId returns null when user id is null`() {
        val result = service.getLastRegistrationByEventIdAndUserId(UUID.randomUUID(), null)

        assertNull(result)
        verify(eventRegistrationRepository, never()).findByEventIdAndUserId(any(), any())
    }

    @Test
    fun `getLastRegistrationByEventIdAndUserId returns last registration`() {
        val eventId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val first = createRegistration()
        val second = createRegistration()
        whenever(eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(listOf(first, second))

        val result = service.getLastRegistrationByEventIdAndUserId(eventId, userId)

        assertEquals(second, result)
    }

    @Test
    fun `getRegistrationRolesCountsByEventId counts leader and follower`() {
        val eventId = UUID.randomUUID()
        whenever(eventRegistrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED)).thenReturn(
            listOf(
                createRegistration("Leader"),
                createRegistration("Leader"),
                createRegistration("Follower"),
                createRegistration(null)
            )
        )

        val stats = service.getRegistrationRolesCountsByEventId(eventId)

        assertEquals(4, stats.total)
        assertEquals(2, stats.leaders)
        assertEquals(1, stats.followers)
    }

    @Test
    fun `getAllApprovedRegistrations maps repository result to dto list`() {
        val eventId = UUID.randomUUID()
        val registrations = listOf(createRegistration(), createRegistration())
        val dtos = listOf(
            EventRegistrationDto(registrationId = "1", user = null),
            EventRegistrationDto(registrationId = "2", user = null)
        )
        whenever(eventRegistrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED)).thenReturn(registrations)
        whenever(eventRegistrationMapper.toDtoList(registrations)).thenReturn(dtos)

        val result = service.getAllApprovedRegistrations(eventId)

        assertEquals(dtos, result)
        verify(eventRegistrationMapper).toDtoList(eq(registrations))
    }

    @Test
    fun `resolveCoupleRoleIds returns leader and follower ids`() {
        val leaderId = UUID.randomUUID()
        val followerId = UUID.randomUUID()
        whenever(dancerRoleRepository.findAll()).thenReturn(
            listOf(
                DancerRole(id = leaderId, name = "Leader"),
                DancerRole(id = followerId, name = "Follower")
            )
        )

        val ids = service.resolveCoupleRoleIds()

        assertEquals(leaderId, ids.leaderId)
        assertEquals(followerId, ids.followerId)
    }

    @Test
    fun `resolveCoupleRoleIds throws when leader role missing`() {
        whenever(dancerRoleRepository.findAll()).thenReturn(listOf(DancerRole(id = UUID.randomUUID(), name = "Follower")))

        assertThrows<IllegalStateException> {
            service.resolveCoupleRoleIds()
        }
    }

    private fun createRegistration(roleName: String? = "Leader"): EventRegistration {
        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = UUID.randomUUID(),
            userId = UUID.randomUUID(),
            status = RegistrationStatus.REGISTERED,
            roleId = UUID.randomUUID(),
            role = roleName?.let { DancerRole(id = UUID.randomUUID(), name = it) },
            email = "test@example.com"
        )
    }
}

