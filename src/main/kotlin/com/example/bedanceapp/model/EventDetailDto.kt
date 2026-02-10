package com.example.bedanceapp.model

import com.example.bedanceapp.controller.RegistrationStatus
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
    val location: LocationRequest?,
    val date: String,
    val time: String,
    val price: BigDecimal?,
    val currency: String?,
    val endDate: String?,
    val organizer: OrganizerDto,
    val recurringDates: List<RecurringDateInfo>,
    val status: String,  // "Past", "Scheduled", "Cancelled"
    val statusUser: RegistrationStatus?,  // "Joined", "Interested", null
    val registrationType: RegistrationMode,
    val formId: String? = null,
)

data class RecurringDateInfo(
    val date: String,
    val id: String,
)

data class EventDetailAdditionalDetails(
    val danceStyles: List<CodebookItem>,
    val skillLevel: List<CodebookItem>,
    val typeOfEvent: List<CodebookItem>,
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

data class CodebookItem (
    val id: String,
    val name: String,
)


