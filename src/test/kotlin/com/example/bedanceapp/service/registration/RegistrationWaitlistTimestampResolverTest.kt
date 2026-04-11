package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("RegistrationWaitlistTimestampResolver Tests")
class RegistrationWaitlistTimestampResolverTest {

    @Test
    fun `resolve returns null when new status is not waitlisted`() {
        val resolved = RegistrationWaitlistTimestampResolver.resolve(
            previousStatus = RegistrationStatus.WAITLISTED,
            previousWaitlistedAt = LocalDateTime.of(2026, 4, 1, 10, 0),
            newStatus = RegistrationStatus.REGISTERED,
            now = LocalDateTime.of(2026, 4, 10, 12, 0)
        )

        assertNull(resolved)
    }

    @Test
    fun `resolve keeps existing waitlisted timestamp when still waitlisted`() {
        val previous = LocalDateTime.of(2026, 4, 1, 10, 0)

        val resolved = RegistrationWaitlistTimestampResolver.resolve(
            previousStatus = RegistrationStatus.WAITLISTED,
            previousWaitlistedAt = previous,
            newStatus = RegistrationStatus.WAITLISTED,
            now = LocalDateTime.of(2026, 4, 10, 12, 0)
        )

        assertEquals(previous, resolved)
    }

    @Test
    fun `resolve uses now when still waitlisted but previous timestamp missing`() {
        val now = LocalDateTime.of(2026, 4, 10, 12, 0)

        val resolved = RegistrationWaitlistTimestampResolver.resolve(
            previousStatus = RegistrationStatus.WAITLISTED,
            previousWaitlistedAt = null,
            newStatus = RegistrationStatus.WAITLISTED,
            now = now
        )

        assertEquals(now, resolved)
    }

    @Test
    fun `resolve sets now when entering waitlisted from another status`() {
        val now = LocalDateTime.of(2026, 4, 10, 12, 0)

        val resolved = RegistrationWaitlistTimestampResolver.resolve(
            previousStatus = RegistrationStatus.PENDING,
            previousWaitlistedAt = LocalDateTime.of(2026, 4, 1, 10, 0),
            newStatus = RegistrationStatus.WAITLISTED,
            now = now
        )

        assertEquals(now, resolved)
    }
}

