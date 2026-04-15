package com.example.bedanceapp.service.registration
import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.RegistrationAction
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.validation.EventAccessValidator
import com.example.bedanceapp.service.validation.RegistrationAccessValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
@DisplayName("OrganizerRegistrationService Tests")
class OrganizerRegistrationServiceTest {
    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var eventRepository: EventRepository
    @Mock private lateinit var registrationStatusService: RegistrationStatusService
    @Mock private lateinit var registrationRecalculateService: RegistrationRecalculateService
    @Mock private lateinit var googleFormSyncService: GoogleFormSyncService
    @Mock private lateinit var eventAccessValidator: EventAccessValidator
    @Mock private lateinit var registrationAccessValidator: RegistrationAccessValidator
    @Mock private lateinit var eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
    private lateinit var organizerRegistrationService: OrganizerRegistrationService
    private val organizerId = UUID.randomUUID()
    private val eventId = UUID.randomUUID()
    private val registrationId = UUID.randomUUID()
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        organizerRegistrationService = OrganizerRegistrationService(
            eventRegistrationRepository,
            registrationStatusService,
            registrationRecalculateService,
            googleFormSyncService,
            eventAccessValidator,
            registrationAccessValidator,
            eventRegistrationSettingsRepository
        )
        whenever(eventRegistrationRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.PUBLISHED)).thenReturn(createEvent(maxAttendees = 8))
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId)).thenReturn(createEvent(maxAttendees = 8))
    }
    @Test
    @DisplayName("updateRegistrationStatus approves pending registration")
    fun testUpdateRegistrationStatusApprove() {
        val registration = createRegistration(status = RegistrationStatus.PENDING, waitlistedAt = LocalDateTime.now())
        whenever(registrationAccessValidator.requireForEvent(registrationId, eventId)).thenReturn(registration)
        whenever(registrationStatusService.resolveApprovedStatus(
            registrations = any(),
            settings = anyOrNull(),
            roleId = anyOrNull(),
            maxAttendees = any()
        )).thenReturn(RegistrationStatus.REGISTERED)
        val updated = organizerRegistrationService.updateRegistrationStatus(
            eventId = eventId,
            registrationId = registrationId,
            organizerId = organizerId,
            action = RegistrationAction.APPROVE
        )
        assertEquals(RegistrationStatus.REGISTERED, updated.status)
        assertNull(updated.waitlistedAt)
        verify(eventRegistrationRepository).flush()
        verify(registrationRecalculateService, org.mockito.kotlin.never()).recalculate(any(), any())
    }
    @Test
    @DisplayName("updateRegistrationStatus rejects registration and recalculates capacity")
    fun testUpdateRegistrationStatusReject() {
        val registration = createRegistration(status = RegistrationStatus.REGISTERED)
        whenever(registrationAccessValidator.requireForEvent(registrationId, eventId)).thenReturn(registration)
        val updated = organizerRegistrationService.updateRegistrationStatus(
            eventId = eventId,
            registrationId = registrationId,
            organizerId = organizerId,
            action = RegistrationAction.REJECT
        )
        assertEquals(RegistrationStatus.REJECTED, updated.status)
        assertNull(updated.waitlistedAt)
        verify(eventRegistrationRepository).flush()
        verify(registrationRecalculateService).recalculate(eventId, 8)
    }
    @Test
    @DisplayName("updateRegistrationStatus rejects invalid action")
    fun testUpdateRegistrationStatusInvalidAction() {
        whenever(registrationAccessValidator.requireForEvent(registrationId, eventId))
            .thenReturn(createRegistration(status = RegistrationStatus.REGISTERED))
        assertThrows<IllegalArgumentException> {
            organizerRegistrationService.updateRegistrationStatus(
                eventId = eventId,
                registrationId = registrationId,
                organizerId = organizerId,
                action = RegistrationAction.CANCEL
            )
        }
        verify(eventRegistrationRepository, org.mockito.kotlin.never()).save(any())
        verify(registrationRecalculateService, org.mockito.kotlin.never()).recalculate(any(), any())
    }
    @Test
    @DisplayName("syncGoogleFormData delegates sync to Google form service")
    fun testSyncGoogleFormData() {
        val event = createEvent(maxAttendees = 12)
        whenever(eventAccessValidator.requireOwnedEvent(eventId, organizerId)).thenReturn(event)
        organizerRegistrationService.syncGoogleFormData(eventId, organizerId)
        verify(googleFormSyncService).syncRegistrationData(eventId, organizerId, 12)
    }
    private fun createEvent(maxAttendees: Int?): Event {
        return Event(
            id = eventId,
            organizerId = organizerId,
            organizer = createUser(organizerId),
            eventName = "Test Event",
            description = "Test",
            eventDate = LocalDate.now().plusDays(1),
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
            userId = UUID.randomUUID(),
            user = null,
            status = status,
            roleId = null,
            role = null,
            email = "test@example.com",
            isAnonymous = false,
            responseId = null,
            formResponses = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            waitlistedAt = waitlistedAt
        )
    }
    private fun createUser(id: UUID): User {
        return User(
            id = id,
            email = "organizer@example.com",
            provider = "google",
            providerId = "google-organizer"
        )
    }
}
