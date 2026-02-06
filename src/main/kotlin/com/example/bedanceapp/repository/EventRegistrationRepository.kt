package com.example.bedanceapp.repository

import com.example.bedanceapp.model.EventRegistration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EventRegistrationRepository : JpaRepository<EventRegistration, UUID> {

    fun findByEventId(eventId: UUID): List<EventRegistration>

    fun findByUserId(userId: UUID): List<EventRegistration>

    fun findByEventIdAndUserId(eventId: UUID, userId: UUID): List<EventRegistration>

    fun findByEventIdAndStatus(eventId: UUID, status: String): List<EventRegistration>

    /**
     * Find all registrations for an event excluding INTERESTED status
     * This returns only users who are actually registered (GOING, WAITLISTED, etc.)
     */
    @Query("SELECT er FROM EventRegistration er WHERE er.eventId = :eventId AND er.status != 'INTERESTED'")
    fun findRegisteredUsersByEventId(@Param("eventId") eventId: UUID): List<EventRegistration>
}
