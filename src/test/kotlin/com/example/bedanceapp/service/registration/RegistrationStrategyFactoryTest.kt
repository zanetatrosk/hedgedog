package com.example.bedanceapp.service.registration

import com.example.bedanceapp.model.Event
import com.example.bedanceapp.model.RegistrationMode
import com.example.bedanceapp.repository.DancerRoleRepository
import com.example.bedanceapp.repository.EventRegistrationRepository
import com.example.bedanceapp.repository.EventRegistrationSettingsRepository
import com.example.bedanceapp.repository.SkillLevelRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

@DisplayName("RegistrationStrategyFactory Tests")
class RegistrationStrategyFactoryTest {

    @Test
    fun `getStrategy resolves all supported modes`() {
        val eventRegistrationRepository: EventRegistrationRepository = mock()
        val skillLevelRepository: SkillLevelRepository = mock()
        val dancerRoleRepository: DancerRoleRepository = mock()
        val eventRegistrationSettingsRepository: EventRegistrationSettingsRepository = mock()
        val mapper: GoogleFormMapper = mock()

        val openStrategy = OpenModeRegistrationStrategy(eventRegistrationRepository, skillLevelRepository)
        val coupleStrategy = CoupleModeRegistrationStrategy(
            eventRegistrationRepository,
            skillLevelRepository,
            dancerRoleRepository
        )
        val googleFormStrategy = GoogleFormRegistrationStrategy(
            eventRegistrationRepository,
            eventRegistrationSettingsRepository,
            mapper
        )

        val factory = RegistrationStrategyFactory(listOf(openStrategy, coupleStrategy, googleFormStrategy))

        assertEquals(openStrategy, factory.getStrategy(RegistrationMode.OPEN))
        assertEquals(coupleStrategy, factory.getStrategy(RegistrationMode.COUPLE))
        assertEquals(googleFormStrategy, factory.getStrategy(RegistrationMode.GOOGLE_FORM))
    }

    @Test
    fun `constructor throws for unknown strategy type`() {
        val unknown = object : RegistrationDataStrategy {
            override fun getRegistrationData(event: Event): RegistrationData {
                return RegistrationData(headers = emptyList(), registrations = emptyList())
            }
        }

        assertThrows<IllegalArgumentException> {
            RegistrationStrategyFactory(listOf(unknown))
        }
    }
}

