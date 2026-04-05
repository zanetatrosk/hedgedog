package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.AttendeeStats
import com.example.bedanceapp.model.CodebookItem
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventDetailAdditionalDetails
import com.example.bedanceapp.model.EventDetailBasicInfo
import com.example.bedanceapp.model.EventDetailData
import com.example.bedanceapp.model.EventDto
import com.example.bedanceapp.model.Location
import com.example.bedanceapp.model.LocationRequest
import com.example.bedanceapp.model.OrganizerDto
import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.RegistrationStats
import com.example.bedanceapp.model.UserRegistrationStatus
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EventMapper(
    private val mediaService: MediaService,
    private val eventRegistrationQueryService: EventRegistrationQueryService,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {

    /**
     * Maps an Event entity to a lightweight EventDto (used for lists/search).
     */
    fun toDto(event: Event, userId: UUID?): EventDto {
        val eventId = event.id ?: throw IllegalStateException("Event ID cannot be null for mapping")
        val regCounts = eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId)
        val interestedCount = eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)
        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)

        return EventDto(
            id = eventId.toString(),
            organizer = event.toOrganizerDto(),
            eventName = event.eventName,
            description = event.description,
            date = event.eventDate.toString(),
            endDate = event.endDate?.toString(),
            time = event.eventTime.toString(),
            location = event.location?.toLocationDto(),
            price = event.price,
            currency = event.currency?.code,
            maxAttendees = event.maxAttendees,
            tags = event.extractTags(),
            attendees = regCounts.total,
            interested = interestedCount,
            promoMedia = mediaService.mapToDTO(event.promoMedia),
            registrationStatus = getUserStatus(eventId, userId),
            status = event.status,
            registrationType = settings?.registrationMode ?: RegistrationMode.OPEN,
            formId = settings?.formId
        )
    }

    /**
     * Maps an Event entity to the full EventDetailData object.
     */
    fun toDetailData(event: Event, userId: UUID?, recurringDates: List<RecurringDateInfo>): EventDetailData {
        val eventId = event.id ?: throw IllegalStateException("Event ID cannot be null")
        val regCounts = eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId)
        val interestedCount = eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)
        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)

        return EventDetailData(
            id = eventId.toString(),
            basicInfo = EventDetailBasicInfo(
                eventName = event.eventName,
                location = event.location?.toLocationDto(),
                date = event.eventDate.toString(),
                time = event.eventTime.toString(),
                price = event.price,
                currency = event.currency?.code,
                endDate = event.endDate?.toString(),
                recurringDates = recurringDates,
                organizer = event.toOrganizerDto(),
                status = event.status.name,
                registrationStatus = getUserStatus(eventId, userId),
                registrationType = settings?.registrationMode ?: RegistrationMode.OPEN,
                formId = settings?.formId
            ),
            additionalDetails = EventDetailAdditionalDetails(
                danceStyles = event.danceStyles.map { CodebookItem(it.id.toString(), it.name) },
                skillLevel = event.skillLevels.map { CodebookItem(it.id.toString(), it.name) },
                typeOfEvent = event.typesOfEvents.map { CodebookItem(it.id.toString(), it.name) },
                maxAttendees = event.maxAttendees
            ),
            description = event.description,
            coverImage = mediaService.mapToDTO(event.promoMedia),
            facebookEventUrl = event.facebookEventUrl,
            media = event.media.mapNotNull { mediaService.mapToDTO(it) },
            attendeeStats = AttendeeStats(
                going = RegistrationStats(
                    total = regCounts.total,
                    leaders = regCounts.leaders,
                    followers = regCounts.followers,
                    both = regCounts.both
                ),
                interested = interestedCount
            )
        )
    }

    private fun getUserStatus(eventId: UUID, userId: UUID?): UserRegistrationStatus? {
        return userId?.let {
            eventRegistrationQueryService.getLastRegistrationByEventIdAndUserId(eventId, it)?.let { reg ->
                UserRegistrationStatus(reg.id.toString(), reg.status)
            }
        }
    }

    private fun Event.toOrganizerDto() = OrganizerDto(
        userId = organizerId.toString(),
        firstName = organizer.profile?.firstName,
        lastName = organizer.profile?.lastName
    )

    private fun Location.toLocationDto() = LocationRequest(
        name = name,
        street = street,
        city = city,
        country = country,
        county = county,
        postalCode = postalCode,
        houseNumber = houseNumber,
        state = state
    )

    private fun Event.extractTags(): List<String> {
        return danceStyles.map { it.name } + skillLevels.map { it.name } + typesOfEvents.map { it.name }
    }
}


