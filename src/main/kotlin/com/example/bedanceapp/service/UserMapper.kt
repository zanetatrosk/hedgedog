package com.example.bedanceapp.service

import com.example.bedanceapp.model.ProfileData
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.model.toCodebook
import com.example.bedanceapp.model.toCodebookList
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.MediaRepository
import org.springframework.stereotype.Component

@Component
class UserMapper(
    private val mediaService: MediaService,
    private val dancerRoleRepository: DancerRoleRepository,
    private val mediaRepository: MediaRepository
) {
    fun toProfileData(profile: UserProfile): ProfileData {
        val role = profile.roleId?.let { roleId ->
            dancerRoleRepository.findById(roleId).orElse(null)?.toCodebook()
        }
        val avatar = profile.avatarMediaId?.let { avatarId ->
            mediaRepository.findById(avatarId).orElse(null)?.let { mediaService.mapToDTO(it) }
        }

        return ProfileData(
            firstName = profile.firstName,
            lastName = profile.lastName,
            bio = profile.bio,
            role = role,
            level = profile.generalSkillLevel?.toCodebook(),
            danceStyles = profile.danceStyles.toCodebookList(),
            media = profile.userMedia.mapNotNull { mediaService.mapToDTO(it) },
            avatar = avatar
        )
    }
}

