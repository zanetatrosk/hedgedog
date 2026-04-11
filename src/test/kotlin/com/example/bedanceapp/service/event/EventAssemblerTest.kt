package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.AdditionalDetailsRequest
import com.example.bedanceapp.model.BasicInfoRequest
import com.example.bedanceapp.model.CreateUpdateEventDto
import com.example.bedanceapp.model.Currency
import com.example.bedanceapp.model.DanceStyle
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.EventType
import com.example.bedanceapp.model.EventMedia
import com.example.bedanceapp.model.Location
import com.example.bedanceapp.model.Media
import com.example.bedanceapp.model.SkillLevel
import com.example.bedanceapp.model.User
import com.example.bedanceapp.repository.CurrencyRepository
import com.example.bedanceapp.repository.DanceStyleRepository
import com.example.bedanceapp.repository.EventTypeRepository
import com.example.bedanceapp.repository.MediaRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import com.example.bedanceapp.repository.UserRepository
import com.example.bedanceapp.service.LocationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

@DisplayName("EventAssembler Tests")
class EventAssemblerTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var currencyRepository: CurrencyRepository
    @Mock private lateinit var locationService: LocationService
    @Mock private lateinit var danceStyleRepository: DanceStyleRepository
    @Mock private lateinit var skillLevelRepository: SkillLevelRepository
    @Mock private lateinit var eventTypeRepository: EventTypeRepository
    @Mock private lateinit var mediaRepository: MediaRepository

    private lateinit var assembler: EventAssembler

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        assembler = EventAssembler(
            userRepository,
            currencyRepository,
            locationService,
            danceStyleRepository,
            skillLevelRepository,
            eventTypeRepository,
            mediaRepository
        )
    }

    @Test
    fun `buildEventFromRequest maps request with linked entities`() {
        val organizerId = UUID.randomUUID()
        val danceStyleId = UUID.randomUUID()
        val skillLevelId = UUID.randomUUID()
        val eventTypeId = UUID.randomUUID()
        val mediaId = UUID.randomUUID()

        val request = createRequest(
            danceStyles = listOf(danceStyleId),
            skillLevels = listOf(skillLevelId),
            eventTypes = listOf(eventTypeId),
            mediaIds = listOf(mediaId),
            currency = "EUR",
            price = BigDecimal(100)
        )

        val organizer = User(id = organizerId, email = "org@example.com", provider = "google", providerId = "p1")
        val currency = Currency(code = "EUR", name = "Euro", symbol = "EUR")
        val location = Location(id = UUID.randomUUID(), name = "Studio", city = "City", country = "Country")
        val media = Media(id = mediaId, mediaType = "image", filePath = "/tmp/file.jpg", ownerId = organizerId)

        whenever(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(currency))
        whenever(
            locationService.createLocation(
                any(),
                anyOrNull(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(location)
        whenever(danceStyleRepository.findAllById(listOf(danceStyleId))).thenReturn(listOf(DanceStyle(id = danceStyleId, name = "Bachata")))
        whenever(skillLevelRepository.findAllById(listOf(skillLevelId))).thenReturn(listOf(SkillLevel(id = skillLevelId, name = "Beginner", levelOrder = 1)))
        whenever(eventTypeRepository.findAllById(listOf(eventTypeId))).thenReturn(listOf(EventType(id = eventTypeId, name = "Party")))
        whenever(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media))
        whenever(userRepository.findById(organizerId)).thenReturn(Optional.of(organizer))

        val event = assembler.buildEventFromRequest(request, organizerId)

        assertEquals("Sample Event", event.eventName)
        assertEquals(EventStatus.DRAFT, event.status)
        assertEquals(location.id, event.locationId)
        assertEquals("EUR", event.currency?.code)
        assertEquals(1, event.danceStyles.size)
        assertEquals(1, event.skillLevels.size)
        assertEquals(1, event.typesOfEvents.size)
        assertEquals(1, event.media.size)
    }

    @Test
    fun `buildEventFromRequest throws when currency does not exist`() {
        val organizerId = UUID.randomUUID()
        val request = createRequest(currency = "ZZZ", price = BigDecimal(50))
        whenever(currencyRepository.findByCode("ZZZ")).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            assembler.buildEventFromRequest(request, organizerId)
        }

        assertEquals("Currency not found: ZZZ", exception.message)
    }

    @Test
    fun `buildEventFromRequest throws when organizer does not exist`() {
        val organizerId = UUID.randomUUID()
        val request = createRequest(currency = null)
        whenever(userRepository.findById(organizerId)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            assembler.buildEventFromRequest(request, organizerId)
        }

        assertEquals("Organizer not found: $organizerId", exception.message)
    }

    @Test
    fun `buildEventFromRequest throws when media does not exist`() {
        val organizerId = UUID.randomUUID()
        val missingMediaId = UUID.randomUUID()
        val request = createRequest(mediaIds = listOf(missingMediaId))

        whenever(mediaRepository.findById(missingMediaId)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> {
            assembler.buildEventFromRequest(request, organizerId)
        }

        assertEquals("Media not found: $missingMediaId", exception.message)
        verify(userRepository, never()).findById(any())
    }

    private fun createRequest(
        danceStyles: List<UUID> = emptyList(),
        skillLevels: List<UUID> = emptyList(),
        eventTypes: List<UUID> = emptyList(),
        mediaIds: List<UUID> = emptyList(),
        currency: String? = null,
        price: BigDecimal? = null,
    ): CreateUpdateEventDto {
        return CreateUpdateEventDto(
            basicInfo = BasicInfoRequest(
                eventName = "Sample Event",
                location = com.example.bedanceapp.model.LocationRequest(
                    name = "Studio",
                    street = "Street",
                    city = "City",
                    country = "Country",
                    postalCode = null,
                    houseNumber = null,
                    state = null,
                    county = null
                ),
                date = LocalDate.of(2026, 5, 1),
                time = LocalTime.NOON,
                endDate = null,
                isRecurring = false,
                recurrenceType = null,
                recurrenceEndDate = null,
                price = price,
                currency = currency
            ),
            additionalDetails = AdditionalDetailsRequest(
                danceStyles = danceStyles,
                skillLevel = skillLevels,
                typeOfEvent = eventTypes,
                maxAttendees = 20,
                facebookEventUrl = null
            ),
            description = "desc",
            coverImage = EventMedia(type = "image", url = "https://example.com/cover.jpg", id = UUID.randomUUID()),
            media = mediaIds.map { EventMedia(type = "image", url = "https://example.com/$it.jpg", id = it) }
        )
    }
}


