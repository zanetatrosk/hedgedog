package com.example.bedanceapp.model

data class PublishEventRequest(
    val registrationMode: RegistrationMode,
    val formId: String?,
    val allowWaitlist: Boolean?,
    val allowPartnerPairing: Boolean?,
    val requireApproval: Boolean
)

