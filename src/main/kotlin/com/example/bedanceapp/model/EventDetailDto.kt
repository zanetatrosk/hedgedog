package com.example.bedanceapp.model

import java.math.BigDecimal

data class EventDetailData(
    val id: String,
    val basicInfo: EventDetailBasicInfo,
    val additionalDetails: EventDetailAdditionalDetails,
    val description: String?,
    val coverImage: EventMedia?,
    val facebookEventUrl: String? = null,
    val media: List<EventMedia>?,
    val attendeeStats: AttendeeStats
)

data class EventDetailBasicInfo(
    val eventName: String,
    val address: String?,
    val date: String,
    val time: String,
    val price: BigDecimal?,
    val currency: String?,
    val endDate: String?,
    val organizer: OrganizerDto,
    val recurringDates: List<RecurringDateInfo>
)

data class RecurringDateInfo(
    val date: String,
    val id: String,
    val status: String,  // "Past", "Scheduled", "Cancelled"
    val statusUser: String?  // "Joined", "Interested", null
)

data class EventDetailAdditionalDetails(
    val danceStyles: List<String>,
    val skillLevel: List<String>,
    val typeOfEvent: List<String>,
    val maxAttendees: Int?,
    val allowWaitlist: Boolean,
    val allowPartnerPairing: Boolean
)

data class AttendeeStats(
    val going: RegistrationStats,
    val interested: Int
)

data class RegistrationStats(
    val total: Int,
    val leaders: Int,
    val followers: Int
)

