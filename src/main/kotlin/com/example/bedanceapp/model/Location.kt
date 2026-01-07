package com.example.bedanceapp.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "locations")
data class Location(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, length = 255)
    val name: String,

    @Column(length = 500)
    val street: String? = null,

    @Column(nullable = false, length = 100)
    val city: String,

    @Column(nullable = false, length = 100)
    val country: String,

    @Column(length = 20)
    val postalCode: String? = null,

    @Column(length = 100)
    val houseNumber: String? = null,

    @Column
    val state: String,

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
)
