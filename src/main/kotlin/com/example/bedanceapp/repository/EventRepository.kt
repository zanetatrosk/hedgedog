package com.example.bedanceapp.repository

import com.example.bedanceapp.model.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EventRepository : JpaRepository<Event, UUID> {
    fun findByStatus(status: String): List<Event>
}
