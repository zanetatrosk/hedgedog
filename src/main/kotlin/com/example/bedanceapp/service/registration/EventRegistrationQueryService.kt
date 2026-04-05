package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationStats
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.model.RegistrationUserDto
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for event registration statistics and analytics
 * Handles counting and aggregating registration data
 */
@Service
class EventRegistrationQueryService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val userRepository: UserRepository
) {
    @Transactional(readOnly = true)
    fun getRegistrationCountByEventId(eventId: UUID?, state: RegistrationStatus): Int {
        if (eventId == null) {
            return 0
        }
        val eventRegistrations = eventRegistrationRepository.findByEventIdAndStatus(eventId, state)
        return eventRegistrations.size
    }

    @Transactional
    fun getLastRegistrationByEventIdAndUserId(eventId: UUID, userId: UUID?): EventRegistration? {
        if (userId == null) {
            return null
        }
        val registrations = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
        return registrations.lastOrNull()
    }

    @Transactional(readOnly = true)
    fun getRegistrationRolesCountsByEventId(eventId: UUID): EventRegistrationStats {
        val regs = eventRegistrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED)
        val counts = regs.groupingBy { it.role?.name ?: "Unknown" }.eachCount()

        return EventRegistrationStats(
            total = regs.size,
            leaders = counts["Leader"] ?: 0,
            followers = counts["Follower"] ?: 0,
            both = counts["Both"] ?: 0
        )
    }

    fun getAllApprovedRegistrations(eventId: UUID): List<EventRegistrationDto>{
        val registrations = eventRegistrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED)

        return registrations.map { registration ->
            // Fetch user data for level, but only expose user info if not anonymous
            val user = registration.userId?.let { userId ->
                userRepository.findById(userId).orElse(null)
            }

            EventRegistrationDto(
                registrationId = registration.id.toString(),
                user = if (registration.isAnonymous) {
                    null  // Hide user information when anonymous
                } else {
                    user?.let { u ->
                        RegistrationUserDto(
                            userId = u.id.toString(),
                            name = "${u.profile?.firstName ?: ""} ${u.profile?.lastName ?: ""}".trim(),
                            avatar = u.profile?.avatarMediaId?.toString()
                        )
                    }
                },
                level = user?.profile?.generalSkillLevel?.name,  // Still show level
                role = registration.role?.name  // Still show role
            )
        }
    }
}

