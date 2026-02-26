package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationCount
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.registration.GoogleFormRegistrationStrategy
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Service responsible for managing event registrations (CRUD operations)
 * Handles user registration, cancellation, and retrieval of registration records
 */
@Service
class EventRegistrationManager(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val dancerRoleRepository: DancerRoleRepository,
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    @Lazy private val googleFormRegistrationStrategy: GoogleFormRegistrationStrategy
) {

    @Transactional
    fun getLastRegistrationByEventIdAndUserId(eventId: UUID, userId: UUID?): EventRegistration? {
        if (userId == null) {
            return null
        }
        val registrations = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
        return registrations.lastOrNull()
    }

    @Transactional
    fun deleteRegistrationByRegistrationId(registrationId: UUID){
        val registration = eventRegistrationRepository.findById(registrationId)
            .orElseThrow { IllegalArgumentException("Registration not found with id: $registrationId") }
        if( registration.status != RegistrationStatus.INTERESTED){
            throw IllegalArgumentException("Cannot delete non-interested registration")
        }
        eventRegistrationRepository.delete(registration)

    }

    @Transactional(readOnly = true)
    fun getRegistrationRolesCountsByEventId(eventRegistrations: List<EventRegistration>): EventRegistrationCount {
        val roles = dancerRoleRepository.findAll().map { Pair(it.name, 0)  }
        val rolesCount = roles.toMap().toMutableMap()
        for (registration in eventRegistrations) {
            registration.role?.name?.let { roleName ->
                rolesCount[roleName] = (rolesCount[roleName]?.plus(1) ?: 0)
            }
        }
        return EventRegistrationCount(eventRegistrations.size, rolesCount["Leader"] ?: 0, rolesCount["Follower"] ?: 0)
    }

    fun assignEventStatus(
        registrations: List<EventRegistration>,
        eventId: UUID,
        status: RegistrationStatus,
        role: UUID?,
        maxAttendees: Int?
    ): RegistrationStatus {
        if( status == RegistrationStatus.INTERESTED ){
            return status
        }
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventId)
        val statuses = listOf(RegistrationStatus.GOING, RegistrationStatus.PENDING)
        val registrationsGoingOrPending = registrations.filter { it.status in statuses }
        if( maxAttendees != null ) {
            if( registrationSettings?.registrationMode === RegistrationMode.COUPLE ) {
                val registrationsWithinThatRole = registrationsGoingOrPending.filter { it.roleId == role  }
                if( registrationsWithinThatRole.size >= maxAttendees.div(2)){
                    return RegistrationStatus.WAITLISTED
                }
            }
            if( registrationsGoingOrPending.size >= maxAttendees ){
                return RegistrationStatus.WAITLISTED
            }
        }

        if( registrationSettings?.requireApproval == true ){
            return RegistrationStatus.PENDING
        }

        return RegistrationStatus.GOING
    }

    fun getActiveRegistrations(registrations: List<EventRegistration>): List<EventRegistration> {
        val statuses = listOf(RegistrationStatus.GOING, RegistrationStatus.PENDING)
        return registrations.filter { it.status in statuses }
    }

    fun recalculateRegistrations(eventId: UUID, maxAttendees: Int, registrations: List<EventRegistration>){
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventId)
        val statuses = listOf(RegistrationStatus.GOING, RegistrationStatus.PENDING)
        val activeRegistrations = registrations.filter { it.status in statuses }.toMutableList()

        // Get all waitlisted registrations ordered by creation time (FIFO)
        val waitlistedRegistrations = registrations
            .filter { it.status == RegistrationStatus.WAITLISTED }
            .sortedBy { it.createdAt }
            .toMutableList()

        if (waitlistedRegistrations.isEmpty()) {
            return
        }

        // Determine target status based on approval requirement
        val targetStatus = if (registrationSettings?.requireApproval == true) {
            RegistrationStatus.PENDING
        } else {
            RegistrationStatus.GOING
        }

        // Collect registrations to update
        val toUpdate = mutableListOf<EventRegistration>()

        // Process waitlisted registrations
        if (registrationSettings?.registrationMode == RegistrationMode.COUPLE) {
            // For COUPLE mode: balance roles and promote from waitlist
            val maxPerRole = maxAttendees / 2

            // Get all roles from active registrations
            val roles = dancerRoleRepository.findAll()
            val roleIds = roles.map { it.id }

            // Group waitlisted registrations by role
            val waitlistedByRole = waitlistedRegistrations.groupBy { it.roleId }.mapValues { it.value.toMutableList() }

            // Keep promoting until we can't anymore
            var promoted = true
            while (promoted) {
                promoted = false

                if (activeRegistrations.size >= maxAttendees) {
                    break
                }

                // Find the role with fewer registrations
                val roleCounts = roleIds.associateWith { roleId ->
                    activeRegistrations.count { it.roleId == roleId }
                }

                // Try to promote from the role with fewer registrations or ties
                for (roleId in roleIds.sortedBy { roleCounts[it] ?: 0 }) {
                    val currentCount = roleCounts[roleId] ?: 0
                    if (currentCount < maxPerRole && activeRegistrations.size < maxAttendees) {
                        val nextWaitlisted = waitlistedByRole[roleId]
                            ?.firstOrNull { it.status == RegistrationStatus.WAITLISTED }

                        if (nextWaitlisted != null) {
                            val updatedRegistration = nextWaitlisted.copy(status = targetStatus)
                            toUpdate.add(updatedRegistration)
                            activeRegistrations.add(updatedRegistration)
                            waitlistedByRole[roleId]?.remove(nextWaitlisted)
                            promoted = true
                            break
                        }
                    }
                }
            }
        } else {
            // For OPEN mode: promote from waitlist based on total capacity
            val spotsAvailable = maxAttendees - activeRegistrations.size

            waitlistedRegistrations.take(spotsAvailable).forEach { waitlisted ->
                val updatedRegistration = waitlisted.copy(status = targetStatus)
                toUpdate.add(updatedRegistration)
            }
        }

        // Save all updates in a batch
        if (toUpdate.isNotEmpty()) {
            eventRegistrationRepository.saveAll(toUpdate)
        }
    }

    @Transactional
    fun handleOrganizerAction(
        registrationId: UUID,
        action: OrganizerAction
    ): EventRegistration {

        val registration = eventRegistrationRepository.findById(registrationId).get()
        val newStatus = when (action) {
            OrganizerAction.APPROVE -> {
                when (registration.status) {
                    RegistrationStatus.PENDING -> RegistrationStatus.GOING
                    else -> throw IllegalStateException("Cannot approve from ${registration.status}")
                }
            }

            OrganizerAction.REJECT -> {
                when (registration.status) {
                    RegistrationStatus.PENDING, RegistrationStatus.GOING -> RegistrationStatus.REJECTED
                    else -> throw IllegalStateException("Cannot reject from ${registration.status}")
                }
            }
        }

        val registrationWithNewStatus = registration.copy(status = newStatus)
        val savedRegistration = eventRegistrationRepository.save(registrationWithNewStatus)
        eventRegistrationRepository.flush()

        // If we rejected a PENDING or GOING registration, recalculate to promote waitlisted registrations
        if (action == OrganizerAction.REJECT) {
            val event = eventRepository.findById(registration.eventId).get()
            val allRegistrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(registration.eventId)
            event.maxAttendees?.let { maxAttendees ->
                recalculateRegistrations(registration.eventId, maxAttendees, allRegistrations)
            }
        }

        return savedRegistration
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

        if(status != RegistrationStatus.INTERESTED && status != RegistrationStatus.GOING){
            throw IllegalArgumentException("Invalid registration status: $status")
        }

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

        // Get all registrations for this event to calculate status
        val allRegistrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)

        val registration = EventRegistration(
            id = existing?.id,
            eventId = eventId,
            userId = userId,
            status = assignEventStatus(allRegistrations, eventId, status, role.id, event.maxAttendees),
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
    fun cancelRegistration(eventId: UUID, userId: UUID, registrationId: UUID): Boolean {
        val event = eventRepository.findById(eventId)
        val registrationToCancel = eventRegistrationRepository.findById(registrationId)
        if( registrationToCancel.isEmpty ){
            throw IllegalArgumentException("Registration not found with id: $registrationId")
        }
        if( registrationToCancel.get().status == RegistrationStatus.INTERESTED ) {
            throw IllegalArgumentException("Cannot cancel an interested registration")
        }
        if( userId != registrationToCancel.get().userId && userId != event.get().organizerId ){
            throw IllegalArgumentException("User is not authorized to cancel this registration")
        }

        // Cancel the registration first
        val updatedRegistration = registrationToCancel.get().copy(status = RegistrationStatus.CANCELLED)
        eventRegistrationRepository.save(updatedRegistration)
        eventRegistrationRepository.flush()

        // Get all registrations for the event (including the just-cancelled one)
        val allRegistrations = eventRegistrationRepository.findByEventIdOrderByCreatedAt(eventId)

        // Recalculate to promote waitlisted registrations if there's now space
        event.get().maxAttendees?.let { maxAttendees ->
            recalculateRegistrations(eventId, maxAttendees, allRegistrations)
        }

        return true
    }

    /**
     * Sync event registration form structure with Google Forms
     * This fetches the latest form structure from Google Forms and updates the database
     */
    @Transactional
    fun syncGoogleFormData(eventId: UUID) {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        // Sync form structure to database
        googleFormRegistrationStrategy.syncRegistrationData(event)
    }

    fun assertOrganizer(eventId: UUID, organizerId: UUID) {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }
        if (event.organizerId != organizerId) {
            throw IllegalArgumentException("User is not authorized to perform this action")
        }
    }
}

