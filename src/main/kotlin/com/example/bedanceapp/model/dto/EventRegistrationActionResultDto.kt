package com.example.bedanceapp.model

import java.time.LocalDateTime
import java.util.UUID

data class EventRegistrationActionResultDto(
    val registrationId: UUID,
    val eventId: UUID,
    val userId: UUID?,
    val status: RegistrationStatus,
    val roleId: UUID?,
    val waitlistedAt: LocalDateTime?,
    val updatedAt: LocalDateTime
)

