package com.example.bedanceapp.model
import java.util.UUID
import jakarta.persistence.*

@Table(name = "dancer_roles")
@Entity
data class DancerRole(
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    override val id: UUID? = null,

    @Column(nullable = false, unique = true)
    override val name: String
) : Identifiable







