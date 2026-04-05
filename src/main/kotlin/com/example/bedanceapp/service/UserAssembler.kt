package com.example.bedanceapp.service

import com.example.bedanceapp.model.Media
import com.example.bedanceapp.model.ProfileData
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.repository.DanceStyleRepository
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.MediaRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import com.example.bedanceapp.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserAssembler(
    private val userRepository: UserRepository,
    private val dancerRoleRepository: DancerRoleRepository,
    private val skillLevelRepository: SkillLevelRepository,
    private val danceStyleRepository: DanceStyleRepository,
    private val mediaRepository: MediaRepository
) {
    fun buildProfile(userId: UUID, request: ProfileData, existing: UserProfile?): UserProfile {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found with id: $userId") }

        val role = request.role?.id?.let { roleId ->
            dancerRoleRepository.findById(UUID.fromString(roleId)).orElseThrow { IllegalArgumentException("Dancer role not found with id: $roleId") }
        }
        val level = request.level?.id?.let { levelId ->
            skillLevelRepository.findById(UUID.fromString(levelId)).orElseThrow { IllegalArgumentException("Skill level not found with id: $levelId") }
        }

        val media: MutableList<Media> = request.media?.map { mediaItem ->
            mediaRepository.findById(mediaItem.id).orElseThrow { IllegalArgumentException("Media not found with id: ${mediaItem.id}") }
        }?.toMutableList() ?: existing?.userMedia?.toMutableList() ?: mutableListOf()

        val danceStyles = request.danceStyles?.map { styleItem ->
            val styleId = UUID.fromString(styleItem.id)
            danceStyleRepository.findById(styleId).orElseThrow { IllegalArgumentException("Dance style not found with id: $styleId") }
        } ?: existing?.danceStyles ?: emptyList()

        return UserProfile(
            userId = userId,
            user = user,
            firstName = request.firstName ?: existing?.firstName,
            lastName = request.lastName ?: existing?.lastName,
            bio = request.bio ?: existing?.bio,
            roleId = role?.id ?: existing?.roleId,
            generalSkillLevel = level ?: existing?.generalSkillLevel,
            city = existing?.city,
            country = existing?.country,
            avatarMediaId = request.avatar?.id ?: existing?.avatarMediaId,
            userMedia = media,
            danceStyles = danceStyles
        )
    }
}

