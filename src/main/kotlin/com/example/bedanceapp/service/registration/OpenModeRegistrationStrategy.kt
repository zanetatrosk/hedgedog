package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset

@Component
open class OpenModeRegistrationStrategy(
    protected val eventRegistrationRepository: EventRegistrationRepository,
    protected val skillLevelRepository: SkillLevelRepository
) : RegistrationDataStrategy {

    @Transactional(readOnly = true)
    override fun getRegistrationData(event: Event): RegistrationData {
        val headers = getHeaders()
        val registrations = getRegistrations(event)
        return RegistrationData(headers, registrations)
    }

    protected fun getHeaders(): List<Header> {
        val skillLevels = skillLevelRepository.findAll()
        return listOf(
            RegistrationHeaders.FULLNAME,
            RegistrationHeaders.EMAIL,
            RegistrationHeaders.experience(skillLevels),
            RegistrationHeaders.UPDATED_AT
        )
    }

    protected fun getRegistrations(event: Event): List<RegistrationRow> {
        val eventId = event.id ?: throw IllegalArgumentException("Event ID cannot be null")
        // Fetch only registered users, excluding those with INTERESTED status
        val registrations = eventRegistrationRepository.findByEventIdAndStatusNot(eventId, RegistrationStatus.INTERESTED)

        return registrations.map { registration ->
            mapRegistrationToRow(registration)
        }
    }

    protected fun mapRegistrationToRow(registration: EventRegistration): RegistrationRow {
        val fullName = buildFullName(registration)

        return RegistrationRow(
            id = registration.id.toString(),
            user = RegistrationUserDto(
                email = registration.user?.email ?: "",
                userId = registration.user?.id
            ),
            data = buildDataFields(registration, fullName),
            status = registration.status
        )
    }

    /**
     * Build the data fields for a registration
     * Override this to add mode-specific fields (e.g., role for couple mode)
     */
    protected fun buildDataFields(registration: EventRegistration, fullName: String): List<RegistrationDataDto> {
        return listOf(
            RegistrationDataDto(RegistrationHeaders.FULLNAME.id, fullName),
            RegistrationDataDto(RegistrationHeaders.EMAIL.id, registration.user?.email ?: ""),
            RegistrationDataDto(RegistrationHeaders.EXPERIENCE_ID, registration.user?.profile?.generalSkillLevel?.name ?: ""),
            RegistrationDataDto(RegistrationHeaders.UPDATED_AT.id, registration.updatedAt.atOffset(ZoneOffset.UTC).toString())
        )
    }

    /**
     * Helper to build full name from registration
     */
    protected fun buildFullName(registration: EventRegistration): String {
        return "${registration.user?.profile?.firstName ?: ""} ${registration.user?.profile?.lastName ?: ""}".trim()
    }
}

