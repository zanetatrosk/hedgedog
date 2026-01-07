package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "media")
data class Media(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "media_type", nullable = false)
    val mediaType: String,

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    val filePath: String,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
