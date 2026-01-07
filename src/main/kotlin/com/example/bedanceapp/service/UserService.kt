package com.example.bedanceapp.service

import com.example.bedanceapp.model.User
import com.example.bedanceapp.model.UserProfile
import com.example.bedanceapp.repository.UserProfileRepository
import com.example.bedanceapp.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userProfileRepository: UserProfileRepository
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
}
