package com.example.bedanceapp.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_media")
@IdClass(UserMediaId::class)
data class UserMedia(
    @Id
    @Column(name = "user_id")
    val userId: UUID,

    @Id
    @Column(name = "media_id")
    val mediaId: UUID,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "media_id", insertable = false, updatable = false)
    val media: Media? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class UserMediaId(
    val userId: UUID? = null,
    val mediaId: UUID? = null
) : java.io.Serializable

