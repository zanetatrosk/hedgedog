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
}
