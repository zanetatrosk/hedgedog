package com.example.bedanceapp.service.user

import com.example.bedanceapp.model.UserProfileDto
import com.example.bedanceapp.repository.UserProfileRepository
import com.example.bedanceapp.service.mapping.UserMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userProfileRepository: UserProfileRepository,
    private val userMapper: UserMapper,
    private val userAssembler: UserAssembler
) {

    @Transactional(readOnly = true)
    fun getProfileData(userId: UUID): UserProfileDto? {
        return userProfileRepository.findById(userId)
            .map { userMapper.toProfileData(it) }
            .orElse(null)
    }

    @Transactional
    fun updateProfileData(userId: UUID, request: UserProfileDto): UserProfileDto {
        val existing = userProfileRepository.findById(userId).orElse(null)
        val updatedProfile = userAssembler.buildProfile(userId, request, existing)
        val saved = userProfileRepository.save(updatedProfile)
        return userMapper.toProfileData(saved)
    }
}
