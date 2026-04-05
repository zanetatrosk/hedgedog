package com.example.bedanceapp.service

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.Location
import com.example.bedanceapp.model.LocationRequest
import com.example.bedanceapp.model.OrganizerDto

/**
 * Pure transformation extensions for Event-related entities.
 * These do not require database access or external services.
 */

fun Event.toOrganizerDto() = OrganizerDto(
    userId = organizerId.toString(),
    firstName = organizer.profile?.firstName,
    lastName = organizer.profile?.lastName
)

fun Location?.toLocationDto(): LocationRequest? = this?.let {
    LocationRequest(
        name = it.name,
        street = it.street,
        city = it.city,
        country = it.country,
        postalCode = it.postalCode,
        houseNumber = it.houseNumber,
        state = it.state,
        county = it.county
    )
}

/**
 * Aggregates all category names into a single list of strings.
 */
fun Event.extractTags(): List<String> =
    danceStyles.map { it.name } +
            skillLevels.map { it.name } +
            typesOfEvents.map { it.name }