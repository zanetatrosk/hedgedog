package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationAction
import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.registration.GoogleFormSyncService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrganizerRegistrationService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRepository: EventRepository,
    private val registrationRecalculateService: RegistrationRecalculateService,
    private val googleFormSyncService: GoogleFormSyncService,
    private val eventAccessValidator: EventAccessValidator,
    private val registrationAccessValidator: RegistrationAccessValidator
) {

    @Transactional
    fun updateRegistrationStatus(
        eventId: UUID,
        registrationId: UUID,
        organizerId: UUID,
        action: RegistrationAction
    ): EventRegistration {
        eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.PUBLISHED)
        val registration = registrationAccessValidator.requireForEvent(registrationId, eventId)

        val organizerAction = when (action) {
            RegistrationAction.APPROVE -> OrganizerAction.APPROVE
            RegistrationAction.REJECT -> OrganizerAction.REJECT
            else -> throw IllegalArgumentException("Invalid organizer action")
        }

        return handleOrganizerAction(registration, organizerAction)
    }

    @Transactional
    fun handleOrganizerAction(registration: EventRegistration, action: OrganizerAction): EventRegistration {
        val newStatus = when (action) {
            OrganizerAction.APPROVE -> when (registration.status) {
                RegistrationStatus.PENDING -> RegistrationStatus.REGISTERED
                else -> throw IllegalStateException("Cannot approve from ${registration.status}")
            }

            OrganizerAction.REJECT -> when (registration.status) {
                RegistrationStatus.PENDING, RegistrationStatus.REGISTERED -> RegistrationStatus.REJECTED
                else -> throw IllegalStateException("Cannot reject from ${registration.status}")
            }
        }

        val savedRegistration = eventRegistrationRepository.save(registration.copy(status = newStatus))
        eventRegistrationRepository.flush()

        if (action == OrganizerAction.REJECT) {
            val event = eventRepository.findById(registration.eventId)
                .orElseThrow { IllegalArgumentException("Event not found with id: ${registration.eventId}") }
            event.maxAttendees?.let { maxAttendees ->
                registrationRecalculateService.recalculate(registration.eventId, maxAttendees)
            }
        }

        return savedRegistration
    }

    /**
     * Sync event registration form structure with Google Forms.
     */
    fun syncGoogleFormData(eventId: UUID, organizerId: UUID) {
        val event = eventAccessValidator.requireOwnedEvent(eventId, organizerId)
        googleFormSyncService.syncRegistrationData(event)
    }
}
