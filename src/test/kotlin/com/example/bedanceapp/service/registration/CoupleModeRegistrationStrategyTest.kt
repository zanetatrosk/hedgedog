package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("CoupleModeRegistrationStrategy Tests")
class CoupleModeRegistrationStrategyTest {

    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var skillLevelRepository: SkillLevelRepository
    @Mock private lateinit var dancerRoleRepository: DancerRoleRepository

    private lateinit var strategy: CoupleModeRegistrationStrategy

    private val eventId = UUID.randomUUID()
    private val organizerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        strategy = CoupleModeRegistrationStrategy(
            eventRegistrationRepository,
            skillLevelRepository,
            dancerRoleRepository
        )
    }

    @Test
    fun `getRegistrationData includes role header before updatedAt and role value in row`() {
        val registration = createRegistration(roleName = "Leader")

        whenever(skillLevelRepository.findAll()).thenReturn(listOf(SkillLevel(name = "Beginner", levelOrder = 1)))
        whenever(dancerRoleRepository.findAll()).thenReturn(
            listOf(
                DancerRole(id = UUID.randomUUID(), name = "Leader"),
                DancerRole(id = UUID.randomUUID(), name = "Follower")
            )
        )
        whenever(eventRegistrationRepository.findByEventIdAndStatusNot(eventId, RegistrationStatus.INTERESTED))
            .thenReturn(listOf(registration))

        val data = strategy.getRegistrationData(createEvent())

        assertEquals(5, data.headers.size)
        assertEquals(RegistrationHeaders.FULLNAME.id, data.headers[0].id)
        assertEquals(RegistrationHeaders.EMAIL.id, data.headers[1].id)
        assertEquals(RegistrationHeaders.EXPERIENCE_ID, data.headers[2].id)
        assertEquals(CoupleHeaders.ROLE_ID, data.headers[3].id)
        assertEquals(RegistrationHeaders.UPDATED_AT.id, data.headers[4].id)

        val row = data.registrations.first()
        assertEquals("Leader", row.data[3].value)
        assertEquals(RegistrationHeaders.UPDATED_AT.id, row.data[4].id)
    }

    @Test
    fun `getRegistrationData maps empty role when registration has null role`() {
        val registration = createRegistration(roleName = null)

        whenever(skillLevelRepository.findAll()).thenReturn(emptyList())
        whenever(dancerRoleRepository.findAll()).thenReturn(emptyList())
        whenever(eventRegistrationRepository.findByEventIdAndStatusNot(eventId, RegistrationStatus.INTERESTED))
            .thenReturn(listOf(registration))

        val data = strategy.getRegistrationData(createEvent())

        val row = data.registrations.first()
        assertEquals("", row.data[3].value)
    }

    private fun createEvent(): Event {
        return Event(
            id = eventId,
            organizerId = organizerId,
            organizer = User(
                id = organizerId,
                email = "organizer@example.com",
                provider = "google",
                providerId = "provider-org"
            ),
            eventName = "Couple Event",
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

    private fun createRegistration(roleName: String?): EventRegistration {
        val userId = UUID.randomUUID()
        val user = User(
            id = userId,
            email = "dancer@example.com",
            provider = "google",
            providerId = "provider-user",
            profile = UserProfile(
                userId = userId,
                user = User(
                    id = userId,
                    email = "dancer@example.com",
                    provider = "google",
                    providerId = "provider-profile"
                ),
                firstName = "Ana",
                lastName = "Fox",
                generalSkillLevel = SkillLevel(name = "Intermediate", levelOrder = 1)
            )
        )

        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = eventId,
            userId = userId,
            user = user,
            status = RegistrationStatus.REGISTERED,
            roleId = roleName?.let { UUID.randomUUID() },
            role = roleName?.let { DancerRole(id = UUID.randomUUID(), name = it) },
            email = "dancer@example.com",
            updatedAt = LocalDateTime.of(2026, 4, 10, 12, 0)
        )
    }
}

