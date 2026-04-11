package com.example.bedanceapp.service.user

import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.model.UserProfileDto
import com.example.bedanceapp.repository.UserProfileRepository
import com.example.bedanceapp.service.mapping.UserMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private lateinit var userProfileRepository: UserProfileRepository
    @Mock private lateinit var userMapper: UserMapper
    @Mock private lateinit var userAssembler: UserAssembler

    private lateinit var service: UserService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = UserService(userProfileRepository, userMapper, userAssembler)
    }

    @Test
    fun `getProfileData returns mapped dto when profile exists`() {
        val userId = UUID.randomUUID()
        val profile = createProfile(userId, "Ada")
        val dto = UserProfileDto(firstName = "Ada", lastName = "Lovelace")
        whenever(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile))
        whenever(userMapper.toProfileData(profile)).thenReturn(dto)

        val result = service.getProfileData(userId)

        assertEquals(dto, result)
    }

    @Test
    fun `getProfileData returns null when profile does not exist`() {
        val userId = UUID.randomUUID()
        whenever(userProfileRepository.findById(userId)).thenReturn(Optional.empty())

        val result = service.getProfileData(userId)

        assertNull(result)
    }

    @Test
    fun `updateProfileData builds saves and maps profile`() {
        val userId = UUID.randomUUID()
        val request = UserProfileDto(firstName = "New", lastName = "Name")
        val existing = createProfile(userId, "Old")
        val built = createProfile(userId, "New")
        val saved = createProfile(userId, "New")
        val mapped = UserProfileDto(firstName = "New", lastName = "Name")

        whenever(userProfileRepository.findById(userId)).thenReturn(Optional.of(existing))
        whenever(userAssembler.buildProfile(userId, request, existing)).thenReturn(built)
        whenever(userProfileRepository.save(built)).thenReturn(saved)
        whenever(userMapper.toProfileData(saved)).thenReturn(mapped)

        val result = service.updateProfileData(userId, request)

        assertEquals(mapped, result)
        verify(userAssembler).buildProfile(userId, request, existing)
        verify(userProfileRepository).save(built)
        verify(userMapper).toProfileData(saved)
    }

    private fun createProfile(userId: UUID, firstName: String): UserProfile {
        return UserProfile(
            userId = userId,
            user = User(id = userId, email = "user@example.com", provider = "google", providerId = "p1"),
            firstName = firstName,
            lastName = "Lovelace",
            bio = "bio"
        )
    }
}


