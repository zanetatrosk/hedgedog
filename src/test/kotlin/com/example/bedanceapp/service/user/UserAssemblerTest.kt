package com.example.bedanceapp.service.user

import com.example.bedanceapp.model.CodebookItem
import com.example.bedanceapp.model.DanceStyle
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.EventMedia
import com.example.bedanceapp.model.Media
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.model.UserProfileDto
import com.example.bedanceapp.repository.DanceStyleRepository
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.MediaRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import com.example.bedanceapp.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("UserAssembler Tests")
class UserAssemblerTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var dancerRoleRepository: DancerRoleRepository
    @Mock private lateinit var skillLevelRepository: SkillLevelRepository
    @Mock private lateinit var danceStyleRepository: DanceStyleRepository
    @Mock private lateinit var mediaRepository: MediaRepository

    private lateinit var assembler: UserAssembler

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        assembler = UserAssembler(
            userRepository,
            dancerRoleRepository,
            skillLevelRepository,
            danceStyleRepository,
            mediaRepository
        )
    }

    @Test
    fun `buildProfile throws when user does not exist`() {
        val userId = UUID.randomUUID()
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            assembler.buildProfile(userId, UserProfileDto(), null)
        }

        assertEquals("User not found with id: $userId", exception.message)
    }

    @Test
    fun `buildProfile keeps existing values when request fields are null`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "user@example.com", provider = "google", providerId = "p1")
        val existingRoleId = UUID.randomUUID()
        val existingLevel = SkillLevel(id = UUID.randomUUID(), name = "Intermediate", levelOrder = 2)
        val existingAvatarId = UUID.randomUUID()
        val existingMedia = mutableListOf(Media(id = UUID.randomUUID(), mediaType = "image", filePath = "/tmp/a.jpg"))
        val existingDanceStyles = listOf(DanceStyle(id = UUID.randomUUID(), name = "Bachata"))
        val existing = UserProfile(
            userId = userId,
            user = user,
            firstName = "Ada",
            lastName = "Lovelace",
            bio = "Old bio",
            roleId = existingRoleId,
            generalSkillLevel = existingLevel,
            avatarMediaId = existingAvatarId,
            userMedia = existingMedia,
            danceStyles = existingDanceStyles
        )
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

        val result = assembler.buildProfile(userId, UserProfileDto(), existing)

        assertEquals("Ada", result.firstName)
        assertEquals("Lovelace", result.lastName)
        assertEquals("Old bio", result.bio)
        assertEquals(existingRoleId, result.roleId)
        assertEquals(existingLevel, result.generalSkillLevel)
        assertEquals(existingAvatarId, result.avatarMediaId)
        assertEquals(existingMedia, result.userMedia)
        assertEquals(existingDanceStyles, result.danceStyles)
    }

    @Test
    fun `buildProfile resolves role level media and dance styles from request`() {
        val userId = UUID.randomUUID()
        val user = User(id = userId, email = "user@example.com", provider = "google", providerId = "p1")
        val roleId = UUID.randomUUID()
        val levelId = UUID.randomUUID()
        val styleId = UUID.randomUUID()
        val mediaId = UUID.randomUUID()

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(dancerRoleRepository.findById(roleId)).thenReturn(Optional.of(DancerRole(id = roleId, name = "Leader")))
        whenever(skillLevelRepository.findById(levelId)).thenReturn(Optional.of(SkillLevel(id = levelId, name = "Beginner", levelOrder = 1)))
        whenever(danceStyleRepository.findById(styleId)).thenReturn(Optional.of(DanceStyle(id = styleId, name = "Salsa")))
        whenever(mediaRepository.findById(mediaId)).thenReturn(Optional.of(Media(id = mediaId, mediaType = "image", filePath = "/tmp/m.jpg")))

        val request = UserProfileDto(
            firstName = "Grace",
            role = CodebookItem(roleId.toString(), "Leader"),
            level = CodebookItem(levelId.toString(), "Beginner"),
            danceStyles = listOf(CodebookItem(styleId.toString(), "Salsa")),
            media = listOf(EventMedia(type = "image", url = "https://example.com/m.jpg", id = mediaId)),
            avatar = EventMedia(type = "image", url = "https://example.com/a.jpg", id = mediaId)
        )

        val result = assembler.buildProfile(userId, request, null)

        assertEquals("Grace", result.firstName)
        assertEquals(roleId, result.roleId)
        assertEquals(levelId, result.generalSkillLevel?.id)
        assertEquals(mediaId, result.avatarMediaId)
        assertEquals(listOf(styleId), result.danceStyles.mapNotNull { it.id })
        assertEquals(listOf(mediaId), result.userMedia.mapNotNull { it.id })
    }
}

