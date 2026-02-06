package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.repository.EventRegistrationRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Base strategy for OPEN registration mode
 * Simple registration without roles or partner matching
 * Can be extended by CoupleModeRegistrationStrategy to add role functionality
 */
@Component
open class OpenModeRegistrationStrategy(
    protected val eventRegistrationRepository: EventRegistrationRepository
) : RegistrationDataStrategy {

    @Transactional(readOnly = true)
    override fun getRegistrationData(event: Event): RegistrationData {
        val headers = getHeaders()
        val registrations = getRegistrations(event)
        return RegistrationData(headers, registrations)
    }

    /**
     * Get headers for this registration mode
     * Override this to add mode-specific headers (e.g., role for couple mode)
     */
    protected open fun getHeaders(): List<Header> {
        return listOf(
            RegistrationHeaders.FULLNAME,
            RegistrationHeaders.EXPERIENCE,
            RegistrationHeaders.STATUS,
            RegistrationHeaders.CREATED_AT
        )
    }

    /**
     * Get all registrations for an event
     * Only includes users who are actually registered (excludes INTERESTED status)
     */
    protected open fun getRegistrations(event: Event): List<RegistrationRow> {
        val eventId = event.id ?: throw IllegalArgumentException("Event ID cannot be null")
        // Fetch only registered users, excluding those with INTERESTED status
        val registrations = eventRegistrationRepository.findRegisteredUsersByEventId(eventId)

        return registrations.map { registration ->
            mapRegistrationToRow(registration)
        }
    }

    /**
     * Map a single registration to a row
     * Override this to add mode-specific data fields
     */
    protected open fun mapRegistrationToRow(registration: EventRegistration): RegistrationRow {
        val fullName = buildFullName(registration)

        return RegistrationRow(
            id = registration.id.toString(),
            user = RegistrationUserDto(
                email = registration.user?.email ?: "",
                userId = registration.user?.id
            ),
            data = buildDataFields(registration, fullName)
        )
    }

    /**
     * Build the data fields for a registration
     * Override this to add mode-specific fields (e.g., role for couple mode)
     */
    protected open fun buildDataFields(registration: EventRegistration, fullName: String): List<RegistrationDataDto> {
        return listOf(
            RegistrationDataDto(RegistrationHeaders.FULLNAME.id, fullName),
            RegistrationDataDto(RegistrationHeaders.EXPERIENCE.id, registration.user?.profile?.generalSkillLevel?.name ?: ""),
            RegistrationDataDto(RegistrationHeaders.STATUS.id, registration.status),
            RegistrationDataDto(RegistrationHeaders.CREATED_AT.id, registration.createdAt.toString())
        )
    }

    /**
     * Helper to build full name from registration
     */
    protected fun buildFullName(registration: EventRegistration): String {
        return "${registration.user?.profile?.firstName ?: ""} ${registration.user?.profile?.lastName ?: ""}".trim()
    }
}

