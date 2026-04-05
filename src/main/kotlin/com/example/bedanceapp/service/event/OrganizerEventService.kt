package com.example.bedanceapp.service.event

import com.example.bedanceapp.controller.RegistrationStatus
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
    private val recurringEventService: RecurringEventService,
    private val eventAccessValidator: EventAccessValidator
) {

    @Transactional
    fun createEventByOccurrence(request: CreateUpdateEventDto, organizerId: UUID): List<Event> {
        return recurringEventService.createRecurringEvents(
            request = request,
            organizerId = organizerId,
            createEventFn = { req, orgId, date, parentId ->
                createEvent(req, orgId, date, parentId)
            }
        )
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
        if (registrationMode == RegistrationMode.GOOGLE_FORM && formId.isNullOrBlank()) {
            throw IllegalArgumentException("Form ID is required when registration mode is GOOGLE_FORM")
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

    @Transactional
    fun createEvent(request: CreateUpdateEventDto, organizerId: UUID, date: LocalDate? = null, parentId: UUID? = null): Event {
        val eventData = eventAssembler.buildEventFromRequest(request, organizerId, date, parentId)
        return eventRepository.save(eventData)
    }


    private fun handleCapacityRecalculation(existingEvent: Event, request: CreateUpdateEventDto) {
        val newMax = request.additionalDetails?.maxAttendees ?: return
        val eventId = existingEvent.id ?: return
        val confirmedCount = eventRegistrationRepository.findByEventId(eventId)
            .count { it.status == RegistrationStatus.REGISTERED }

        // 3. Validation: Don't let them shrink the bucket smaller than the people already inside
        require(newMax >= confirmedCount) {
            "Cannot lower capacity to $newMax. You already have $confirmedCount confirmed attendees. " +
                    "Please cancel some attendees before lowering the limit."
        }

        // 4. If the capacity actually changed, let the Manager handle the fallout
        if (existingEvent.maxAttendees != newMax) {
            registrationRecalculateService.recalculate(eventId, newMax)
        }
    }
}

