package com.example.bedanceapp.service.registration

import com.example.bedanceapp.controller.RegistrationStatus
import java.time.LocalDateTime

object RegistrationWaitlistTimestampResolver {
    fun resolve(
        previousStatus: RegistrationStatus?,
        previousWaitlistedAt: LocalDateTime?,
        newStatus: RegistrationStatus,
        now: LocalDateTime = LocalDateTime.now()
    ): LocalDateTime? {
        if (newStatus != RegistrationStatus.WAITLISTED) {
            return null
        }

        return if (previousStatus == RegistrationStatus.WAITLISTED) {
            previousWaitlistedAt ?: now
        } else {
            now
        }
    }
}

