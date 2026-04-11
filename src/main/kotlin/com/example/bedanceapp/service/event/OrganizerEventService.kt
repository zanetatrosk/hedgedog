package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.model.CreateUpdateEventDto
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.PublishEventDto
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.service.EventAccessValidator
import com.example.bedanceapp.service.registration.OrganizerRegistrationService
import com.example.bedanceapp.service.registration.RegistrationRecalculateService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class OrganizerEventService(
    private val eventRepository: EventRepository,
    private val eventAssembler: EventAssembler,
    private val organizerRegistrationService: OrganizerRegistrationService,
    private val registrationRecalculateService: RegistrationRecalculateService,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val createEventService: CreateEventService,
    private val eventAccessValidator: EventAccessValidator
) {

    @Transactional
    fun createEventByOccurrence(request: CreateUpdateEventDto, organizerId: UUID): List<Event> {
        val events =  createEventService.createSingleEventOrRecurringEvents(
            request = request,
            organizerId = organizerId,
        )
        eventRepository.saveAll(events)
        return events;
    }

    @Transactional
    fun updateEvent(eventId: UUID, request: CreateUpdateEventDto, organizerId: UUID): Event {
        val existingEvent = eventAccessValidator.requireOwnedEvent(eventId, organizerId)
        handleCapacityRecalculation(existingEvent, request)

        val eventData = eventAssembler.buildEventFromRequest(
            request,
            organizerId,
            date = request.basicInfo.date,
            parentId = existingEvent.parentEventId,
            status = existingEvent.status,
            existingEventId = eventId
        )

        return eventRepository.save(eventData)
    }

    @Transactional
    fun publishEvent(eventId: UUID, organizerId: UUID, publishRequest: PublishEventDto? = null): Event {
        val event = eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)

        val registrationMode = publishRequest?.registrationMode ?: RegistrationMode.OPEN
        val formId = publishRequest?.formId
        require(!event.eventDate.isBefore(LocalDate.now())) {
            "Event date must be today or in the future."
        }
        require(registrationMode != RegistrationMode.GOOGLE_FORM || formId != null) {
            "Form ID must be provided when using GOOGLE_FORM registration mode."
        }

        require(registrationMode != RegistrationMode.COUPLE || event.maxAttendees?.rem(2) == 0) {
            "You cannot have odd number of capacity for couple event, please change capacity to even number"
        }

        val registrationSettings = EventRegistrationSettings(
            eventId = eventId,
            registrationMode = registrationMode,
            formId = formId,
            formStructure = null,
            requireApproval = publishRequest?.requireApproval ?: false
        )
        eventRegistrationSettingsRepository.save(registrationSettings)

        if (registrationMode == RegistrationMode.GOOGLE_FORM) {
            try {
                organizerRegistrationService.syncGoogleFormData(eventId, organizerId)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to fetch form structure from Google Forms: ${e.message}")
            }
        }

        return eventRepository.save(
            event.copy(
                status = EventStatus.PUBLISHED,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    @Transactional
    fun cancelEvent(eventId: UUID, organizerId: UUID): Event {
        val event = eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT, EventStatus.PUBLISHED)

        return eventRepository.save(
            event.copy(
                status = EventStatus.CANCELLED,
                updatedAt = LocalDateTime.now()
            )
        )
    }

    @Transactional
    fun deleteEvent(eventId: UUID, organizerId: UUID) {
        val event = eventAccessValidator.requireOwnedEvent(eventId, organizerId, EventStatus.DRAFT)
        eventRepository.delete(event)
    }

    private fun handleCapacityRecalculation(existingEvent: Event, request: CreateUpdateEventDto) {
        val newMax = request.additionalDetails?.maxAttendees ?: return
        val eventId = existingEvent.id ?: return
        val confirmedCount = eventRegistrationRepository.findByEventId(eventId)
            .count { it.status == RegistrationStatus.REGISTERED }
        val settings = eventRegistrationSettingsRepository.findByEventId(eventId)

        // 3. Validation: Don't let them shrink the bucket smaller than the people already inside
        require(newMax >= confirmedCount) {
            "Cannot lower capacity to $newMax. You already have $confirmedCount confirmed attendees. " +
                    "Please cancel some attendees before lowering the limit."
        }

        require(settings?.registrationMode == RegistrationMode.COUPLE || newMax % 2 == 0) {
            "You cannot have odd capacity for couple event"
        }

        // 4. If the capacity actually changed, let the Manager handle the fallout
        if (existingEvent.maxAttendees != newMax) {
            registrationRecalculateService.recalculate(eventId, newMax)
        }
    }
}

