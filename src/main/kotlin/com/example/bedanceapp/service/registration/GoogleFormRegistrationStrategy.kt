package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GoogleFormRegistrationStrategy(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository,
    private val googleFormMapper: GoogleFormMapper
) : RegistrationDataStrategy {

    @Transactional(readOnly = true)
    override fun getRegistrationData(event: Event): RegistrationData {
        val eventId = event.id ?: throw IllegalArgumentException("Event ID cannot be null")
        val formStructureJson = eventRegistrationSettingsRepository.findByEventId(eventId)?.formStructure
        val headers = googleFormMapper.parseFormStructure(formStructureJson)?.headers ?: emptyList()

        val registrationsByResponseId = eventRegistrationRepository.findByEventId(eventId)
            .associateBy { it.responseId ?: throw IllegalArgumentException("Registration with ID ${it.id} has null responseId") }

        val rows = registrationsByResponseId.values
            .map(googleFormMapper::mapRegistrationToRegistrationRow)

        return RegistrationData(headers, rows)
    }
}
