package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RegistrationStatusService(
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    private val eventRegistrationQueryService: EventRegistrationQueryService,
) {
    /**
     * Resolves the final status for an approval-eligible registration.
     * The result depends on current capacity, the registration mode, and
     * role-specific balancing rules in couple mode.
     */
    fun resolveApprovedStatus(
        registrations: List<EventRegistration>,
        settings: EventRegistrationSettings?,
        roleId: UUID?,
        maxAttendees: Int?
    ): RegistrationStatus {
        if (maxAttendees != null) {
            val activeRegistrations = registrations.filter { it.status == RegistrationStatus.REGISTERED }

            // If the event is already full, every new approved registration goes to the waitlist.
            if (activeRegistrations.size >= maxAttendees) {
                return RegistrationStatus.WAITLISTED
            }

            if (settings?.registrationMode == RegistrationMode.COUPLE) {
                val roleIds = eventRegistrationQueryService.resolveCoupleRoleIds()
                val maxPerRole = maxAttendees / 2
                if (maxPerRole <= 0) {
                    return RegistrationStatus.WAITLISTED
                }

                // In couple mode we enforce separate leader/follower limits.
                val activeWithinRole = when (roleId) {
                    roleIds.leaderId -> activeRegistrations.count { it.roleId == roleIds.leaderId }
                    roleIds.followerId -> activeRegistrations.count { it.roleId == roleIds.followerId }
                    // Unknown or missing role cannot be balanced safely, so waitlist it.
                    else -> return RegistrationStatus.WAITLISTED
                }

                if (activeWithinRole >= maxPerRole) {
                    return RegistrationStatus.WAITLISTED
                }
            }
        }

        return RegistrationStatus.REGISTERED
    }

    /**
     * Applies the status decision order:
     * INTERESTED stays unchanged, approval-required events become PENDING,
     * and all other cases defer to capacity/role checks.
     */
    fun assignRegistrationStatus(
        registrations: List<EventRegistration>,
        status: RegistrationStatus,
        eventId: UUID,
        roleId: UUID?,
        maxAttendees: Int?
    ): RegistrationStatus {
        if (status == RegistrationStatus.INTERESTED) {
            return status
        }

        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)
        if (settings?.requireApproval == true) {
            return RegistrationStatus.PENDING
        }

        return resolveApprovedStatus(registrations, settings, roleId, maxAttendees)
    }
}

