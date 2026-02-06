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
import com.example.bedanceapp.specification.EventSpecification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
) {

    fun getTags(event: Event): List<String> {
        val danceStyles = event.danceStyles.map { it.name }
        val skillLevels = event.skillLevels.map { it.name }
        val typesOfEvents = event.typesOfEvents.map { it.name }

        return danceStyles + skillLevels + typesOfEvents
    }

    @Transactional(readOnly = true)
    fun getAllPublishedEventsPaginated(
        userId: UUID? = null,
        pageable: Pageable,
        eventName: String? = null,
        city: String? = null,
        country: String? = null,
        danceStyleIds: List<UUID>? = null,
        eventTypeIds: List<UUID>? = null,
        includeCancelled: Boolean = true
    ): Page<EventDto> {
        val specification = EventSpecification.buildSpecificationForPublicEvents(
            includeCancelled = includeCancelled,
            eventName = eventName,
            city = city,
            country = country,
            danceStyleIds = danceStyleIds,
            eventTypeIds = eventTypeIds
        )
        val eventsPage = eventRepository.findAll(specification, pageable)
        return eventsPage.map { event ->
            mapEventToDto(event, userId)
        }
    }

    private fun mapEventToDto(event: Event, userId: UUID?): EventDto {
        val organizer = OrganizerDto(event.organizerId.toString(), event.organizer?.profile?.firstName, event.organizer?.profile?.lastName)
        // Build address string from location
        val eventId = event.id
        val countEventRegistration = eventRegistrationService.getRegistrationRolesCountsByEventId(eventId, "going")
        val countInterested = eventRegistrationService.getRegistrationCountByEventId(eventId, "interested")
        val registrationStatus = if(userId != null && eventId != null) {
            eventRepository.findUserRegistrationStatus(eventId, userId)
        } else {
            null
        }
        return EventDto(
            id = eventId.toString(),
            organizer = organizer,
            eventName = event.eventName,
            description = event.description,
            date = event.eventDate.toString(),
            time = event.eventTime.toString(),
            location = locationToDto(event.location),
            price = event.price,
            currency = event.currency?.code,
            maxAttendees = event.maxAttendees,
            tags = getTags(event),
            attendees = countEventRegistration.total,
            interested = countInterested,
            promoMedia = mediaService.mapToDTO(event.promoMedia),
            registrationStatus = registrationStatus,
            status = event.status,
            registrationType = event.registrationMode,
            formId = event.formId
        )
    }

    @Transactional(readOnly = true)
    fun getEventDetailById(eventId: UUID, userId: UUID? = null): EventDetailData {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        val statusUser = eventRegistrationService.getLastRegistrationByEventIdAndUserId(eventId, userId)

        // Get registration stats
        val registrationCount = eventRegistrationService.getRegistrationRolesCountsByEventId(event.id, "going")
        val interestedCount = eventRegistrationService.getRegistrationCountByEventId(eventId, "interested")
        // Handle recurring dates if this event has a parent
        val parentEventId = event.parentEventId
        val recurringDates = getUpcomingDates(parentEventId)

        // Get end date for recurring events
        val endDate = if (recurringDates.isNotEmpty()) {
            recurringDates.maxByOrNull { it.date }?.date
        } else {
            null
        }

        return EventDetailData(
            id = event.id.toString(),
            basicInfo = EventDetailBasicInfo(
                eventName = event.eventName,
                location = locationToDto(event.location),
                date = event.eventDate.toString(),
                time = event.eventTime.toString(),
                price = event.price,
                currency = event.currency?.code,
                endDate = endDate,
                recurringDates = recurringDates,
                organizer = OrganizerDto(event.organizerId.toString(), event.organizer?.profile?.firstName, event.organizer?.profile?.lastName),
                status = event.status.name,
                statusUser = statusUser?.status,
                registrationType = event.registrationMode,
                formId = event.formId
            ),
            additionalDetails = EventDetailAdditionalDetails(
                danceStyles = event.danceStyles.map { CodebookItem(it.id.toString(), it.name) },
                skillLevel = event.skillLevels.map { CodebookItem(it.id.toString(), it.name) },
                typeOfEvent = event.typesOfEvents.map { CodebookItem(it.id.toString(), it.name) },
                maxAttendees = event.maxAttendees,
                allowWaitlist = event.allowWaitlist,
                allowPartnerPairing = event.allowPartnerPairing
            ),
            description = event.description,
            coverImage = mediaService.mapToDTO(event.promoMedia),
            facebookEventUrl = null, // TODO: Add to Event model when needed
            media = event.media.mapNotNull { mediaService.mapToDTO(it) },
            attendeeStats = AttendeeStats(
                going = RegistrationStats(
                    total = registrationCount.total,
                    leaders = registrationCount.leaders,
                    followers = registrationCount.followers
                ),
                interested = interestedCount
            )
        )
    }

    private fun locationToDto(location: Location?): LocationRequest? {
        return if (location != null) {
            LocationRequest(
                name = location.name,
                street = location.street,
                city = location.city,
                country = location.country,
                county = location.county,
                postalCode = location.postalCode,
                houseNumber = location.houseNumber,
                state = location.state
                )
        } else {
            null
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
            status = existingEvent.status,
            existingEventId = eventId
        )

        return eventRepository.save(eventData)
    }

    private fun buildEventFromRequest(
        request: CreateEventRequest,
        organizerId: UUID,
        date: LocalDate? = null,
        parentId: UUID? = null,
        status: EventStatus? = null,
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
                state = locationRequest.state,
                county = locationRequest.county
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
        // Note: registrationMode, formId, allowWaitlist, and requireApproval are set to defaults
        // These can only be changed when publishing the event
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
            requireApproval = false,  // Default: cannot be set via POST/PUT
            status = status ?: EventStatus.DRAFT,
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

    fun getUpcomingDates(parentEventId: UUID?): List<RecurringDateInfo> {
        if (parentEventId == null) {
            return emptyList()
        }
        val siblingEvents = eventRepository.findByParentEventId(parentEventId)
        return siblingEvents.map { sibling ->
            RecurringDateInfo(
                date = sibling.eventDate.toString(),
                id = sibling.id.toString()
            )
        }.sortedBy { it.date }

    }
    @Transactional
    fun createEventByOccurrence(request: CreateEventRequest, organizerId: UUID): List<Event> {
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

    @Transactional
    fun publishEvent(eventId: UUID, organizerId: UUID, publishRequest: PublishEventRequest? = null): Event {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        // Validate organizer
        if (event.organizerId != organizerId) {
            throw IllegalArgumentException("Only the event organizer can publish this event")
        }

        // Validate current status
        if (event.status != EventStatus.DRAFT) {
            throw IllegalArgumentException("Only draft events can be published. Current status: ${event.status}")
        }

        // Parse and validate registration mode if provided
        val registrationMode = publishRequest?.registrationMode?.let {
            try {
                RegistrationMode.valueOf(it.name)
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid registration mode: ${it.name}")
            }
        } ?: event.registrationMode

        // Validate formId is provided when registrationMode is GOOGLE_FORM
        val formId = publishRequest?.formId ?: event.formId
        if (registrationMode == RegistrationMode.GOOGLE_FORM && formId.isNullOrBlank()) {
            throw IllegalArgumentException("Form ID is required when registration mode is GOOGLE_FORM")
        }

        val requireApproval = publishRequest?.requireApproval ?: event.requireApproval
        val allowWaitlist = publishRequest?.allowWaitlist ?: event.allowWaitlist

        // Update status to published with new registration settings
        val updatedEvent = event.copy(
            status = EventStatus.PUBLISHED,
            registrationMode = registrationMode,
            formId = formId,
            requireApproval = requireApproval,
            allowWaitlist = allowWaitlist,
            updatedAt = java.time.LocalDateTime.now()
        )
        return eventRepository.save(updatedEvent)
    }

    @Transactional
    fun cancelEvent(eventId: UUID, organizerId: UUID): Event {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        // Validate organizer
        if (event.organizerId != organizerId) {
            throw IllegalArgumentException("Only the event organizer can cancel this event")
        }

        // Validate current status
        if (event.status != EventStatus.PUBLISHED) {
            throw IllegalArgumentException("Only published events can be cancelled. Current status: ${event.status}")
        }

        // Update status to cancelled (soft delete)
        val updatedEvent = event.copy(status = EventStatus.CANCELLED, updatedAt = java.time.LocalDateTime.now())
        return eventRepository.save(updatedEvent)
    }

    @Transactional
    fun deleteEvent(eventId: UUID, organizerId: UUID) {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        // Validate organizer
        if (event.organizerId != organizerId) {
            throw IllegalArgumentException("Only the event organizer can delete this event")
        }

        // Validate current status - only drafts can be hard deleted
        if (event.status != EventStatus.DRAFT) {
            throw IllegalArgumentException("Only draft events can be deleted. Published events must be cancelled instead. Current status: ${event.status}")
        }

        // Hard delete
        eventRepository.delete(event)
    }
}
