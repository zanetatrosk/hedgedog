package com.example.bedanceapp.service.mapping

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.EventMedia
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.Media
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("EventRegistrationMapper Tests")
class EventRegistrationMapperTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var userMapper: UserMapper

    private lateinit var mapper: EventRegistrationMapper

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mapper = EventRegistrationMapper(userRepository, userMapper)
    }

    @Test
    fun `toDto returns null user when registration is anonymous`() {
        val userId = UUID.randomUUID()
        val registration = createRegistration(userId = userId, isAnonymous = true)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(createUserWithProfile(userId)))

        val dto = mapper.toDto(registration)

        assertNull(dto.user)
        assertEquals("Intermediate", dto.level)
        assertEquals("Leader", dto.role)
        verify(userMapper, never()).getAvatarMedia(any())
    }

    @Test
    fun `toDto maps user name and avatar for non anonymous registration`() {
        val userId = UUID.randomUUID()
        val avatarId = UUID.randomUUID()
        val user = createUserWithProfile(userId, avatarId)
        val avatarDto = EventMedia(type = "image", url = "http://localhost/api/media/$avatarId", id = avatarId)
        val registration = createRegistration(userId = userId, isAnonymous = false)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(userMapper.getAvatarMedia(avatarId)).thenReturn(avatarDto)

        val dto = mapper.toDto(registration)

        assertEquals(registration.id.toString(), dto.registrationId)
        assertEquals(userId.toString(), dto.user?.userId)
        assertEquals("Ada Lovelace", dto.user?.name)
        assertEquals(avatarDto, dto.user?.avatar)
        assertEquals("Intermediate", dto.level)
        assertEquals("Leader", dto.role)
    }

    @Test
    fun `toDtoList maps each registration`() {
        val first = createRegistration(userId = UUID.randomUUID(), isAnonymous = true)
        val second = createRegistration(userId = UUID.randomUUID(), isAnonymous = true)

        val list = mapper.toDtoList(listOf(first, second))

        assertEquals(2, list.size)
        assertEquals(first.id.toString(), list[0].registrationId)
        assertEquals(second.id.toString(), list[1].registrationId)
    }

    @Test
    fun `toActionResult maps flat registration fields`() {
        val registration = createRegistration(userId = UUID.randomUUID(), isAnonymous = false)

        val dto = mapper.toActionResult(registration)

        assertEquals(registration.id, dto.registrationId)
        assertEquals(registration.eventId, dto.eventId)
        assertEquals(registration.userId, dto.userId)
        assertEquals(registration.status, dto.status)
        assertEquals(registration.roleId, dto.roleId)
        assertEquals(registration.waitlistedAt?.atOffset(ZoneOffset.UTC)?.toString(), dto.waitlistedAt)
        assertEquals(registration.updatedAt.atOffset(ZoneOffset.UTC).toString(), dto.updatedAt)
    }

    private fun createRegistration(userId: UUID, isAnonymous: Boolean): EventRegistration {
        return EventRegistration(
            id = UUID.randomUUID(),
            eventId = UUID.randomUUID(),
            userId = userId,
            user = null,
            status = RegistrationStatus.REGISTERED,
            roleId = UUID.randomUUID(),
            role = DancerRole(id = UUID.randomUUID(), name = "Leader"),
            email = "test@example.com",
            isAnonymous = isAnonymous,
            responseId = null,
            formResponses = null,
            waitlistedAt = null
        )
    }

    private fun createUserWithProfile(userId: UUID, avatarId: UUID = UUID.randomUUID()): User {
        val baseUser = User(id = userId, email = "user@example.com", provider = "google", providerId = "p1")
        val profile = UserProfile(
            userId = userId,
            user = baseUser,
            firstName = "Ada",
            lastName = "Lovelace",
            generalSkillLevel = SkillLevel(id = UUID.randomUUID(), name = "Intermediate", levelOrder = 2),
            avatarMediaId = avatarId,
            userMedia = mutableListOf(Media(id = UUID.randomUUID(), mediaType = "image", filePath = "/tmp/1.jpg"))
        )
        return baseUser.copy(profile = profile)
    }
}

