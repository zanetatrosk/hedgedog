package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_dance_styles")
@IdClass(UserDanceStyleId::class)
data class UserDanceStyle(
    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @Id
    @Column(name = "dance_style_id")
    val danceStyleId: UUID,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dance_style_id", insertable = false, updatable = false)
    val danceStyle: DanceStyle? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

data class UserDanceStyleId(
    val userId: UUID? = null,
    val danceStyleId: UUID? = null
) : java.io.Serializable

