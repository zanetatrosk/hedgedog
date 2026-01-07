package com.example.bedanceapp.model
import java.util.UUID
import jakarta.persistence.*

@Table(name = "dancer_role")
@Entity
data class DancerRole(
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Id
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val name: String
)







