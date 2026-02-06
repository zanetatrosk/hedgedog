package com.example.bedanceapp.service

import com.example.bedanceapp.model.*
import com.example.bedanceapp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository,
    private val userDanceStyleRepository: UserDanceStyleRepository,
    private val userMediaRepository: UserMediaRepository,
    private val dancerRoleRepository: DancerRoleRepository,
    private val skillLevelRepository: SkillLevelRepository,
    private val danceStyleRepository: DanceStyleRepository,
    private val mediaRepository: MediaRepository,
    private val mediaService: MediaService
) {

    fun getAllUsers(): List<User> {
        return userRepository.findAll()
    }

    fun getUserById(id: UUID): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun createUser(user: User): User {
        return userRepository.save(user)
    }

    fun getProfileByUserId(userId: UUID): UserProfile? {
        return userProfileRepository.findById(userId).orElse(null)
    }

    @Transactional
    fun createOrUpdateProfile(profile: UserProfile): UserProfile {
        return userProfileRepository.save(profile)
    }

    @Transactional(readOnly = true)
    fun getProfileData(userId: UUID): ProfileData? {
        val profile = userProfileRepository.findById(userId).orElse(null) ?: return null

        // Get role
        val roleId = profile.roleId
        val role = if (roleId != null) {
            dancerRoleRepository.findById(roleId).map {
                CodebookItem(it.id.toString(), it.name)
            }.orElse(null)
        } else null

        // Get skill level
        val levelId = profile.generalSkillLevel?.id
        val level = if (levelId != null) {
            skillLevelRepository.findById(levelId).map {
                CodebookItem(it.id.toString(), it.name)
            }.orElse(null)
        } else null

        // Get dance styles
        val danceStyles = userDanceStyleRepository.findByUserId(userId).mapNotNull {
            it.danceStyle?.let { ds -> CodebookItem(ds.id.toString(), ds.name) }
        }

        // Get media
        val userMediaList = userMediaRepository.findByUserId(userId)
        val media = userMediaList.mapNotNull { um ->
            um.media?.let { mediaService.mapToDTO(it) }
        }

        // Get avatar (primary media)
        val avatar = profile.avatarMediaId?.let { mediaId ->
            mediaRepository.findById(mediaId).orElse(null)?.let { mediaService.mapToDTO(it) }
        }

        return ProfileData(
            firstName = profile.firstName,
            lastName = profile.lastName,
            bio = profile.bio,
            role = role,
            danceStyles = danceStyles,
            media = media,
            level = level,
            avatar = avatar
        )
    }

    @Transactional
    fun updateProfileData(userId: UUID, request: ProfileData): ProfileData {
        // Get or create user profile
        val existingProfile = userProfileRepository.findById(userId).orElse(null)
        val mediaList = request.media?.map { media ->
            mediaRepository.findById(media.id)
                .orElseThrow { IllegalArgumentException("Media not found with id: ${media.id}") }
        }
        val danceStyles = request.danceStyles?.map {
            danceStyleRepository.findById(UUID.fromString(it.id))
        }

        // Update or create profile
        val updatedProfile = UserProfile(
            userId = userId,
            user = userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("User not found with id: $userId") },
            firstName = request.firstName ?: existingProfile?.firstName,
            lastName = request.lastName ?: existingProfile?.lastName,
            bio = request.bio ?: existingProfile?.bio,
            roleId = request.role?.id?.let { UUID.fromString(it) } ?: existingProfile?.roleId,
            generalSkillLevel = request.level?.id?.let {
                skillLevelRepository.findById(UUID.fromString(it)).orElse(null)
            } ?: existingProfile?.generalSkillLevel,
            city = existingProfile?.city,
            country = existingProfile?.country,
            avatarMediaId = request.avatar?.id ?: existingProfile?.avatarMediaId,
            userMedia = mediaList?.toMutableList() ?: mutableListOf(),
            danceStyles = danceStyles?.mapNotNull { it.orElse(null) } ?: emptyList()
        )
        userProfileRepository.save(updatedProfile)

        return getProfileData(userId)
            ?: throw IllegalStateException("Failed to retrieve updated profile")
    }
}
