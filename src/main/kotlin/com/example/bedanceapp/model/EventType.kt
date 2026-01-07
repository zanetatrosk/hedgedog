package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "event_type")
data class EventType(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @ManyToMany(mappedBy = "typesOfEvents")
    val events: List<Event> = emptyList()
)
