package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
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
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("RegistrationRecalculateService - Edge Case Scenarios")
class RegistrationRecalculateServiceTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var eventRegistrationQueryService: EventRegistrationQueryService
    @Mock private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository

    @Captor private lateinit var registrationCaptor: ArgumentCaptor<List<EventRegistration>>

    private lateinit var service: RegistrationRecalculateService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = RegistrationRecalculateService(
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
    @DisplayName("OPEN Mode: Promotes exactly the oldest registrations when capacity is partial")
    fun testOpenModePartialCapacityFifo() {
        val now = LocalDateTime.now()

        // 1 active user
        val active = createReg("active", RegistrationStatus.REGISTERED)

        // 3 waitlisted users with distinct times
        val firstWaitlisted = createReg("first", RegistrationStatus.WAITLISTED, waitAt = now.minusHours(3))
        val secondWaitlisted = createReg("second", RegistrationStatus.WAITLISTED, waitAt = now.minusHours(2))
        val thirdWaitlisted = createReg("third", RegistrationStatus.WAITLISTED, waitAt = now.minusHours(1))

        whenever(eventRegistrationSettingsRepository.findByEventId(EVENT_ID))
            .thenReturn(createSettings(RegistrationMode.OPEN))
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(EVENT_ID))
            .thenReturn(listOf(active, firstWaitlisted, secondWaitlisted, thirdWaitlisted))

        // Total capacity 3. 1 is taken, so only 2 spots open.
        // We expect 'first' and 'second' to be promoted, 'third' stays waitlisted.
        service.recalculate(EVENT_ID, 3)

        verify(eventRegistrationRepository).saveAll(registrationCaptor.capture())
        val saved = registrationCaptor.value

        assertEquals(2, saved.size, "Should only promote 2 users to fill the 3-person capacity")

        val promotedIds = saved.map { it.userId }
        assertTrue(promotedIds.contains(firstWaitlisted.userId), "Should contain the oldest waitlisted user")
        assertTrue(promotedIds.contains(secondWaitlisted.userId), "Should contain the second oldest")
        assertFalse(promotedIds.contains(thirdWaitlisted.userId), "Should NOT promote the newest user")
    }

    @Test
    @DisplayName("COUPLE Mode: Skips earliest user if their role is already full")
    fun testCoupleModeRoleCapping() {
        val now = LocalDateTime.now()

        // Scenario: Max 4 people (2 leaders, 2 followers).
        // Current: 2 leaders registered. Leaders are FULL.
        val lead1 = createReg("L1", RegistrationStatus.REGISTERED, ROLE_LEADER)
        val lead2 = createReg("L2", RegistrationStatus.REGISTERED, ROLE_LEADER)

        // Waitlist: Leader (arrived first), then Follower (arrived later).
        val waitLead = createReg("WaitL", RegistrationStatus.WAITLISTED, ROLE_LEADER, waitAt = now.minusHours(2))
        val waitFollow = createReg("WaitF", RegistrationStatus.WAITLISTED, ROLE_FOLLOWER, waitAt = now.minusHours(1))

        whenever(eventRegistrationSettingsRepository.findByEventId(EVENT_ID))
            .thenReturn(createSettings(RegistrationMode.COUPLE))
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds())
            .thenReturn(StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER))
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(EVENT_ID))
            .thenReturn(listOf(lead1, lead2, waitLead, waitFollow))

        // Recalculate for max 4.
        service.recalculate(EVENT_ID, 4)

        verify(eventRegistrationRepository).saveAll(registrationCaptor.capture())
        val saved = registrationCaptor.value

        // Only the follower should be promoted because leader spots are 2/2 full.
        assertEquals(1, saved.size)
        assertEquals(waitFollow.userId, saved.first().userId, "Follower should be promoted even though they were later in line than the Leader")
    }

    @Test
    @DisplayName("COUPLE Mode: Promotes nothing if capacity is met but roles are imbalanced")
    fun testCoupleModeNoPromotionWhenSpecificRoleFull() {
        // Max 2 (1 leader, 1 follower). Leader is already registered.
        val lead1 = createReg("L1", RegistrationStatus.REGISTERED, ROLE_LEADER)

        // Waitlist only has leaders.
        val waitLead = createReg("WaitL", RegistrationStatus.WAITLISTED, ROLE_LEADER)

        whenever(eventRegistrationSettingsRepository.findByEventId(EVENT_ID))
            .thenReturn(createSettings(RegistrationMode.COUPLE))
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds())
            .thenReturn(StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER))
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(EVENT_ID))
            .thenReturn(listOf(lead1, waitLead))

        service.recalculate(EVENT_ID, 2)

        // Should not save anything because the only person waiting is a leader, and lead capacity is 1/1.
        verify(eventRegistrationRepository, never()).saveAll(any<List<EventRegistration>>())    }

    // --- Helpers ---

    private fun createReg(
        name: String,
        status: RegistrationStatus,
        roleId: UUID? = null,
        waitAt: LocalDateTime? = null
    ): EventRegistration {
        val id = UUID.randomUUID()
        return EventRegistration(
            id = id,
            eventId = EVENT_ID,
            userId = id, // using ID as a pseudo-username for tracking
            status = status,
            roleId = roleId,
            email = "$name@dance.com",
            createdAt = LocalDateTime.now().minusDays(1),
            waitlistedAt = waitAt,
            updatedAt = LocalDateTime.now(),
            isAnonymous = false,
            user = null, role = null, responseId = null, formResponses = null
        )
    }

    private fun createSettings(mode: RegistrationMode) =
        com.example.bedanceapp.model.EventRegistrationSettings(
            eventId = EVENT_ID,
            registrationMode = mode,
            requireApproval = false
        )
}


