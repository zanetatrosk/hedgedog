package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationStats
import com.example.bedanceapp.model.EventRegistrationDto
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.service.mapping.EventRegistrationMapper
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
    private val eventRegistrationMapper: EventRegistrationMapper,
    private val dancerRoleRepository: DancerRoleRepository
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
            followers = counts["Follower"] ?: 0
        )
    }

    @Transactional(readOnly = true)
    fun getAllApprovedRegistrations(eventId: UUID): List<EventRegistrationDto> {
        val registrations = eventRegistrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.REGISTERED)
        return eventRegistrationMapper.toDtoList(registrations)
    }

    fun resolveCoupleRoleIds(): StatusCoupleRoleIds {
        val rolesByName = dancerRoleRepository.findAll()
            .filter { it.id != null }
            .associateBy { it.name.lowercase() }

        val leaderId = rolesByName["leader"]?.id
            ?: throw IllegalStateException("Leader role is not configured")
        val followerId = rolesByName["follower"]?.id
            ?: throw IllegalStateException("Follower role is not configured")

        return StatusCoupleRoleIds(leaderId, followerId)
    }
}

data class StatusCoupleRoleIds(
    val leaderId: UUID,
    val followerId: UUID
)

