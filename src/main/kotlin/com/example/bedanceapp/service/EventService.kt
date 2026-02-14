package com.example.bedanceapp.service

import com.example.bedanceapp.controller.RegistrationStatus
import com.example.bedanceapp.model.*
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.DanceStyleRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import com.example.bedanceapp.repository.EventTypeRepository
import com.example.bedanceapp.repository.MediaRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.repository.CurrencyRepository
import com.example.bedanceapp.repository.EventParentRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.registration.GoogleFormRegistrationStrategy
import com.example.bedanceapp.specification.EventSpecification
import com.google.api.client.json.gson.GsonFactory
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
    private val eventRegistrationStatsService: EventRegistrationStatsService,
    private val eventRegistrationManager: EventRegistrationManager,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    private val googleFormRegistrationStrategy: GoogleFormRegistrationStrategy,
    private val eventRegistrationRepository: EventRegistrationRepository
) {

    private val jsonFactory = GsonFactory.getDefaultInstance()

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
        val registrations = eventId?.let { eventRegistrationRepository.findByEventIdAndStatus(it, RegistrationStatus.GOING) }
        val countEventRegistration = eventRegistrationManager.getRegistrationRolesCountsByEventId(registrations?: emptyList())
        val countInterested = eventRegistrationStatsService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)
        val registrationStatus = if(userId != null && eventId != null) {
            eventRepository.findUserRegistrationStatus(eventId, userId)
        } else {
            null
        }

        // Fetch registration settings (optional)
        val registrationSettings = eventId?.let { eventRegistrationSettingsRepository.findByEventId(it) }

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
            registrationType = registrationSettings?.registrationMode ?: RegistrationMode.OPEN,
            formId = registrationSettings?.formId
        )
    }

    @Transactional(readOnly = true)
    fun getEventDetailById(eventId: UUID, userId: UUID? = null): EventDetailData {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found with id: $eventId") }

        val statusUser = eventRegistrationManager.getLastRegistrationByEventIdAndUserId(eventId, userId)
        val registrations = eventRegistrationRepository.findByEventIdAndStatus(eventId, RegistrationStatus.GOING)
        // Get registration stats
        val registrationCount = eventRegistrationManager.getRegistrationRolesCountsByEventId(registrations)
        val interestedCount = eventRegistrationStatsService.getRegistrationCountByEventId(eventId, RegistrationStatus.INTERESTED)
        // Handle recurring dates if this event has a parent
        val parentEventId = event.parentEventId
        val recurringDates = getUpcomingDates(parentEventId)

        // Get end date for recurring events
        val endDate = if (recurringDates.isNotEmpty()) {
            recurringDates.maxByOrNull { it.date }?.date
        } else {
            null
        }

        // Fetch registration settings (optional)
        val registrationSettings = eventRegistrationSettingsRepository.findByEventId(eventId)

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
                registrationType = registrationSettings?.registrationMode ?: RegistrationMode.OPEN,
                formId = registrationSettings?.formId
            ),
            additionalDetails = EventDetailAdditionalDetails(
                danceStyles = event.danceStyles.map { CodebookItem(it.id.toString(), it.name) },
                skillLevel = event.skillLevels.map { CodebookItem(it.id.toString(), it.name) },
                typeOfEvent = event.typesOfEvents.map { CodebookItem(it.id.toString(), it.name) },
                maxAttendees = event.maxAttendees,
                allowWaitlist = registrationSettings?.allowWaitlist ?: false,
                allowPartnerPairing = registrationSettings?.allowPartnerPairing ?: false
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

        val registrations = eventRegistrationRepository.findByEventId(eventId)
        val activeRegistrations = eventRegistrationManager.getActiveRegistrations(registrations)

        request.additionalDetails?.maxAttendees.let {
            maxAttendees ->
            if (maxAttendees != null) {
                if(maxAttendees < activeRegistrations.size) {
                    throw IllegalArgumentException("Cannot lower capacity below current attendee count")
                }
                if(existingEvent.maxAttendees != null && existingEvent.maxAttendees != maxAttendees){
                    eventRegistrationManager.recalculateRegistrations(eventId, maxAttendees, registrations)
                }

            }
        }



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

        val organizer = userRepository.findById(organizerId)

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
            status = status ?: EventStatus.DRAFT,
            danceStyles = danceStyles,
            skillLevels = skillLevels,
            typesOfEvents = eventTypes,
            promoMediaId = request.coverImage?.id,
            media = mediaList?.toMutableList() ?: mutableListOf(),
            organizer = organizer.get()
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

        // Get organizer user
        val organizer = userRepository.findById(organizerId)
            .orElseThrow { IllegalArgumentException("Organizer not found with id: $organizerId") }

        // Parse and validate registration mode if provided
        val registrationMode = publishRequest?.registrationMode ?: RegistrationMode.OPEN

        // Validate formId is provided when registrationMode is GOOGLE_FORM
        val formId = publishRequest?.formId
        if (registrationMode == RegistrationMode.GOOGLE_FORM && formId.isNullOrBlank()) {
            throw IllegalArgumentException("Form ID is required when registration mode is GOOGLE_FORM")
        }

        val requireApproval = publishRequest?.requireApproval ?: false
        val allowWaitlist = publishRequest?.allowWaitlist ?: false
        val allowPartnerPairing = publishRequest?.allowPartnerPairing ?: false

        // Create or update registration settings
        val registrationSettings = EventRegistrationSettings(
            eventId = eventId,
            registrationMode = registrationMode,
            formId = formId,
            formStructure = null,
            allowWaitlist = allowWaitlist,
            allowPartnerPairing = allowPartnerPairing,
            requireApproval = requireApproval
        )
        eventRegistrationSettingsRepository.save(registrationSettings)

        // Fetch form structure from Google Forms API if using GOOGLE_FORM mode
        if (registrationMode == RegistrationMode.GOOGLE_FORM) {
            try {
                eventRegistrationManager.syncGoogleFormData(eventId)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to fetch form structure from Google Forms: ${e.message}")
            }
        }

        // Update event status to published
        val updatedEvent = event.copy(
            status = EventStatus.PUBLISHED,
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
