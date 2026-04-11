package com.example.bedanceapp.service.mapping

import com.example.bedanceapp.model.DanceStyle
import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.EventType
import com.example.bedanceapp.model.Location
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("EventMappingExtensions Tests")
class EventMappingExtensionsTest {

    @Test
    fun `toOrganizerDto maps organizer basic data`() {
        val organizerId = UUID.randomUUID()
        val organizer = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "p1")
        val event = createEvent(organizer = organizer, organizerId = organizerId)

        val dto = event.toOrganizerDto()

        assertEquals(organizerId.toString(), dto.userId)
        assertNull(dto.firstName)
        assertNull(dto.lastName)
    }

    @Test
    fun `toLocationDto maps nullable location correctly`() {
        val location = Location(
            id = UUID.randomUUID(),
            name = "Studio",
            street = "Main",
            city = "Warsaw",
            country = "Poland",
            postalCode = "00-001",
            houseNumber = "10",
            state = null,
            county = "Mazowieckie"
        )

        val dto = location.toLocationDto()

        assertEquals("Studio", dto?.name)
        assertEquals("Warsaw", dto?.city)
        assertEquals("Poland", dto?.country)
        assertEquals("Mazowieckie", dto?.county)
        assertNull((null as Location?).toLocationDto())
    }

    @Test
    fun `extractTags returns styles levels and types in order`() {
        val event = createEvent(
            danceStyles = listOf(DanceStyle(id = UUID.randomUUID(), name = "Bachata")),
            skillLevels = listOf(SkillLevel(id = UUID.randomUUID(), name = "Beginner", levelOrder = 1)),
            eventTypes = listOf(EventType(id = UUID.randomUUID(), name = "Workshop"))
        )

        val tags = event.extractTags()

        assertEquals(listOf("Bachata", "Beginner", "Workshop"), tags)
    }

    private fun createEvent(
        organizer: User = User(id = UUID.randomUUID(), email = "org@example.com", provider = "google", providerId = "p1"),
        organizerId: UUID = organizer.id!!,
        danceStyles: List<DanceStyle> = emptyList(),
        skillLevels: List<SkillLevel> = emptyList(),
        eventTypes: List<EventType> = emptyList()
    ): Event {
        return Event(
            id = UUID.randomUUID(),
            organizerId = organizerId,
            organizer = organizer,
            eventName = "Event",
            description = "desc",
            eventDate = LocalDate.of(2026, 5, 1),
            endDate = null,
            eventTime = LocalTime.NOON,
            locationId = null,
            location = null,
            currency = null,
            price = null,
            maxAttendees = null,
            status = EventStatus.PUBLISHED,
            facebookEventUrl = null,
            danceStyles = danceStyles,
            skillLevels = skillLevels,
            typesOfEvents = eventTypes
        )
    }
}

