package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * Recalculates waitlisted registrations when event capacity changes or registrations are cancelled/rejected.
 */
@Service
class RegistrationRecalculateService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRegistrationQueryService: EventRegistrationQueryService,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {

    @Transactional
    fun recalculate(eventId: UUID, maxAttendees: Int) {
        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)
        val registrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)

        val active = registrations.filter { it.status == RegistrationStatus.REGISTERED }
        val waitlisted = registrations.filter { it.status == RegistrationStatus.WAITLISTED }
            .sortedBy { it.waitlistedAt ?: it.createdAt }

        if (waitlisted.isEmpty()) return

        val updates = if (settings?.registrationMode == RegistrationMode.COUPLE) {
            calculateCoupleMode(active, waitlisted, maxAttendees)
        } else {
            calculateOpenMode(active.size, waitlisted, maxAttendees)
        }

        if (updates.isNotEmpty()) eventRegistrationRepository.saveAll(updates)
    }

    private fun calculateOpenMode(activeCount: Int, waitlisted: List<EventRegistration>, max: Int) =
        waitlisted.take((max - activeCount).coerceAtLeast(0)).map { it.promote() }

    private fun calculateCoupleMode(
        active: List<EventRegistration>,
        waitlisted: List<EventRegistration>,
        maxAttendees: Int
    ): List<EventRegistration> {
        val roleIds = eventRegistrationQueryService.resolveCoupleRoleIds()
        val maxPerRole = maxAttendees / 2

        // Track remaining capacity for each role
        val remainingRoleCapacity = mutableMapOf(
            roleIds.leaderId to (maxPerRole - active.count { it.roleId == roleIds.leaderId }),
            roleIds.followerId to (maxPerRole - active.count { it.roleId == roleIds.followerId })
        )

        return waitlisted.filter { registration ->
            val capacity = remainingRoleCapacity[registration.roleId] ?: 0
            if (capacity > 0) {
                remainingRoleCapacity[registration.roleId!!] = capacity - 1
                true
            } else false
        }.map { it.promote() }
    }

    // Helper to handle the transition logic and object copying
    private fun EventRegistration.promote(): EventRegistration {
        this.status.requireTransitionTo(RegistrationStatus.REGISTERED)
        return this.copy(
            status = RegistrationStatus.REGISTERED,
            updatedAt = LocalDateTime.now(),
            waitlistedAt = null
        )
    }
}



