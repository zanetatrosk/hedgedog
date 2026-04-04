package com.example.bedanceapp.service

import com.example.bedanceapp.model.Location
import com.example.bedanceapp.repository.LocationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LocationService(
    private val locationRepository: LocationRepository
) {
    /**
     * Find or create a location based on address details.
     * This helps avoid duplicate locations when creating events.
     */
    @Transactional
    fun createLocation(
        name: String,
        street: String?,
        city: String,
        country: String,
        postalCode: String?,
        houseNumber: String?,
        state: String?,
        county: String? = null
    ): Location {
        // For now, always create a new location
        // You could add logic to find existing locations by matching address fields
        val location = Location(
            name = name,
            street = street,
            city = city,
            country = country,
            postalCode = postalCode,
            houseNumber = houseNumber,
            state = state,
            county = county
        )
        return locationRepository.save(location)
    }
}

