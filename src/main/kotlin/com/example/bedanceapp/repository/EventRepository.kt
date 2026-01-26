package com.example.bedanceapp.repository

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EventRepository : JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    fun findByStatus(status: EventStatus): List<Event>

    fun findByStatus(status: EventStatus, pageable: Pageable): Page<Event>

    fun findByParentEventId(parentEventId: UUID): List<Event>

    fun findByOrganizerId(organizerId: UUID): List<Event>

    fun findByOrganizerId(organizerId: UUID, pageable: Pageable): Page<Event>

    fun findByOrganizerIdAndStatus(organizerId: UUID, status: EventStatus, pageable: Pageable): Page<Event>

    @Query("""
        SELECT er.status 
        FROM EventRegistration er 
        WHERE er.eventId = :eventId 
        AND er.userId = :userId
        ORDER BY er.createdAt DESC
        LIMIT 1
    """)
    fun findUserRegistrationStatus(
        @Param("eventId") eventId: UUID,
        @Param("userId") userId: UUID
    ): String?
}
