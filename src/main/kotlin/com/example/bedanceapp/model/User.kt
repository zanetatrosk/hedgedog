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

    @Column(name = "google_access_token", length = 2048)
    var googleAccessToken: String? = null,

    @Column(name = "google_refresh_token", length = 512)
    var googleRefreshToken: String? = null,

    @Column(name = "google_token_expiry")
    var googleTokenExpiry: OffsetDateTime? = null,

    @Column(name = "google_scopes", length = 1024)
    var googleScopes: String? = null,

    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val profile: UserProfile? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: OffsetDateTime? = null
) {
    override fun toString(): String {
        return "User(id=$id, email='$email', provider='$provider', providerId='$providerId', createdAt=$createdAt)"
    }
}
