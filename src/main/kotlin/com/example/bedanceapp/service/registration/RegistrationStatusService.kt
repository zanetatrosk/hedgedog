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
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {
    fun resolveApprovedStatus(
        registrations: List<EventRegistration>,
        settings: EventRegistrationSettings?,
        roleId: UUID?,
        maxAttendees: Int?
    ): RegistrationStatus {
        if (maxAttendees != null) {
            val activeStatuses = listOf(RegistrationStatus.REGISTERED)
            val activeRegistrations = registrations.filter { it.status in activeStatuses }

            if (settings?.registrationMode == RegistrationMode.COUPLE) {
                val activeWithinRole = activeRegistrations.filter { it.roleId == roleId }
                if (activeWithinRole.size >= maxAttendees / 2) {
                    return RegistrationStatus.WAITLISTED
                }
            }
            if (activeRegistrations.size >= maxAttendees) {
                return RegistrationStatus.WAITLISTED
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

