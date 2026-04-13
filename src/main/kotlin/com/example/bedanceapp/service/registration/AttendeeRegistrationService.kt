package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.RegistrationAccessValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class AttendeeRegistrationService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val dancerRoleRepository: DancerRoleRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val registrationStatusService: RegistrationStatusService,
    private val registrationRecalculateService: RegistrationRecalculateService,
    private val registrationAccessValidator: RegistrationAccessValidator
) {


    @Transactional
    fun deleteRegistrationByRegistrationId(eventId: UUID, userId: UUID, registrationId: UUID) {
        val registration = registrationAccessValidator.requireForUserInEvent(registrationId, eventId, userId)
        if (registration.status != RegistrationStatus.INTERESTED) {
            throw IllegalArgumentException("Cannot delete non-interested registration")
        }
        eventRegistrationRepository.delete(registration)
    }

    @Transactional
    fun registerUserForEvent(
        eventId: UUID,
        userId: UUID,
        status: RegistrationStatus,
        roleId: UUID?,
        isAnonymous: Boolean
    ): EventRegistration {
        val event = getPublishedEvent(eventId)

        if (event.organizerId == userId) {
            throw IllegalArgumentException("Event organizer cannot register for their own event")
        }

        if (status != RegistrationStatus.INTERESTED && status != RegistrationStatus.REGISTERED) {
            throw IllegalArgumentException("Invalid registration status: $status")
        }

        val userEmail = resolveUserEmail(userId)
        val existing = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId).lastOrNull()
        val role = resolveRole(roleId)
        val allRegistrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)
        val now = LocalDateTime.now()
        val resolvedStatus = registrationStatusService.assignRegistrationStatus(
            allRegistrations,
            status,
            eventId,
            role?.id,
            event.maxAttendees
        )

        val registration = EventRegistration(
            id = existing?.id,
            eventId = eventId,
            userId = userId,
            status = resolvedStatus,
            roleId = role?.id,
            role = role,
            email = userEmail,
            isAnonymous = isAnonymous,
            formResponses = null,
            updatedAt = now,
            waitlistedAt = RegistrationWaitlistTimestampResolver.resolve(
                existing?.status,
                existing?.waitlistedAt,
                resolvedStatus,
                now
            )
        )

        existing?.status?.requireTransitionTo(registration.status)

        return eventRegistrationRepository.save(registration)
    }

    @Transactional
    fun cancelRegistration(eventId: UUID, userId: UUID, registrationId: UUID): EventRegistration {
        val event = getPublishedEvent(eventId)
        val registrationToCancel = registrationAccessValidator.requireForUserInEvent(registrationId, eventId, userId)

        registrationToCancel.status.requireTransitionTo(RegistrationStatus.CANCELLED)

        event.maxAttendees?.let { maxAttendees ->
            registrationRecalculateService.recalculate(eventId, maxAttendees)
        }

        return eventRegistrationRepository.save(
            registrationToCancel.copy(
                status = RegistrationStatus.CANCELLED,
                updatedAt = LocalDateTime.now(),
                waitlistedAt = null
            )
        )
    }

    private fun getPublishedEvent(eventId: UUID): Event {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        require(event.status == EventStatus.PUBLISHED) {
            "Event must be published to register"
        }
        return event
    }

    private fun resolveUserEmail(userId: UUID): String {
        return userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found with id: $userId") }
            .email
    }

    private fun resolveRole(roleId: UUID?): DancerRole? = if (roleId != null) {
        dancerRoleRepository.findById(roleId)
            .orElseThrow { IllegalArgumentException("Invalid role ID: $roleId") }
    } else {
        null
    }
}
