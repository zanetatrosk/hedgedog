package com.example.bedanceapp.repository

import com.example.bedanceapp.model.EventRegistrationSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EventRegistrationSettingsRepository : JpaRepository<EventRegistrationSettings, UUID> {
    fun findByEventId(eventId: UUID): EventRegistrationSettings?
}

