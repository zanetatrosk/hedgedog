package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.EventRegistration
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import org.springframework.stereotype.Component

/**
 * Strategy for COUPLE registration mode
 * Extends OpenModeRegistrationStrategy and adds role selection and future partner matching/coupling features
 *
 * Why extend instead of duplicate?
 * - Couple mode is essentially Open mode + role functionality
 * - Follows DRY principle - reuses all the common mapping logic
 * - Only overrides what's different: headers and the role data field
 * - Makes it easy to maintain - changes to common logic only need to be made once
 */
@Component
class CoupleModeRegistrationStrategy(
    eventRegistrationRepository: EventRegistrationRepository,
    skillLevelRepository: SkillLevelRepository,
    private val dancerRoleRepository: DancerRoleRepository
) : OpenModeRegistrationStrategy(eventRegistrationRepository, skillLevelRepository) {

    /**
     * Override to add role header for couple mode
     */
    override fun getHeaders(): List<Header> {
        val skillLevels = skillLevelRepository.findAll()
        val dancerRoles = dancerRoleRepository.findAll()
        return listOf(
            RegistrationHeaders.FULLNAME,
            RegistrationHeaders.experience(skillLevels),
            CoupleHeaders.role(dancerRoles),
            // CoupleHeaders.PARTNER, // TODO: Add when implementing partner matching feature
            RegistrationHeaders.CREATED_AT
        )
    }

    /**
     * Override to add role data field
     * Reuses the base buildFullName() helper
     */
    override fun buildDataFields(registration: EventRegistration, fullName: String): List<RegistrationDataDto> {
        return listOf(
            RegistrationDataDto(RegistrationHeaders.FULLNAME.id, fullName),
            RegistrationDataDto("experience", registration.user?.profile?.generalSkillLevel?.name ?: ""),
            RegistrationDataDto("role", registration.role?.name ?: ""),
            // TODO: Add partner data when implementing partner matching
            RegistrationDataDto(RegistrationHeaders.CREATED_AT.id, registration.createdAt.toString())
        )
    }

    // TODO: Future methods for couple-specific features
    // fun suggestPartnerMatches(eventId: UUID): List<PartnerMatch>
    // fun coupleRegistrations(registration1Id: UUID, registration2Id: UUID)
    // fun uncoupleRegistrations(coupleId: UUID)
}

