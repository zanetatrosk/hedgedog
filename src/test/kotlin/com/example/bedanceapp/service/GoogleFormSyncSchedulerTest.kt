package com.example.bedanceapp.service

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventRegistrationSettings
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.service.registration.GoogleFormSyncService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@DisplayName("GoogleFormSyncScheduler Tests")
class GoogleFormSyncSchedulerTest {

    @Mock private lateinit var settingsRepository: EventRegistrationSettingsRepository
    @Mock private lateinit var googleFormSyncService: GoogleFormSyncService

    private lateinit var scheduler: GoogleFormSyncScheduler

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        scheduler = GoogleFormSyncScheduler(settingsRepository, googleFormSyncService)
    }

    @Test
    fun `syncAllGoogleForms syncs only published google-form events with form id`() {
        val publishedEvent = createEvent(EventStatus.PUBLISHED)
        val cancelledEvent = createEvent(EventStatus.CANCELLED)

        val valid = EventRegistrationSettings(eventId = publishedEvent.id!!, event = publishedEvent, registrationMode = RegistrationMode.GOOGLE_FORM, formId = "form1")
        val noFormId = EventRegistrationSettings(eventId = UUID.randomUUID(), event = publishedEvent, registrationMode = RegistrationMode.GOOGLE_FORM, formId = null)
        val wrongMode = EventRegistrationSettings(eventId = UUID.randomUUID(), event = publishedEvent, registrationMode = RegistrationMode.OPEN, formId = "x")
        val cancelled = EventRegistrationSettings(eventId = cancelledEvent.id!!, event = cancelledEvent, registrationMode = RegistrationMode.GOOGLE_FORM, formId = "form2")

        whenever(settingsRepository.findAll()).thenReturn(listOf(valid, noFormId, wrongMode, cancelled))

        scheduler.syncAllGoogleForms()

        verify(googleFormSyncService).syncRegistrationData(publishedEvent.id!!, publishedEvent.organizerId, publishedEvent.maxAttendees)
        verify(googleFormSyncService, never()).syncRegistrationData(cancelledEvent.id!!, cancelledEvent.organizerId, cancelledEvent.maxAttendees)
    }

    @Test
    fun `syncAllGoogleForms continues when single event sync fails`() {
        val event = createEvent(EventStatus.PUBLISHED)
        val settings = EventRegistrationSettings(eventId = event.id!!, event = event, registrationMode = RegistrationMode.GOOGLE_FORM, formId = "form1")
        whenever(settingsRepository.findAll()).thenReturn(listOf(settings))
        whenever(googleFormSyncService.syncRegistrationData(any(), any(), any())).thenThrow(RuntimeException("timeout"))

        scheduler.syncAllGoogleForms()

        verify(googleFormSyncService).syncRegistrationData(event.id!!, event.organizerId, event.maxAttendees)
    }

    private fun createEvent(status: EventStatus): Event {
        val organizerId = UUID.randomUUID()
        return Event(
            id = UUID.randomUUID(),
            organizerId = organizerId,
            organizer = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "p1"),
            eventName = "Event",
            description = null,
            eventDate = LocalDate.now().plusDays(1),
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = null,
            maxAttendees = 20,
            status = status,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}

