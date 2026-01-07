package com.example.bedanceapp.model

import java.math.BigDecimal

data class EventDto(
    val id: String,
    val organizer: String,
    val eventName: String,
    val description: String? = null,
    val date: String,
    val time: String,
    val address: String?,
    val price: BigDecimal?,
    val currency: String? = null,
    val maxAttendees: Int? = null,
    val tags: List<String>? = null,
    val attendees: Int? = 0,
    val interested: Int? = 0,
    val promoMedia: EventMedia? = null
)
