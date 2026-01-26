package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    val provider: String,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val profile: UserProfile? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "last_login_at")
    val lastLoginAt: OffsetDateTime? = null
)
