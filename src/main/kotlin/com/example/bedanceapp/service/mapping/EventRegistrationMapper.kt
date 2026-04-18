package com.example.bedanceapp.service.mapping

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationActionResultDto
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.model.RegistrationUserDto
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.UserRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class EventRegistrationMapper(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {
    fun toActionResultList(registrations: List<EventRegistration>): List<EventRegistrationActionResultDto> {
        return registrations.map { toActionResult(it) }
    }

    fun toActionResult(registration: EventRegistration): EventRegistrationActionResultDto {
        return EventRegistrationActionResultDto(
            registrationId = requireNotNull(registration.id) { "Registration ID is missing" },
            eventId = registration.eventId,
            userId = registration.userId,
            status = registration.status,
            roleId = registration.roleId,
            waitlistedAt = registration.waitlistedAt?.let(::toUtcOffsetString),
            updatedAt = toUtcOffsetString(registration.updatedAt)
        )
    }

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

    private fun toUtcOffsetString(value: LocalDateTime): String {
        return value.atOffset(ZoneOffset.UTC).toString()
    }
}

