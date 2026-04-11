package com.example.bedanceapp.service.registration
import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
@DisplayName("RegistrationRecalculateService Tests")
class RegistrationRecalculateServiceTest {
    @Mock
    private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock
    private lateinit var eventRegistrationQueryService: EventRegistrationQueryService
    @Mock
    private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
    @Captor
    private lateinit var registrationCaptor: ArgumentCaptor<List<EventRegistration>>
    private lateinit var registrationRecalculateService: RegistrationRecalculateService
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        registrationRecalculateService = RegistrationRecalculateService(
            eventRegistrationRepository,
            eventRegistrationQueryService,
            eventRegistrationSettingsRepository
        )
    }
    companion object {
        private val EVENT_ID = UUID.randomUUID()
        private val ROLE_LEADER = UUID.randomUUID()
        private val ROLE_FOLLOWER = UUID.randomUUID()
    }
    @Test
    @DisplayName("recalculate promotes waitlisted users in FIFO order for OPEN mode")
    fun testRecalculateOpenModePromotesFifo() {
        val user1 = UUID.randomUUID()
        val user2 = UUID.randomUUID()
        val user3 = UUID.randomUUID()
        val now = LocalDateTime.now()
        val registrations = listOf(
            createRegistration(user1, RegistrationStatus.REGISTERED),
            createRegistration(user2, RegistrationStatus.WAITLISTED, waitlistedAt = now.minusMinutes(10)),
            createRegistration(user3, RegistrationStatus.WAITLISTED, waitlistedAt = now.minusMinutes(5))
        )
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(EVENT_ID)).thenReturn(registrations)
        whenever(eventRegistrationSettingsRepository.findByEventId(EVENT_ID)).thenReturn(
            com.example.bedanceapp.model.EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.OPEN)
        )
        registrationRecalculateService.recalculate(EVENT_ID, 3)
        verify(eventRegistrationRepository).saveAll(registrationCaptor.capture())
        val saved = registrationCaptor.value
        assertEquals(2, saved.size)
        assertEquals(user2, saved[0].userId)
        assertEquals(user3, saved[1].userId)
        assertTrue(saved.all { it.status == RegistrationStatus.REGISTERED })
        assertTrue(saved.all { it.waitlistedAt == null })
    }
    @Test
    @DisplayName("recalculate does nothing when no waitlisted users")
    fun testRecalculateNoActionWhenNoWaitlistedUsers() {
        val user1 = UUID.randomUUID()
        val registrations = listOf(createRegistration(user1, RegistrationStatus.REGISTERED))
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(EVENT_ID)).thenReturn(registrations)
        whenever(eventRegistrationSettingsRepository.findByEventId(EVENT_ID)).thenReturn(
            com.example.bedanceapp.model.EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.OPEN)
        )
        registrationRecalculateService.recalculate(EVENT_ID, 5)
        verify(eventRegistrationRepository, never()).saveAll(any<List<EventRegistration>>())
    }
    @Test
    @DisplayName("recalculate balances leader and follower waitlist promotions in COUPLE mode")
    fun testRecalculateCoupleModeBalancesRoles() {
        val now = LocalDateTime.now()
        val leaderActive = createRegistration(UUID.randomUUID(), RegistrationStatus.REGISTERED, ROLE_LEADER)
        val followerActive = createRegistration(UUID.randomUUID(), RegistrationStatus.REGISTERED, ROLE_FOLLOWER)
        val leaderWaitlisted = createRegistration(UUID.randomUUID(), RegistrationStatus.WAITLISTED, ROLE_LEADER, now.minusMinutes(15))
        val followerWaitlisted = createRegistration(UUID.randomUUID(), RegistrationStatus.WAITLISTED, ROLE_FOLLOWER, now.minusMinutes(5))
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(EVENT_ID)).thenReturn(
            listOf(leaderActive, followerActive, leaderWaitlisted, followerWaitlisted)
        )
        whenever(eventRegistrationSettingsRepository.findByEventId(EVENT_ID)).thenReturn(
            com.example.bedanceapp.model.EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.COUPLE)
        )
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds()).thenReturn(
            StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER)
        )
        registrationRecalculateService.recalculate(EVENT_ID, 4)
        verify(eventRegistrationRepository).saveAll(registrationCaptor.capture())
        val saved = registrationCaptor.value
        assertEquals(2, saved.size)
        assertEquals(ROLE_LEADER, saved[0].roleId)
        assertEquals(ROLE_FOLLOWER, saved[1].roleId)
        assertTrue(saved.all { it.status == RegistrationStatus.REGISTERED })
        assertTrue(saved.all { it.waitlistedAt == null })
    }
    private fun createRegistration(
        userId: UUID,
        status: RegistrationStatus,
        roleId: UUID? = null,
        waitlistedAt: LocalDateTime? = null
    ): EventRegistration {
        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = EVENT_ID,
            userId = userId,
            user = null,
            status = status,
            roleId = roleId,
            role = null,
            email = "$userId@example.com",
            isAnonymous = false,
            responseId = null,
            formResponses = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            waitlistedAt = waitlistedAt
        )
    }
}
