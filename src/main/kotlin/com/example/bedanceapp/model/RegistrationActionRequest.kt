package com.example.bedanceapp.model

import java.util.UUID

data class RegistrationActionRequest(
    val action: RegistrationAction,
    val registrations: List<UUID>
)


