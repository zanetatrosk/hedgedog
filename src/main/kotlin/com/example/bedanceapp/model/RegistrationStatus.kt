package com.example.bedanceapp.model

enum class RegistrationStatus {
    REGISTERED,
    INTERESTED,
    WAITLISTED,
    CANCELLED,
    REJECTED,
    PENDING;

    fun canTransitionTo(target: RegistrationStatus): Boolean {
        if (this == target) return true

        val allowedTargets = when (this) {
            INTERESTED -> setOf(PENDING, REGISTERED, WAITLISTED)
            PENDING -> setOf(REGISTERED, WAITLISTED, REJECTED, CANCELLED)
            WAITLISTED -> setOf(REGISTERED, REJECTED, CANCELLED)
            REGISTERED -> setOf(REJECTED, CANCELLED)
            CANCELLED -> setOf(PENDING, REGISTERED, WAITLISTED)
            REJECTED -> emptySet()
        }

        return target in allowedTargets
    }

    fun requireTransitionTo(target: RegistrationStatus) {
        require(canTransitionTo(target)) {
            "Transition from $this to $target is not allowed"
        }
    }
}

