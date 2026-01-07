package com.example.bedanceapp.repository

import com.example.bedanceapp.model.EventType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface EventTypeRepository : JpaRepository<EventType, UUID>

