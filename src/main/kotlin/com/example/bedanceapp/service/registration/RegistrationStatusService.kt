package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
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
    fun resolveApprovedStatus(
        registrations: List<EventRegistration>,
        settings: EventRegistrationSettings?,
        roleId: UUID?,
        maxAttendees: Int?
    ): RegistrationStatus {
        if (maxAttendees != null) {
            val activeRegistrations = registrations.filter { it.status == RegistrationStatus.REGISTERED }

            if (activeRegistrations.size >= maxAttendees) {
                return RegistrationStatus.WAITLISTED
            }

            if (settings?.registrationMode == RegistrationMode.COUPLE) {
                val roleIds = eventRegistrationQueryService.resolveCoupleRoleIds()
                val maxPerRole = maxAttendees / 2
                if (maxPerRole <= 0) {
                    return RegistrationStatus.WAITLISTED
                }

                val activeWithinRole = when (roleId) {
                    roleIds.leaderId -> activeRegistrations.count { it.roleId == roleIds.leaderId }
                    roleIds.followerId -> activeRegistrations.count { it.roleId == roleIds.followerId }
                    else -> return RegistrationStatus.WAITLISTED
                }

                if (activeWithinRole >= maxPerRole) {
                    return RegistrationStatus.WAITLISTED
                }
            }
        }

        return RegistrationStatus.REGISTERED
    }

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

