package com.example.bedanceapp.model

import java.math.BigDecimal

data class EventDto(
    val id: String,
    val organizer: OrganizerDto,
    val eventName: String,
    val description: String? = null,
    val date: String,
    val endDate: String? = null,
    val time: String,
    val location: LocationRequest?,
    val price: BigDecimal?,
    val currency: String? = null,
    val maxAttendees: Int? = null,
    val tags: List<String>? = null,
    val attendees: Int? = 0,
    val interested: Int? = 0,
    val promoMedia: EventMedia? = null,
    val registrationStatus: String? = null,
    val status: EventStatus? = null,
    val registrationType: RegistrationMode,
    val formId: String? = null,
)