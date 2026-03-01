package com.example.bedanceapp.model

import java.math.BigDecimal
import java.util.UUID

data class CreateEventRequest(
    val basicInfo: BasicInfoRequest,
    val additionalDetails: AdditionalDetailsRequest?,
    val description: String?,
    val coverImage: EventMedia?,
    val media: List<EventMedia>?
)

data class BasicInfoRequest(
    val eventName: String,
    val location: LocationRequest?,  // New structured location
    val date: String,      // ISO date format: "2024-01-15"
    val time: String,      // Format: "18:00"
    val endDate: String?,
    val isRecurring: Boolean?,
    val recurrenceType: RecurrenceType?,
    val recurrenceEndDate: String?,  // ISO date format for when recurring events should end
    val price: BigDecimal?,
    val currency: String?
)

data class LocationRequest(
    val name: String,
    val street: String?,
    val city: String,
    val country: String,
    val postalCode: String?,
    val houseNumber: String?,
    val state: String?,
    val county: String?
)

data class AdditionalDetailsRequest(
    val danceStyles: List<UUID>,      // List of dance style IDs
    val skillLevel: List<UUID>,       // List of skill level IDs
    val typeOfEvent: List<UUID>,      // List of event type IDs
    val maxAttendees: Int?,
    val facebookEventUrl: String?     // Facebook event URL
    // Note: registrationMode, formId, allowWaitlist, allowPartnerPairing, and requireApproval
    // can only be set when publishing the event via PATCH /api/events/{id}/publish
)

data class EventMedia(
    val type: String,  // "image" or "video"
    val url: String,
    val id: UUID
)

data class CreateEventResponse(
    val events: List<UUID?>,
    val message: String
)

enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY
}

