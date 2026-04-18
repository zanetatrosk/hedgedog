package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("OpenModeRegistrationStrategy Tests")
class OpenModeRegistrationStrategyTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var skillLevelRepository: SkillLevelRepository

    private lateinit var strategy: OpenModeRegistrationStrategy

    private val eventId = UUID.randomUUID()
    private val organizerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = OpenModeRegistrationStrategy(eventRegistrationRepository, skillLevelRepository)
    }

    @Test
    fun `getRegistrationData builds headers and maps registration row`() {
        val beginner = SkillLevel(name = "Beginner", levelOrder = 3)
        val intermediate = SkillLevel(name = "Intermediate", levelOrder = 1)
        val advanced = SkillLevel(name = "Advanced", levelOrder = 2)
        val registration = createRegistrationWithProfile()

        whenever(skillLevelRepository.findAll()).thenReturn(listOf(beginner, intermediate, advanced))
        whenever(eventRegistrationRepository.findByEventIdAndStatusNot(eventId, RegistrationStatus.INTERESTED))
            .thenReturn(listOf(registration))

        val data = strategy.getRegistrationData(createEvent(eventId))

        assertEquals(4, data.headers.size)
        assertEquals(RegistrationHeaders.FULLNAME.id, data.headers[0].id)
        assertEquals(RegistrationHeaders.EMAIL.id, data.headers[1].id)
        assertEquals(RegistrationHeaders.EXPERIENCE_ID, data.headers[2].id)
        assertEquals(RegistrationHeaders.UPDATED_AT.id, data.headers[3].id)

        val experienceHeader = data.headers[2] as ChoiceHeader
        assertEquals(listOf("Intermediate", "Advanced", "Beginner"), experienceHeader.answerSet)

        assertEquals(1, data.registrations.size)
        val row = data.registrations.first()
        assertEquals(registration.id.toString(), row.id)
        assertEquals("dancer@example.com", row.user.email)
        assertEquals(registration.userId, row.user.userId)
        assertEquals(RegistrationStatus.REGISTERED, row.status)
        assertEquals("Ana Fox", row.data.first { it.id == RegistrationHeaders.FULLNAME.id }.value)
        assertEquals("dancer@example.com", row.data.first { it.id == RegistrationHeaders.EMAIL.id }.value)
        assertEquals("Intermediate", row.data.first { it.id == RegistrationHeaders.EXPERIENCE_ID }.value)
        assertEquals(registration.updatedAt.atOffset(ZoneOffset.UTC).toString(), row.data.first { it.id == RegistrationHeaders.UPDATED_AT.id }.value)

        verify(eventRegistrationRepository).findByEventIdAndStatusNot(eq(eventId), eq(RegistrationStatus.INTERESTED))
    }

    @Test
    fun `getRegistrationData maps empty fallback values when user profile is missing`() {
        whenever(skillLevelRepository.findAll()).thenReturn(emptyList())
        whenever(eventRegistrationRepository.findByEventIdAndStatusNot(eventId, RegistrationStatus.INTERESTED))
            .thenReturn(listOf(createRegistrationWithoutUser()))

        val data = strategy.getRegistrationData(createEvent(eventId))
        val row = data.registrations.first()

        assertEquals("", row.user.email)
        assertEquals(null, row.user.userId)
        assertEquals("", row.data.first { it.id == RegistrationHeaders.FULLNAME.id }.value)
        assertEquals("", row.data.first { it.id == RegistrationHeaders.EMAIL.id }.value)
        assertEquals("", row.data.first { it.id == RegistrationHeaders.EXPERIENCE_ID }.value)
    }

    @Test
    fun `getRegistrationData throws when event id is null`() {
        whenever(skillLevelRepository.findAll()).thenReturn(emptyList())

        assertThrows<IllegalArgumentException> {
            strategy.getRegistrationData(createEvent(id = null))
        }
    }

    private fun createRegistrationWithProfile(): EventRegistration {
        val userId = UUID.randomUUID()
        val baseUser = User(
            id = userId,
            email = "dancer@example.com",
            provider = "google",
            providerId = "provider-1"
        )
        val profile = UserProfile(
            userId = userId,
            user = baseUser,
            firstName = "Ana",
            lastName = "Fox",
            generalSkillLevel = SkillLevel(name = "Intermediate", levelOrder = 1)
        )

        val user = baseUser.copy(profile = profile)

        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            userId = userId,
            user = user,
            status = RegistrationStatus.REGISTERED,
            email = "dancer@example.com",
            updatedAt = LocalDateTime.of(2026, 4, 10, 12, 0)
        )
    }

    private fun createRegistrationWithoutUser(): EventRegistration {
        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            userId = null,
            user = null,
            status = RegistrationStatus.PENDING,
            email = "no-user@example.com"
        )
    }

    private fun createEvent(id: UUID?): Event {
        return Event(
            id = id,
            organizerId = organizerId,
            organizer = User(
                id = organizerId,
                email = "organizer@example.com",
                provider = "google",
                providerId = "org-1"
            ),
            eventName = "Open Training",
            description = "desc",
            eventDate = LocalDate.now().plusDays(1),
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = BigDecimal.ZERO,
            maxAttendees = 20,
            status = EventStatus.PUBLISHED,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}

