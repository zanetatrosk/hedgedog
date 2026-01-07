package com.example.bedanceapp.model

import jakarta.persistence.*

@Entity
@Table(name = "currencies")
data class Currency(
    @Id
    @Column(name = "code", nullable = false, unique = true, length = 3)
    val code: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "symbol", nullable = false)
    val symbol: String
)

