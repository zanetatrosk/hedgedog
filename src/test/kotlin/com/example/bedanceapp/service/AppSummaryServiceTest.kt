package com.example.bedanceapp.service

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.UserProfileRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("AppSummaryService Tests")
class AppSummaryServiceTest {

    @Mock private lateinit var userProfileRepository: UserProfileRepository
    @Mock private lateinit var eventRegistrationRepository: EventRegistrationRepository
    @Mock private lateinit var eventRepository: EventRepository

    private lateinit var service: AppSummaryService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = AppSummaryService(userProfileRepository, eventRegistrationRepository, eventRepository)
    }

    @Test
    fun `getAppSummary aggregates dancers registrations and events`() {
        whenever(userProfileRepository.count()).thenReturn(10)
        whenever(eventRegistrationRepository.count()).thenReturn(25)
        whenever(eventRepository.findByStatus(EventStatus.PUBLISHED)).thenReturn(listOf(createEvent(EventStatus.PUBLISHED), createEvent(EventStatus.PUBLISHED)))
        whenever(eventRepository.findByStatus(EventStatus.CANCELLED)).thenReturn(listOf(createEvent(EventStatus.CANCELLED)))

        val summary = service.getAppSummary()

        assertEquals(10, summary.totalDancers)
        assertEquals(25, summary.totalRegistrations)
        assertEquals(3, summary.totalEvents)
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
            maxAttendees = null,
            status = status,
            facebookEventUrl = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}

