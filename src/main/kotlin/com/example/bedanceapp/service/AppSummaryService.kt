package com.example.bedanceapp.service

import com.example.bedanceapp.model.AppSummaryDto
import com.example.bedanceapp.model.EventStatus
import com.example.bedanceapp.model.RegistrationStatus
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRepository
import com.example.bedanceapp.repository.UserProfileRepository
import org.springframework.stereotype.Service

@Service
class AppSummaryService(
    private val userProfileRepository: UserProfileRepository,
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRepository: EventRepository
) {

    fun getAppSummary(): AppSummaryDto {
        val totalDancers = userProfileRepository.count()
        val totalRegistrations = eventRegistrationRepository.findByStatusNot(RegistrationStatus.INTERESTED).count()
        val totalEvents = eventRepository.findByStatus(EventStatus.PUBLISHED).size + eventRepository.findByStatus(EventStatus.CANCELLED).size

        return AppSummaryDto(
            totalDancers = totalDancers,
            totalRegistrations = totalRegistrations,
            totalEvents = totalEvents
        )
    }
}

