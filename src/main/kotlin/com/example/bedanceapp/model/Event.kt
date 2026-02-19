package com.example.bedanceapp.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "events")
data class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(name = "parent_event_id")
    val parentEventId: UUID? = null,

    @Column(name = "organizer_id", nullable = false)
    val organizerId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", insertable = false, updatable = false)
    val organizer: User,

    @Column(name = "event_name", nullable = false)
    val eventName: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "event_date", nullable = false)
    val eventDate: LocalDate,

    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @Column(name = "event_time", nullable = false)
    val eventTime: LocalTime,

    @Column(name = "location_id")
    val locationId: UUID? = null,

    @OneToOne
    @JoinColumn(name = "location_id", insertable = false, updatable = false)
    val location: Location? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_code")
    val currency: Currency? = null,

    @Column(name = "price")
    val price: BigDecimal? = null,

    @Column(name = "max_attendees")
    val maxAttendees: Int? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val status: EventStatus,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "promo_media_id")
    val promoMediaId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_media_id", insertable = false, updatable = false)
    val promoMedia: Media? = null,

    @ManyToMany
    @JoinTable(
        name = "dance_styles_events",
        joinColumns = [JoinColumn(name = "event_id")],
        inverseJoinColumns = [JoinColumn(name = "dance_style_id")]
    )
    val danceStyles: List<DanceStyle> = emptyList(),

    @ManyToMany
    @JoinTable(
        name = "events_skill_levels",
        joinColumns = [JoinColumn(name = "event_id")],
        inverseJoinColumns = [JoinColumn(name = "skill_level_id")]
    )
    val skillLevels: List<SkillLevel> = emptyList(),

    @ManyToMany
    @JoinTable(
        name = "events_event_types",
        joinColumns = [JoinColumn(name = "event_id")],
        inverseJoinColumns = [JoinColumn(name = "event_type_id")]
    )
    val typesOfEvents: List<EventType> = emptyList(),

    @ManyToMany
    @JoinTable(
        name = "events_media",
        joinColumns = [JoinColumn(name = "event_id")],
        inverseJoinColumns = [JoinColumn(name = "media_id")]
    )
    val media: MutableList<Media> = mutableListOf()

)

