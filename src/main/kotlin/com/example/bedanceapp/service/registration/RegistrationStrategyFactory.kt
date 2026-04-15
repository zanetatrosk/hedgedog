package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.RegistrationMode
import org.springframework.stereotype.Component

/**
 * Factory to provide the appropriate registration data strategy based on registration mode
 */
@Component
class RegistrationStrategyFactory(
    strategies: List<RegistrationDataStrategy>
) {
    private val strategyMap: Map<RegistrationMode, RegistrationDataStrategy> = strategies.associateBy {
        when (it) {
            is CoupleModeRegistrationStrategy -> RegistrationMode.COUPLE
            is GoogleFormRegistrationStrategy -> RegistrationMode.GOOGLE_FORM
            is OpenModeRegistrationStrategy -> RegistrationMode.OPEN
            else -> throw IllegalArgumentException("Unknown strategy type: ${it::class.simpleName}")
        }
    }

    fun getStrategy(registrationMode: RegistrationMode): RegistrationDataStrategy {
        return strategyMap[registrationMode]
            ?: throw IllegalArgumentException("No strategy found for registration mode: $registrationMode")
    }
}

