package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_profiles")
data class UserProfile(
    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    val user: User,

    @Column(name = "first_name")
    val firstName: String? = null,

    @Column(name = "last_name")
    val lastName: String? = null,

    @Column(columnDefinition = "TEXT")
    val bio: String? = null,

    @Column(name = "role_id")
    val roleId: UUID? = null,

    @Column(name = "general_skill_level_id")
    val generalSkillLevelId: UUID? = null,

    val city: String? = null,
    val country: String? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
