package com.example.bedanceapp.model

import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_favorites")
data class UserFavorite(
    @EmbeddedId
    val id: UserFavoriteId,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    val user: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("eventId")
    @JoinColumn(name = "event_id", insertable = false, updatable = false)
    val event: Event? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Embeddable
data class UserFavoriteId(
    @Column(name = "user_id")
    val userId: UUID,

    @Column(name = "event_id")
    val eventId: UUID
) : Serializable
