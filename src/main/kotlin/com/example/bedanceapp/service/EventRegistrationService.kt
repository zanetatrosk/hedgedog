package com.example.bedanceapp.service

import com.example.bedanceapp.model.AttendingUsersDTO
import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.model.EventRegistrationCount
import com.example.bedanceapp.model.RegistrationProfile
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.DancerRoleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class EventRegistrationService(
    private val eventRegistrationRepository: EventRegistrationRepository,
    private val dancerRoleRepository: DancerRoleRepository
) {

    fun getDancers(eventRegistrations: List<EventRegistration>): List<AttendingUsersDTO>{
        val pairsRoleUsers = dancerRoleRepository.findAll().map { Pair(it.name, mutableListOf<RegistrationProfile>())  }
        val mapRoleToUsers = pairsRoleUsers.toMap().toMutableMap()
        for (registration in eventRegistrations) {
            mapRoleToUsers[registration.role.name]?.add(
                RegistrationProfile(
                    name = registration.userId.toString(),
                    role = registration.role.name,
                    avatar = null,
                    linkToProfile = registration.userId.toString()
                )
            )
        }
        val result = mutableListOf<AttendingUsersDTO>()
        for (pair in mapRoleToUsers) {
            result.add(
                AttendingUsersDTO(
                    role = pair.key,
                    count = pair.value.size,
                    attending = pair.value
                )
            )
        }
        return result
    }

    @Transactional(readOnly = true)
    fun getRegistrationCountsByEventId(eventId: UUID?): EventRegistrationCount {
        if (eventId == null) {
            return EventRegistrationCount(0, 0, 0)
        }
        val eventRegistrations = eventRegistrationRepository.findByEventId(eventId);
        val roles = dancerRoleRepository.findAll().map { Pair(it.name, 0)  }
        val rolesCount = roles.toMap().toMutableMap()
        for (registration in eventRegistrations) {
            rolesCount[registration.role.name]?.plus(1)
        }
        return EventRegistrationCount(eventRegistrations.size, rolesCount["Leader"] ?: 0, rolesCount["Follower"] ?: 0)
    }

    @Transactional(readOnly = true)
    fun getRegistrationsByUserId(userId: UUID): List<EventRegistration> {
        return eventRegistrationRepository.findByUserId(userId)
    }

    @Transactional
    fun createRegistration(registration: EventRegistration): EventRegistration {
        return eventRegistrationRepository.save(registration)
    }

}

