package com.example.bedanceapp.repository

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.EventRegistration
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EventRegistrationRepository : JpaRepository<EventRegistration, UUID> {

    fun findByEventId(eventId: UUID): List<EventRegistration>

    fun findByUserId(userId: UUID): List<EventRegistration>

    fun findByEventIdAndUserId(eventId: UUID, userId: UUID): List<EventRegistration>

    fun findByEventIdAndResponseId(eventId: UUID, responseId: String): List<EventRegistration>

    fun findByEventIdAndStatus(eventId: UUID, status: RegistrationStatus): List<EventRegistration>

    fun findByUserIdAndStatus(eventId: UUID, status: RegistrationStatus, pageable: Pageable): List<EventRegistration>

    fun findByEventIdOrderByCreatedAt(eventId: UUID): List<EventRegistration>

    fun findByEventIdAndStatusNot(eventId: UUID, status: RegistrationStatus): List<EventRegistration>

    fun findByUserIdAndStatusNot(eventId: UUID, status: RegistrationStatus, pageable: Pageable): List<EventRegistration>

    fun findByIdAndEventId(id: UUID, eventId: UUID): EventRegistration?

    fun findByIdAndEventIdAndUserId(id: UUID, eventId: UUID, userId: UUID): EventRegistration?
}
