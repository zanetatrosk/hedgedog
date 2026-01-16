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

    @Transactional
    fun createLocation(location: Location): Location {
        return locationRepository.save(location)
    }

    @Transactional(readOnly = true)
    fun getLocationById(id: UUID): Location? {
        return locationRepository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun getAllLocations(): List<Location> {
        return locationRepository.findAll()
    }

    @Transactional
    fun updateLocation(id: UUID, location: Location): Location {
        require(locationRepository.existsById(id)) { "Location not found with id: $id" }
        return locationRepository.save(location.copy(id = id))
    }

    @Transactional
    fun deleteLocation(id: UUID) {
        locationRepository.deleteById(id)
    }

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
        state: String,
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

