package com.example.bedanceapp.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "displayMode"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SingleEventDTO::class, name = "SINGLE"),
    JsonSubTypes.Type(value = SeriesEventDTO::class, name = "SERIES")
)
sealed interface MyEvent {
    val id: String
    val eventName: String
    val organizer: OrganizerDto
}

data class SingleEventDTO(
    override val id: String,
    override val eventName: String,
    override val organizer: OrganizerDto,
    val userStatus: RsvpStatus? = null,
    val status: EventStatus,
    val date: String,
    val time: String,
    val location: LocationRequest?,
    val attendeeStats: AttendeeStats
) : MyEvent

data class SeriesEventDTO(
    override val id: String,
    override val eventName: String,
    override val organizer: OrganizerDto,
    val overallStartDate: String,
    val overallEndDate: String,
    val occurrences: List<SingleEventDTO>
) : MyEvent

enum class RsvpStatus {
    HOSTING,
    GOING,
    WAITLISTED,
    INTERESTED
}

enum class StatusFilter {
    JOINED,
    INTERESTED,
    HOSTING,
}

