package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Recalculates waitlisted registrations when event capacity changes or registrations are cancelled/rejected.
 *
 * This service owns the promotion rules so registration orchestration code can stay focused on lifecycle actions.
 */
@Service
class RegistrationRecalculateService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val dancerRoleRepository: DancerRoleRepository,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {

    @Transactional
    fun recalculate(eventId: UUID, maxAttendees: Int) {
        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)
        val allRegistrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)
        val activeRegistrations = allRegistrations.filter {
            it.status == RegistrationStatus.REGISTERED
        }
        val waitlisted = allRegistrations
            .filter { it.status == RegistrationStatus.WAITLISTED }
            .sortedBy { it.createdAt }

        if (waitlisted.isEmpty()) return

        val targetStatus = RegistrationStatus.REGISTERED

        val activeCount = allRegistrations.count {
            it.status == RegistrationStatus.REGISTERED
        }

        val updates = if (settings?.registrationMode == RegistrationMode.COUPLE) {
            calculateCoupleModePromotions(activeRegistrations, waitlisted, maxAttendees, targetStatus)
        } else {
            calculateOpenModePromotions(waitlisted, activeCount, maxAttendees, targetStatus)
        }

        if (updates.isNotEmpty()) {
            eventRegistrationRepository.saveAll(updates)
        }
    }

    private fun calculateOpenModePromotions(
        waitlisted: List<EventRegistration>,
        activeCount: Int,
        maxAttendees: Int,
        targetStatus: RegistrationStatus
    ): List<EventRegistration> {
        val spotsAvailable = maxAttendees - activeCount
        if (spotsAvailable <= 0) return emptyList()

        return waitlisted.take(spotsAvailable).map { it.copy(status = targetStatus) }
    }

    private fun calculateCoupleModePromotions(
        activeRegistrations: List<EventRegistration>,
        waitlisted: List<EventRegistration>,
        maxAttendees: Int,
        targetStatus: RegistrationStatus
    ): List<EventRegistration> {
        if (activeRegistrations.size >= maxAttendees) return emptyList()

        val maxPerRole = maxAttendees / 2
        val roles = dancerRoleRepository.findAll()
        val roleIds = roles.map { it.id }
        val waitlistedByRole = waitlisted.groupBy { it.roleId }.mapValues { it.value.toMutableList() }
        val updates = mutableListOf<EventRegistration>()

        var promoted = true
        while (promoted) {
            promoted = false

            if (activeRegistrations.size + updates.size >= maxAttendees) {
                break
            }

            val roleCounts = roleIds.associateWith { roleId ->
                activeRegistrations.count { it.roleId == roleId } + updates.count { it.roleId == roleId }
            }

            for (roleId in roleIds.sortedBy { roleCounts[it] ?: 0 }) {
                val currentCount = roleCounts[roleId] ?: 0
                if (currentCount < maxPerRole && activeRegistrations.size + updates.size < maxAttendees) {
                    val nextWaitlisted = waitlistedByRole[roleId]
                        ?.firstOrNull { it.status == RegistrationStatus.WAITLISTED }

                    if (nextWaitlisted != null) {
                        val updatedRegistration = nextWaitlisted.copy(status = targetStatus)
                        updates.add(updatedRegistration)
                        waitlistedByRole[roleId]?.remove(nextWaitlisted)
                        promoted = true
                        break
                    }
                }
            }
        }

        return updates
    }
}


