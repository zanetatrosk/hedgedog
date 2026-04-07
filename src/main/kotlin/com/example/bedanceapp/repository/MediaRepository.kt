package com.example.bedanceapp.repository

import com.example.bedanceapp.model.Media
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MediaRepository : JpaRepository<Media, UUID> {
	fun findByIdAndOwnerId(id: UUID, ownerId: UUID): Media?

	@Query(
		value = """
			SELECT m.*
			FROM media m
			WHERE NOT EXISTS (SELECT 1 FROM events e WHERE e.promo_media_id = m.id)
			  AND NOT EXISTS (SELECT 1 FROM user_profiles up WHERE up.avatar_media_id = m.id)
			  AND NOT EXISTS (SELECT 1 FROM events_media em WHERE em.media_id = m.id)
			  AND NOT EXISTS (SELECT 1 FROM user_media um WHERE um.media_id = m.id)
		""",
		nativeQuery = true
	)
	fun findOrphanedMedia(): List<Media>
}
