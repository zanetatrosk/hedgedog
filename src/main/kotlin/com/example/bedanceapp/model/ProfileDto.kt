package com.example.bedanceapp.model

data class ProfileData(
    val firstName: String? = null,
    val lastName: String? = null,
    val bio: String? = null,
    val role: CodebookItem? = null,
    val danceStyles: List<CodebookItem>? = null,
    val media: List<EventMedia>? = null,
    val level: CodebookItem? = null,
    val avatar: EventMedia? = null,
)


