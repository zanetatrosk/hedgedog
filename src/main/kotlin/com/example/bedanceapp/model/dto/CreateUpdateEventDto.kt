package com.example.bedanceapp.model
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.hibernate.validator.constraints.URL

data class CreateUpdateEventDto(
    @field:NotNull(message = "Basic info is required")
    @field:Valid
    val basicInfo: BasicInfoRequest,

    @field:Valid
    val additionalDetails: AdditionalDetailsRequest?,

    @field:Size(max = 5000, message = "Description is too long (max 5000 chars)")
    val description: String?,

    @field:Valid
    val coverImage: EventMedia?,

    @field:Valid
    val media: List<EventMedia>?
)

data class BasicInfoRequest(
    @field:NotBlank(message = "Event name is required")
    val eventName: String,

    @field:Valid
    val location: LocationRequest?,

    @field:NotNull(message = "Start date is required")
    val date: LocalDate, // Jackson automatically parses "YYYY-MM-DD"

    @field:NotNull(message = "Start time is required")
    val time: LocalTime, // Jackson automatically parses "HH:mm"

    @field:FutureOrPresent(message = "End date must be in the future")
    val endDate: LocalDate?,

    val isRecurring: Boolean?,
    val recurrenceType: RecurrenceType?,

    @field:FutureOrPresent(message = "Recurrence end date must be in the future")
    val recurrenceEndDate: LocalDate?,

    @field:PositiveOrZero(message = "Price cannot be negative")
    val price: BigDecimal?,

    @field:Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g., USD)")
    val currency: String?
)

data class LocationRequest(
    @field:NotBlank(message = "Location name is required")
    val name: String,

    val street: String?,

    @field:NotBlank(message = "City is required")
    val city: String,

    @field:NotBlank(message = "Country is required")
    val country: String,

    val postalCode: String?,
    val houseNumber: String?,
    val state: String?,
    val county: String?
)

data class AdditionalDetailsRequest(
    val danceStyles: List<UUID> = emptyList(),
    val skillLevel: List<UUID> = emptyList(),
    val typeOfEvent: List<UUID> = emptyList(),

    @field:Min(value = 1, message = "Capacity must be at least 1")
    val maxAttendees: Int?,

    @field:URL(message = "Invalid Facebook URL format")
    val facebookEventUrl: String?
)

data class EventMedia(
    @field:Pattern(regexp = "^(image|video)$", message = "Type must be 'image' or 'video'")
    val type: String,

    @field:NotBlank @field:URL
    val url: String,

    @field:NotNull
    val id: UUID
)

data class CreateEventResponse(
    val events: List<UUID?>,
    val message: String
)

enum class RecurrenceType {
    DAILY,
    WEEKLY
}