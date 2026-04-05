package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "skill_levels")
data class SkillLevel(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    override val id: UUID? = null,

    @Column(nullable = false, unique = true)
    override val name: String,

    @Column(name = "level_order", nullable = false, unique = true)
    val levelOrder: Int,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) : Identifiable
