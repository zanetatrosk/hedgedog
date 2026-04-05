package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "dance_styles")
data class DanceStyle(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    override val id: UUID? = null,

    @Column(nullable = false, unique = true)
    override val name: String,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) : Identifiable
