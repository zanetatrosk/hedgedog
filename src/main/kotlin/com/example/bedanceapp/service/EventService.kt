package com.example.bedanceapp.service

import com.example.bedanceapp.model.*
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.DanceStyleRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import com.example.bedanceapp.repository.EventTypeRepository
import com.example.bedanceapp.repository.MediaRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.repository.CurrencyRepository
import com.example.bedanceapp.repository.EventParentRepository
import com.example.bedanceapp.repository.UserFavoriteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val danceStyleRepository: DanceStyleRepository,
    private val skillLevelRepository: SkillLevelRepository,
    private val eventTypeRepository: EventTypeRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository,
    private val mediaService: MediaService,
    private val currencyRepository: CurrencyRepository,
    private val locationService: LocationService,
    private val eventParentRepository: EventParentRepository,
    private val eventRegistrationService: EventRegistrationService,
    private val userFavoriteRepository: UserFavoriteRepository
) {

    fun getTags(event: Event): List<String> {
        val danceStyles = event.danceStyles.map { it.name }
        val skillLevels = event.skillLevels.map { it.name }
        val typesOfEvents = event.typesOfEvents.map { it.name }

        return danceStyles + skillLevels + typesOfEvents
    }

    @Transactional(readOnly = true)
    fun getAllPublishedEvents(): List<EventDto> {
        val events = eventRepository.findByStatus("published")
        return events.map { event ->
            val organizerName = if (event.organizer?.profile?.firstName != null && event.organizer?.profile?.lastName != null) {
                "${event.organizer?.profile?.firstName} ${event.organizer?.profile?.lastName}"
            } else {
                event.organizer?.username ?: "Unknown"
            }

            // Build address string from location
            val address = event.location?.let { loc ->
                buildString {
                    loc.street?.let { append("$it ") }
                    loc.houseNumber?.let { append("$it, ") }
                    append("${loc.city}, ")
                    append(loc.country)
                    loc.postalCode?.let { append(" $it") }
                }.trim()
            }

            val countEventRegistration = eventRegistrationService.getRegistrationCountsByEventId(event.id)
            val countInterested = event.id ?.let { userFavoriteRepository.countInterestedUsersByEventId(it) } ?: 0
            EventDto(
                id = event.id.toString(),
                organizer = organizerName,
                eventName = event.eventName,
                description = event.description,
                date = event.eventDate.toString(),
                time = event.eventTime.toString(),
                address = address,
                price = event.price,
                currency = event.currency?.code,
                maxAttendees = event.maxAttendees,
                tags = getTags(event),
                attendees = countEventRegistration.total,
                interested = countInterested,
                promoMedia = mediaService.mapToDTO(event.promoMedia)
            )
        }
    }
    @Transactional
    fun createEvent(request: CreateEventRequest, organizerId: UUID, date: LocalDate? = null, parentId: UUID? = null): Event {
        // Validate organizer exists
        userRepository.findById(organizerId)
            .orElseThrow { IllegalArgumentException("Organizer not found with id: $organizerId") }

        val eventData = buildEventFromRequest(request, organizerId, date, parentId)
        return eventRepository.save(eventData)
    }

    @Transactional
    fun updateEvent(eventId: UUID, request: CreateEventRequest, organizerId: UUID): Event {
        // Find existing event
        val existingEvent = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        // Validate organizer matches (only organizer can update their event)
        if (existingEvent.organizerId != organizerId) {
            throw IllegalArgumentException("Only the event organizer can update this event")
        }

        val eventData = buildEventFromRequest(
            request,
            organizerId,
            date = LocalDate.parse(request.basicInfo.date),
            parentId = existingEvent.parentEventId,
            existingEventId = eventId
        )

        return eventRepository.save(eventData)
    }

    private fun buildEventFromRequest(
        request: CreateEventRequest,
        organizerId: UUID,
        date: LocalDate? = null,
        parentId: UUID? = null,
        existingEventId: UUID? = null
    ): Event {
        // Parse date and time
        val eventDate = date ?: LocalDate.parse(request.basicInfo.date)
        val eventTime = LocalTime.parse(request.basicInfo.time)

        // Fetch currency
        val currency = if (request.basicInfo.currency != null) {
            currencyRepository.findByCode(request.basicInfo.currency)
                .orElseThrow { IllegalArgumentException("Currency not found with code: ${request.basicInfo.currency}") }
        } else {
            null
        }

        // Create or find location
        val location = if (request.basicInfo.location != null) {
            val locationRequest = request.basicInfo.location
            locationService.createLocation(
                name = locationRequest.name,
                street = locationRequest.street,
                city = locationRequest.city,
                country = locationRequest.country,
                postalCode = locationRequest.postalCode,
                houseNumber = locationRequest.houseNumber,
                state = locationRequest.state
            )
        } else {
            null
        }

        // Fetch dance styles
        val danceStyles = if (!request.additionalDetails?.danceStyles.isNullOrEmpty()) {
            danceStyleRepository.findAllById(request.additionalDetails.danceStyles)
        } else {
            emptyList()
        }

        // Fetch skill levels
        val skillLevels = if (!request.additionalDetails?.skillLevel.isNullOrEmpty()) {
            skillLevelRepository.findAllById(request.additionalDetails.skillLevel)
        } else {
            emptyList()
        }

        // Fetch event types
        val eventTypes = if (!request.additionalDetails?.typeOfEvent.isNullOrEmpty()) {
            eventTypeRepository.findAllById(request.additionalDetails.typeOfEvent)
        } else {
            emptyList()
        }

        val mediaList = request.media?.map { media ->
            mediaRepository.findById(media.id)
                .orElseThrow { IllegalArgumentException("Media not found with id: ${media.id}") }
        }

        // Build event (create new or update existing)
        return Event(
            id = existingEventId,
            parentEventId = parentId,
            organizerId = organizerId,
            eventName = request.basicInfo.eventName,
            description = request.description,
            eventDate = eventDate,
            eventTime = eventTime,
            locationId = location?.id,
            currency = currency,
            price = request.basicInfo.price,
            maxAttendees = request.additionalDetails?.maxAttendees,
            allowWaitlist = request.additionalDetails?.allowWaitlist ?: false,
            allowPartnerPairing = request.additionalDetails?.allowPartnerPairing ?: false,
            status = "draft", // Default status
            danceStyles = danceStyles,
            skillLevels = skillLevels,
            typesOfEvents = eventTypes,
            promoMediaId = request.coverImage?.id,
            media = mediaList?.toMutableList() ?: mutableListOf()
        )
    }

    fun generateDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(7)
            }
        return dates
    }
    @Transactional
    fun createEventByOccurance(request: CreateEventRequest, organizerId: UUID): List<Event> {
        // Validate organizer exists
        if( request.basicInfo.endDate == "" || request.basicInfo.endDate == null ) {
            return listOf(createEvent(request, organizerId))
        }
        val parent = eventParentRepository.save(EventParent(name = request.basicInfo.eventName))
        val dates = generateDates(LocalDate.parse(request.basicInfo.date), LocalDate.parse(request.basicInfo.endDate))
        val events = mutableListOf<Event>()
        for(date in dates) {
            val event = createEvent(request, organizerId, date, parent.id)
            events.add(event)
        }
        return events
    }

}
