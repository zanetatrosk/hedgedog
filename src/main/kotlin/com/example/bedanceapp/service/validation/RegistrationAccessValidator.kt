package com.example.bedanceapp.service.validation

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.repository.EventRegistrationRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RegistrationAccessValidator(
    private val eventRegistrationRepository: EventRegistrationRepository
) {
    fun requireForEvent(registrationId: UUID, eventId: UUID): EventRegistration {
        return eventRegistrationRepository.findByIdAndEventId(registrationId, eventId)
            ?: throw IllegalArgumentException("Registration does not belong to this event")
    }

    fun requireForUserInEvent(registrationId: UUID, eventId: UUID, userId: UUID): EventRegistration {
        return eventRegistrationRepository.findByIdAndEventIdAndUserId(registrationId, eventId, userId)
            ?: throw IllegalArgumentException("User is not authorized for this registration")
    }
}


