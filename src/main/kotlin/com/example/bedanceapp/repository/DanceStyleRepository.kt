package com.example.bedanceapp.repository

import com.example.bedanceapp.model.DanceStyle
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DanceStyleRepository : JpaRepository<DanceStyle, UUID>
