package com.example.bedanceapp.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "event_registration_settings")
data class EventRegistrationSettings(
    @Id
    @Column(name = "event_id")
    val eventId: UUID,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    val event: Event? = null,

    @Column(name = "registration_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    val registrationMode: RegistrationMode = RegistrationMode.OPEN,

    @Column(name = "form_id")
    val formId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_structure", columnDefinition = "jsonb")
    val formStructure: String? = null,

    @Column(name = "require_approval", nullable = false)
    val requireApproval: Boolean = false
)

enum class RegistrationMode {
    COUPLE,
    OPEN,
    GOOGLE_FORM
}

