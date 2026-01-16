package com.example.bedanceapp.repository

import com.example.bedanceapp.model.Event
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
    fun findByStatus(status: String): List<Event>

    fun findByStatus(status: String, pageable: Pageable): Page<Event>

    fun findByParentEventId(parentEventId: UUID): List<Event>

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
