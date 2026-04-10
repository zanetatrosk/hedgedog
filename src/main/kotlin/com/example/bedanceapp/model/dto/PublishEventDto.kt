package com.example.bedanceapp.model

data class PublishEventDto(
    val registrationMode: RegistrationMode,
    val formId: String?,
    val requireApproval: Boolean
)

