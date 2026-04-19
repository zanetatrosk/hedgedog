package com.example.bedanceapp.service.registration
import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
@DisplayName("RegistrationStatusService Tests")
class RegistrationStatusServiceTest {
    @Mock
    private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
    @Mock
    private lateinit var eventRegistrationQueryService: EventRegistrationQueryService
    private lateinit var registrationStatusService: RegistrationStatusService
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        registrationStatusService = RegistrationStatusService(
            eventRegistrationSettingsRepository,
            eventRegistrationQueryService
        )
    }
    companion object {
        private val EVENT_ID = UUID.randomUUID()
        private val ROLE_LEADER = UUID.randomUUID()
        private val ROLE_FOLLOWER = UUID.randomUUID()
        private val ROLE_UNKNOWN = UUID.randomUUID()
        private val USER_ID = UUID.randomUUID()
    }
    @Test
    @DisplayName("resolveApprovedStatus returns REGISTERED when capacity not reached")
    fun testResolveApprovedStatusRegisteredWhenUnderCapacity() {
        val registrations = listOf(createEventRegistration(status = RegistrationStatus.REGISTERED))
        val result = registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            settings = EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.OPEN),
            roleId = null,
            maxAttendees = 5
        )
        assertEquals(RegistrationStatus.REGISTERED, result)
    }
    @Test
    @DisplayName("resolveApprovedStatus returns WAITLISTED when capacity reached in OPEN mode")
    fun testResolveApprovedStatusWaitlistedWhenFullCapacityOpenMode() {
        val registrations = listOf(
            createEventRegistration(status = RegistrationStatus.REGISTERED),
            createEventRegistration(status = RegistrationStatus.REGISTERED),
            createEventRegistration(status = RegistrationStatus.REGISTERED)
        )
        val result = registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            settings = EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.OPEN),
            roleId = null,
            maxAttendees = 3
        )
        assertEquals(RegistrationStatus.WAITLISTED, result)
    }
    @Test
    @DisplayName("resolveApprovedStatus returns WAITLISTED when role capacity reached in COUPLE mode")
    fun testResolveApprovedStatusWaitlistedWhenRoleFullCapacityCoupleMode() {
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds())
            .thenReturn(StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER))
        val registrations = listOf(
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER)
        )
        val result = registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            settings = EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.COUPLE),
            roleId = ROLE_LEADER,
            maxAttendees = 4
        )
        assertEquals(RegistrationStatus.WAITLISTED, result)
    }
    @Test
    @DisplayName("resolveApprovedStatus returns REGISTERED when the opposite couple side has room")
    fun testResolveApprovedStatusRegisteredWhenCoupleSideHasRoom() {
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds())
            .thenReturn(StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER))
        val registrations = listOf(
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_FOLLOWER)
        )
        val result = registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            settings = EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.COUPLE),
            roleId = ROLE_FOLLOWER,
            maxAttendees = 4
        )
        assertEquals(RegistrationStatus.REGISTERED, result)
    }
    @Test
    @DisplayName("assignRegistrationStatus returns PENDING when approval required")
    fun testAssignRegistrationStatusReturnsPendingWhenApprovalRequired() {
        whenever(eventRegistrationSettingsRepository.findByEventId(EVENT_ID)).thenReturn(
            EventRegistrationSettings(
                eventId = EVENT_ID,
                registrationMode = RegistrationMode.OPEN,
                requireApproval = true
            )
        )
        val result = registrationStatusService.assignRegistrationStatus(
            registrations = emptyList(),
            status = RegistrationStatus.REGISTERED,
            eventId = EVENT_ID,
            roleId = null,
            maxAttendees = 10
        )
        assertEquals(RegistrationStatus.PENDING, result)
    }
    @Test
    @DisplayName("resolveApprovedStatus returns WAITLISTED for unknown role in COUPLE mode")
    fun testResolveApprovedStatusWaitlistedForUnknownRoleInCoupleMode() {
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds())
            .thenReturn(StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER))
        val registrations = listOf(
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_FOLLOWER)
        )
        val result = registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            settings = EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.COUPLE),
            roleId = ROLE_UNKNOWN,
            maxAttendees = 6
        )
        assertEquals(RegistrationStatus.WAITLISTED, result)
    }
    @Test
    @DisplayName("resolveApprovedStatus ignores non-active statuses and allows registration when role has space")
    fun testResolveApprovedStatusIgnoresNonActiveStatusesInCoupleMode() {
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds())
            .thenReturn(StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER))
        val registrations = listOf(
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_FOLLOWER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_FOLLOWER),
            createEventRegistration(status = RegistrationStatus.CANCELLED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REJECTED, roleId = ROLE_FOLLOWER)
        )
        val result = registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            settings = EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.COUPLE),
            roleId = ROLE_LEADER,
            maxAttendees = 6
        )
        assertEquals(RegistrationStatus.REGISTERED, result)
    }
    @Test
    @DisplayName("resolveApprovedStatus returns WAITLISTED when leader side is full at capacity 6")
    fun testResolveApprovedStatusWaitlistedWhenLeaderSideFullAtCapacitySix() {
        whenever(eventRegistrationQueryService.resolveCoupleRoleIds())
            .thenReturn(StatusCoupleRoleIds(ROLE_LEADER, ROLE_FOLLOWER))
        val registrations = listOf(
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_LEADER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_FOLLOWER),
            createEventRegistration(status = RegistrationStatus.REGISTERED, roleId = ROLE_FOLLOWER)
        )
        val result = registrationStatusService.resolveApprovedStatus(
            registrations = registrations,
            settings = EventRegistrationSettings(eventId = EVENT_ID, registrationMode = RegistrationMode.COUPLE),
            roleId = ROLE_LEADER,
            maxAttendees = 6
        )
        assertEquals(RegistrationStatus.WAITLISTED, result)
    }
    @Test
    @DisplayName("assignRegistrationStatus returns INTERESTED when status is INTERESTED")
    fun testAssignRegistrationStatusReturnsInterested() {
        val result = registrationStatusService.assignRegistrationStatus(
            registrations = emptyList(),
            status = RegistrationStatus.INTERESTED,
            eventId = EVENT_ID,
            roleId = null,
            maxAttendees = 10
        )
        assertEquals(RegistrationStatus.INTERESTED, result)
    }
    private fun createEventRegistration(
        status: RegistrationStatus,
        roleId: UUID? = null,
        userId: UUID? = USER_ID
    ): EventRegistration {
        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = EVENT_ID,
            userId = userId,
            user = null,
            status = status,
            roleId = roleId,
            role = null,
            email = "test@example.com",
            isAnonymous = false,
            responseId = null,
            formResponses = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            waitlistedAt = null
        )
    }
}
