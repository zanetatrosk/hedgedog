package com.example.bedanceapp.service.mapping

import com.example.bedanceapp.model.DanceStyle
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.EventMedia
import com.example.bedanceapp.model.Media
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.MediaRepository
import com.example.bedanceapp.service.MediaService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("UserMapper Tests")
class UserMapperTest {

    @Mock private lateinit var mediaService: MediaService
    @Mock private lateinit var dancerRoleRepository: DancerRoleRepository
    @Mock private lateinit var mediaRepository: MediaRepository

    private lateinit var mapper: UserMapper

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mapper = UserMapper(mediaService, dancerRoleRepository, mediaRepository)
    }

    @Test
    fun `getAvatarMedia returns null for null avatar id`() {
        val avatar = mapper.getAvatarMedia(null)

        assertNull(avatar)
        verify(mediaRepository, never()).findById(any())
    }

    @Test
    fun `getAvatarMedia returns mapped dto when media exists`() {
        val avatarId = UUID.randomUUID()
        val media = Media(id = avatarId, mediaType = "image", filePath = "/tmp/a.jpg")
        val dto = EventMedia(type = "image", url = "http://localhost/api/media/$avatarId", id = avatarId)

        whenever(mediaRepository.findById(avatarId)).thenReturn(Optional.of(media))
        whenever(mediaService.mapToDTO(media)).thenReturn(dto)

        val avatar = mapper.getAvatarMedia(avatarId)

        assertEquals(dto, avatar)
    }

    @Test
    fun `toProfileData maps role level styles media and avatar`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "user@example.com", provider = "google", providerId = "p1")
        val roleId = UUID.randomUUID()
        val avatarId = UUID.randomUUID()
        val role = DancerRole(id = roleId, name = "Leader")
        val skill = SkillLevel(id = UUID.randomUUID(), name = "Intermediate", levelOrder = 2)
        val danceStyle = DanceStyle(id = UUID.randomUUID(), name = "Salsa")
        val media = Media(id = UUID.randomUUID(), mediaType = "image", filePath = "/tmp/profile.jpg")
        val mediaDto = EventMedia(type = "image", url = "http://localhost/api/media/${media.id}", id = media.id!!)
        val avatarMedia = Media(id = avatarId, mediaType = "image", filePath = "/tmp/avatar.jpg")
        val avatarDto = EventMedia(type = "image", url = "http://localhost/api/media/$avatarId", id = avatarId)

        val profile = UserProfile(
            userId = userId,
            user = user,
            firstName = "Ana",
            lastName = "Nowak",
            bio = "Dancer",
            roleId = roleId,
            generalSkillLevel = skill,
            avatarMediaId = avatarId,
            userMedia = mutableListOf(media),
            danceStyles = listOf(danceStyle)
        )

        whenever(dancerRoleRepository.findById(roleId)).thenReturn(Optional.of(role))
        whenever(mediaRepository.findById(avatarId)).thenReturn(Optional.of(avatarMedia))
        whenever(mediaService.mapToDTO(avatarMedia)).thenReturn(avatarDto)
        whenever(mediaService.mapToDTO(media)).thenReturn(mediaDto)

        val result = mapper.toProfileData(profile)

        assertEquals("Ana", result.firstName)
        assertEquals("Nowak", result.lastName)
        assertEquals("Leader", result.role?.name)
        assertEquals("Intermediate", result.level?.name)
        assertEquals(listOf("Salsa"), result.danceStyles?.map { it.name })
        assertEquals(listOf(mediaDto), result.media)
        assertEquals(avatarDto, result.avatar)
    }
}

