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
     * Calls parent to get base headers and inserts role header before the last item (CREATED_AT)
     */
    override fun getHeaders(): List<Header> {
        val baseHeaders = super.getHeaders()
        val dancerRoles = dancerRoleRepository.findAll()

        // Insert role header before CREATED_AT (last item)
        return baseHeaders.dropLast(1) + CoupleHeaders.role(dancerRoles) + baseHeaders.last()
        // CoupleHeaders.PARTNER, // TODO: Add when implementing partner matching feature
    }

    /**
     * Override to add role data field
     * Calls parent to get base data fields and inserts role data before the last item (CREATED_AT)
     */
    override fun buildDataFields(registration: EventRegistration, fullName: String): List<RegistrationDataDto> {
        val baseDataFields = super.buildDataFields(registration, fullName)
        val roleData = RegistrationDataDto("role", registration.role?.name ?: "")

        // Insert role data before CREATED_AT (last item)
        return baseDataFields.dropLast(1) + roleData + baseDataFields.last()
    }
}

