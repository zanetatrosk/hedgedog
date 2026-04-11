package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.CreateUpdateEventDto
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.repository.CurrencyRepository
import com.example.bedanceapp.repository.DanceStyleRepository
import com.example.bedanceapp.repository.EventTypeRepository
import com.example.bedanceapp.repository.MediaRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.LocationService
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class EventAssembler(
    private val userRepository: UserRepository,
    private val currencyRepository: CurrencyRepository,
    private val locationService: LocationService,
    private val danceStyleRepository: DanceStyleRepository,
    private val skillLevelRepository: SkillLevelRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val mediaRepository: MediaRepository
) {
    fun buildEventFromRequest(
        request: CreateUpdateEventDto,
        organizerId: UUID,
        date: LocalDate? = null,
        parentId: UUID? = null,
        status: EventStatus? = null,
        existingEventId: UUID? = null
    ): Event {
        val eventDate = date ?: request.basicInfo.date
        val eventTime = request.basicInfo.time
        val endDate = request.basicInfo.endDate

        validateNewOrUpdatedEvent(request, eventDate)
        val currency = request.basicInfo.currency?.let { code ->
            currencyRepository.findByCode(code)
                .orElseThrow { IllegalArgumentException("Currency not found: $code") }
        }

        val location = request.basicInfo.location?.let { loc ->
            locationService.createLocation(
                name = loc.name, street = loc.street, city = loc.city,
                country = loc.country, postalCode = loc.postalCode,
                houseNumber = loc.houseNumber, state = loc.state, county = loc.county
            )
        }

        val danceStyles = request.additionalDetails?.danceStyles?.let { danceStyleRepository.findAllById(it) } ?: emptyList()
        val skillLevels = request.additionalDetails?.skillLevel?.let { skillLevelRepository.findAllById(it) } ?: emptyList()
        val eventTypes = request.additionalDetails?.typeOfEvent?.let { eventTypeRepository.findAllById(it) } ?: emptyList()

        val mediaList = request.media?.map { media ->
            mediaRepository.findById(media.id)
                .orElseThrow { IllegalArgumentException("Media not found: ${media.id}") }
        }

        val organizer = userRepository.findById(organizerId)
            .orElseThrow { IllegalArgumentException("Organizer not found: $organizerId") }

        return Event(
            id = existingEventId,
            parentEventId = parentId,
            organizerId = organizerId,
            eventName = request.basicInfo.eventName,
            description = request.description,
            eventDate = eventDate,
            eventTime = eventTime,
            endDate = endDate,
            locationId = location?.id,
            currency = currency,
            price = request.basicInfo.price,
            maxAttendees = request.additionalDetails?.maxAttendees,
            facebookEventUrl = request.additionalDetails?.facebookEventUrl,
            status = status ?: EventStatus.DRAFT,
            danceStyles = danceStyles,
            skillLevels = skillLevels,
            typesOfEvents = eventTypes,
            promoMediaId = request.coverImage?.id,
            media = mediaList?.toMutableList() ?: mutableListOf(),
            organizer = organizer
        )
    }

    private fun validateNewOrUpdatedEvent(request: CreateUpdateEventDto, startDate: LocalDate){
        require(request.basicInfo.endDate?.isAfter(startDate) ?: true) {
            "End date must be after start date"
        }

        require(request.basicInfo.price == null && request.basicInfo.currency == null
                || request.basicInfo.price != null && request.basicInfo.currency != null) {
            "Price and currency must be provided together"
        }

    }
}
