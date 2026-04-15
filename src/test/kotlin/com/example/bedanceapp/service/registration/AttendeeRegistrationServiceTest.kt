package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.validation.RegistrationAccessValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("AttendeeRegistrationService Tests")
class AttendeeRegistrationServiceTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var dancerRoleRepository: DancerRoleRepository
    @Mock private lateinit var eventRepository: EventRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var registrationStatusService: RegistrationStatusService
    @Mock private lateinit var registrationRecalculateService: RegistrationRecalculateService
    @Mock private lateinit var registrationAccessValidator: RegistrationAccessValidator

    private lateinit var attendeeRegistrationService: AttendeeRegistrationService

    private val eventId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val registrationId = UUID.randomUUID()
    private val organizerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        attendeeRegistrationService = AttendeeRegistrationService(
            eventRegistrationRepository,
            dancerRoleRepository,
            eventRepository,
            userRepository,
            registrationStatusService,
            registrationRecalculateService,
            registrationAccessValidator
        )

        whenever(eventRegistrationRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `deleteRegistrationByRegistrationId deletes interested registration`() {
        val registration = createRegistration(status = RegistrationStatus.INTERESTED)
        whenever(registrationAccessValidator.requireForUserInEvent(registrationId, eventId, userId)).thenReturn(registration)

        attendeeRegistrationService.deleteRegistrationByRegistrationId(eventId, userId, registrationId)

        verify(eventRegistrationRepository).delete(registration)
    }

    @Test
    fun `deleteRegistrationByRegistrationId throws for non interested status`() {
        val registration = createRegistration(status = RegistrationStatus.REGISTERED)
        whenever(registrationAccessValidator.requireForUserInEvent(registrationId, eventId, userId)).thenReturn(registration)

        assertThrows<IllegalArgumentException> {
            attendeeRegistrationService.deleteRegistrationByRegistrationId(eventId, userId, registrationId)
        }

        verify(eventRegistrationRepository, never()).delete(any())
    }

    @Test
    fun `registerUserForEvent creates registration and uses user email`() {
        val event = createEvent(maxAttendees = 20)
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser(userId, "from-db@example.com")))
        whenever(eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(emptyList())
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)).thenReturn(emptyList())
        whenever(registrationStatusService.assignRegistrationStatus(
            registrations = any(),
            status = eq(RegistrationStatus.REGISTERED),
            eventId = eq(eventId),
            roleId = anyOrNull(),
            maxAttendees = anyOrNull()
        )).thenReturn(RegistrationStatus.REGISTERED)

        val saved = attendeeRegistrationService.registerUserForEvent(
            eventId = eventId,
            userId = userId,
            status = RegistrationStatus.REGISTERED,
            roleId = null,
            isAnonymous = true
        )

        assertEquals(RegistrationStatus.REGISTERED, saved.status)
        assertEquals("from-db@example.com", saved.email)
        assertNull(saved.roleId)
        assertNull(saved.waitlistedAt)
    }

    @Test
    fun `registerUserForEvent uses user email when email is not provided`() {
        val event = createEvent(maxAttendees = 10)
        val roleId = UUID.randomUUID()
        val role = DancerRole(id = roleId, name = "LEADER")

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser(userId, "from-db@example.com")))
        whenever(dancerRoleRepository.findById(roleId)).thenReturn(Optional.of(role))
        whenever(eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(emptyList())
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)).thenReturn(emptyList())
        whenever(registrationStatusService.assignRegistrationStatus(
            registrations = any(),
            status = eq(RegistrationStatus.REGISTERED),
            eventId = eq(eventId),
            roleId = eq(roleId),
            maxAttendees = eq(10)
        )).thenReturn(RegistrationStatus.PENDING)

        val saved = attendeeRegistrationService.registerUserForEvent(
            eventId = eventId,
            userId = userId,
            status = RegistrationStatus.REGISTERED,
            roleId = roleId,
            isAnonymous = false
        )

        assertEquals("from-db@example.com", saved.email)
        assertEquals(roleId, saved.roleId)
        assertEquals(RegistrationStatus.PENDING, saved.status)
    }

    @Test
    fun `registerUserForEvent throws when organizer tries to register`() {
        val event = createEvent(maxAttendees = 15, organizerOverride = userId)
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))

        assertThrows<IllegalArgumentException> {
            attendeeRegistrationService.registerUserForEvent(
                eventId = eventId,
                userId = userId,
                status = RegistrationStatus.REGISTERED,
                roleId = null,
                isAnonymous = false
            )
        }
    }

    @Test
    fun `registerUserForEvent throws for invalid requested status`() {
        val event = createEvent(maxAttendees = 15)
        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))

        assertThrows<IllegalArgumentException> {
            attendeeRegistrationService.registerUserForEvent(
                eventId = eventId,
                userId = userId,
                status = RegistrationStatus.WAITLISTED,
                roleId = null,
                isAnonymous = false
            )
        }
    }

    @Test
    fun `registerUserForEvent throws when transition is not allowed`() {
        val event = createEvent(maxAttendees = 15)
        val existing = createRegistration(status = RegistrationStatus.REJECTED)

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser(userId, "from-db@example.com")))
        whenever(eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(listOf(existing))
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)).thenReturn(emptyList())
        whenever(registrationStatusService.assignRegistrationStatus(
            registrations = any(),
            status = eq(RegistrationStatus.REGISTERED),
            eventId = eq(eventId),
            roleId = anyOrNull(),
            maxAttendees = anyOrNull()
        )).thenReturn(RegistrationStatus.REGISTERED)

        assertThrows<IllegalArgumentException> {
            attendeeRegistrationService.registerUserForEvent(
                eventId = eventId,
                userId = userId,
                status = RegistrationStatus.REGISTERED,
                roleId = null,
                isAnonymous = false
            )
        }

        verify(eventRegistrationRepository, never()).save(any())
    }

    @Test
    fun `registerUserForEvent throws when transitioning from cancelled to interested`() {
        val event = createEvent(maxAttendees = 15)
        val existing = createRegistration(status = RegistrationStatus.CANCELLED)

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUser(userId, "from-db@example.com")))
        whenever(eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)).thenReturn(listOf(existing))
        whenever(eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)).thenReturn(emptyList())
        whenever(registrationStatusService.assignRegistrationStatus(
            registrations = any(),
            status = eq(RegistrationStatus.INTERESTED),
            eventId = eq(eventId),
            roleId = anyOrNull(),
            maxAttendees = anyOrNull()
        )).thenReturn(RegistrationStatus.INTERESTED)

        assertThrows<IllegalArgumentException> {
            attendeeRegistrationService.registerUserForEvent(
                eventId = eventId,
                userId = userId,
                status = RegistrationStatus.INTERESTED,
                roleId = null,
                isAnonymous = false
            )
        }

        verify(eventRegistrationRepository, never()).save(any())
    }

    @Test
    fun `cancelRegistration cancels and recalculates when event has capacity`() {
        val event = createEvent(maxAttendees = 8)
        val registration = createRegistration(status = RegistrationStatus.REGISTERED, waitlistedAt = LocalDateTime.now().minusDays(1))

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(registrationAccessValidator.requireForUserInEvent(registrationId, eventId, userId)).thenReturn(registration)

        val saved = attendeeRegistrationService.cancelRegistration(eventId, userId, registrationId)

        assertEquals(RegistrationStatus.CANCELLED, saved.status)
        assertNull(saved.waitlistedAt)
        verify(registrationRecalculateService).recalculate(eventId, 8)
    }

    @Test
    fun `cancelRegistration cancels without recalculation when event has no capacity limit`() {
        val event = createEvent(maxAttendees = null)
        val registration = createRegistration(status = RegistrationStatus.WAITLISTED, waitlistedAt = LocalDateTime.now())

        whenever(eventRepository.findById(eventId)).thenReturn(Optional.of(event))
        whenever(registrationAccessValidator.requireForUserInEvent(registrationId, eventId, userId)).thenReturn(registration)

        val saved = attendeeRegistrationService.cancelRegistration(eventId, userId, registrationId)

        assertEquals(RegistrationStatus.CANCELLED, saved.status)
        assertNull(saved.waitlistedAt)
        verify(registrationRecalculateService, never()).recalculate(any(), any())
    }

    private fun createEvent(maxAttendees: Int?, organizerOverride: UUID = organizerId): Event {
        return Event(
            id = eventId,
            organizerId = organizerOverride,
            organizer = createUser(organizerOverride, "organizer@example.com"),
            eventName = "Test Event",
            description = "test",
            eventDate = LocalDate.now().plusDays(2),
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = BigDecimal.TEN,
            maxAttendees = maxAttendees,
            status = EventStatus.PUBLISHED,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun createRegistration(status: RegistrationStatus, waitlistedAt: LocalDateTime? = null): EventRegistration {
        return EventRegistration(
            id = registrationId,
            eventId = eventId,
            userId = userId,
            user = null,
            status = status,
            roleId = null,
            role = null,
            email = "test@example.com",
            isAnonymous = false,
            responseId = null,
            formResponses = null,
            createdAt = LocalDateTime.now().minusDays(1),
            updatedAt = LocalDateTime.now(),
            waitlistedAt = waitlistedAt
        )
    }

    private fun createUser(id: UUID, email: String): User {
        return User(
            id = id,
            email = email,
            provider = "google",
            providerId = "provider-$id"
        )
    }
}

