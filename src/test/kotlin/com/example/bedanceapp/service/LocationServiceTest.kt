package com.example.bedanceapp.service

import com.example.bedanceapp.model.Location
import com.example.bedanceapp.repository.LocationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@DisplayName("LocationService Tests")
class LocationServiceTest {

    @Mock private lateinit var locationRepository: LocationRepository

    private lateinit var service: LocationService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = LocationService(locationRepository)
    }

    @Test
    fun `createLocation builds and saves location`() {
        whenever(locationRepository.save(any())).thenAnswer { it.arguments[0] }

        val location = service.createLocation(
            name = "Studio",
            street = "Main",
            city = "Warsaw",
            country = "Poland",
            postalCode = "00-001",
            houseNumber = "10",
            state = "Mazowieckie",
            county = "Warsaw"
        )

        assertEquals("Studio", location.name)
        assertEquals("Warsaw", location.city)
        assertEquals("Poland", location.country)
    }
}

