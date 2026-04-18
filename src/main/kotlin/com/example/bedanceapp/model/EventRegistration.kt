package com.example.bedanceapp.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "registrations")
data class EventRegistration(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "event_id")
    val eventId: UUID,

    @Column(name = "user_id")
    val userId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    val status: RegistrationStatus,

    @Column(name = "role_id")
    val roleId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    val role: DancerRole? = null,

    @Column(name = "email")
    val email: String?,

    @Column(name = "is_anonymous", nullable = false)
    val isAnonymous: Boolean = false,

    @Column(name = "response_id")
    val responseId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_responses", columnDefinition = "jsonb")
    val formResponses: String? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "waitlisted_at")
    val waitlistedAt: LocalDateTime? = null
    )


