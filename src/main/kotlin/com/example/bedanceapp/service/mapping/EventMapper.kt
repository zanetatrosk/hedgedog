package com.example.bedanceapp.service.mapping

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.AttendeeStats
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventDetailAdditionalDetails
import com.example.bedanceapp.model.EventDetailBasicInfo
import com.example.bedanceapp.model.EventDetailDto
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventSummaryDto
import com.example.bedanceapp.model.RecurringDateInfo
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.RegistrationStats
import com.example.bedanceapp.model.SingleEventDTO
import com.example.bedanceapp.model.UserRegistrationStatus
import com.example.bedanceapp.model.toCodebook
import com.example.bedanceapp.model.toCodebookList
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.registration.EventRegistrationQueryService
import com.example.bedanceapp.service.MediaService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class EventMapper(
    private val mediaService: MediaService,
    private val eventRegistrationQueryService: EventRegistrationQueryService,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository
) {

    /**
     * Maps an Event entity to a lightweight EventSummaryDto (used for lists/search).
     */
    fun toDto(event: Event, userId: UUID?): EventSummaryDto {
        val eventId = event.id ?: throw IllegalStateException("Event ID cannot be null for mapping")
        val regCounts = eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId)
        val interestedCount = eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)
        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)

        return EventSummaryDto(
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
     * Maps an Event entity to the full EventDetailDto object.
     */
    fun toDetailData(event: Event, userId: UUID?, recurringDates: List<RecurringDateInfo>): EventDetailDto {
        val eventId = event.id ?: throw IllegalStateException("Event ID cannot be null")
        val regCounts = eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId)
        val interestedCount = eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)
        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)

        return EventDetailDto(
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
                danceStyles = event.danceStyles.toCodebookList(),
                skillLevel = event.skillLevels.toCodebookList(),
                typeOfEvent = event.typesOfEvents.toCodebookList(),
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
                    followers = regCounts.followers
                ),
                interested = interestedCount
            )
        )
    }

    fun toSingleEventDto(event: Event, registration: EventRegistration?): SingleEventDTO {
        val eventId = event.id ?: throw IllegalStateException("Event ID cannot be null")
        val stats = eventRegistrationQueryService.getRegistrationRolesCountsByEventId(eventId)
        val interestedCount = eventRegistrationQueryService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)

        return SingleEventDTO(
            id = eventId.toString(),
            eventName = event.eventName,
            organizer = event.toOrganizerDto(),
            status = event.status,
            userStatus = registration?.status,
            role = registration?.role?.toCodebook(),
            date = event.eventDate.toString(),
            time = event.eventTime.toString(),
            location = event.location.toLocationDto(),
            attendeeStats = AttendeeStats(
                going = RegistrationStats(stats.total, stats.leaders, stats.followers),
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
}



