package com.example.bedanceapp.service.mapping

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.model.RegistrationUserDto
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class EventRegistrationMapper(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {
    fun toDtoList(registrations: List<EventRegistration>): List<EventRegistrationDto> {
        return registrations.map { toDto(it) }
    }

    fun toDto(registration: EventRegistration): EventRegistrationDto {
        val user = registration.userId?.let { userId ->
            userRepository.findById(userId).orElse(null)
        }

        return EventRegistrationDto(
            registrationId = registration.id.toString(),
            user = if (registration.isAnonymous) {
                null
            } else {
                user?.let { u ->
                    RegistrationUserDto(
                        userId = u.id.toString(),
                        name = buildUserName(u),
                        avatar = userMapper.getAvatarMedia(u.profile?.avatarMediaId)
                    )
                }
            },
            level = user?.profile?.generalSkillLevel?.name,
            role = registration.role?.name
        )
    }

    private fun buildUserName(user: User): String {
        return "${user.profile?.firstName ?: ""} ${user.profile?.lastName ?: ""}".trim()
    }
}

