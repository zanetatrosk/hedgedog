package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationCount
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class EventRegistrationService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val dancerRoleRepository: DancerRoleRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getRegistrationRolesCountsByEventId(eventId: UUID?, state: RegistrationStatus): EventRegistrationCount {
        if (eventId == null) {
            return EventRegistrationCount(0, 0, 0)
        }
        val eventRegistrations = eventRegistrationRepository.findByEventIdAndStatus(eventId, state)
        val roles = dancerRoleRepository.findAll().map { Pair(it.name, 0)  }
        val rolesCount = roles.toMap().toMutableMap()
        for (registration in eventRegistrations) {
            registration.role?.name?.let { roleName ->
                rolesCount[roleName] = (rolesCount[roleName]?.plus(1) ?: 0)
            }
        }
        return EventRegistrationCount(eventRegistrations.size, rolesCount["Leader"] ?: 0, rolesCount["Follower"] ?: 0)
    }

    fun getRegistrationCountByEventId(eventId: UUID?, state: RegistrationStatus): Int {
        if (eventId == null) {
            return 0
        }
        val eventRegistrations = eventRegistrationRepository.findByEventIdAndStatus(eventId, state)
        return eventRegistrations.size
    }

    @Transactional(readOnly = true)
    fun getRegistrationsByUserId(userId: UUID): List<EventRegistration> {
        return eventRegistrationRepository.findByUserId(userId)
    }

    @Transactional
    fun createRegistration(registration: EventRegistration): EventRegistration {
        return eventRegistrationRepository.save(registration)
    }

    @Transactional
    fun getLastRegistrationByEventIdAndUserId(eventId: UUID, userId: UUID?): EventRegistration? {
        if (userId == null) {
            return null
        }
        val registrations = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
        return registrations.lastOrNull()
    }

    /**
     * Register a user for an event with a specific status and role
     * Valid statuses: interested, going, waitlisted
     */
    @Transactional
    fun registerUserForEvent(
        eventId: UUID,
        userId: UUID,
        status: RegistrationStatus,
        roleId: UUID?,
        email: String?
    ): EventRegistration {
        // Check if user is the organizer of this event
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        if (event.organizerId == userId) {
            throw IllegalArgumentException("Event organizer cannot register for their own event")
        }

        // Validate status
        val validStatuses = listOf(RegistrationStatus.entries.toTypedArray())

        // Get user email - use provided email or fetch from user profile
        val userEmail = email ?: run {
            val user = userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("User not found with id: $userId") }
            user.email
        }

        // Check if user already has a registration for this event
        val existingRegistrations = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
        val existing = existingRegistrations.lastOrNull()

        // Get role - use provided roleId or default to null
        val role = if (roleId != null) {
            dancerRoleRepository.findById(roleId)
                .orElseThrow { IllegalArgumentException("Invalid role ID: $roleId") }
        } else {
            // Default to "Both" role if no role specified
            dancerRoleRepository.findAll().firstOrNull { it.name == "Both" }
                ?: throw IllegalStateException("Default 'Both' role not found")
        }

        val registration = EventRegistration(
            id = existing?.id,
            eventId = eventId,
            userId = userId,
            status = status,
            roleId = role.id,
            role = role,
            email = userEmail,
            formResponses = null
        )

        return eventRegistrationRepository.save(registration)
    }



    /**
     * Remove/cancel a user's registration for an event
     */
    @Transactional
    fun cancelRegistration(eventId: UUID, userId: UUID): Boolean {
        val registrations = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
        if (registrations.isEmpty()) {
            return false
        }

        // Delete all registrations for this user and event
        registrations.forEach { eventRegistrationRepository.delete(it) }
        return true
    }
}