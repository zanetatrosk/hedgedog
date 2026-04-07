package com.example.bedanceapp.model
import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.dto.OrganizerDto

sealed interface MyEvent {
    val id: String
    val eventName: String
    val organizer: OrganizerDto
    val displayMode: MyEventDisplayMode
}

enum class MyEventDisplayMode {
    SERIES,
    SINGLE
}

data class SingleEventDTO(
    override val id: String,
    override val eventName: String,
    override val organizer: OrganizerDto,
    override val displayMode: MyEventDisplayMode = MyEventDisplayMode.SINGLE,
    val userStatus: RegistrationStatus? = null,
    val status: EventStatus,
    val role: CodebookItem? = null,
    val date: String,
    val time: String,
    val location: LocationRequest?,
    val attendeeStats: AttendeeStats
) : MyEvent

data class SeriesEventDto(
    override val id: String,
    override val eventName: String,
    override val organizer: OrganizerDto,
    override val displayMode: MyEventDisplayMode = MyEventDisplayMode.SERIES,
    val overallStartDate: String,
    val overallEndDate: String,
    val occurrences: List<SingleEventDTO>
) : MyEvent

enum class EventTimeline {
    UPCOMING,
    PAST,
}

enum class StatusFilter {
    JOINED,
    INTERESTED,
    HOSTING,
}

