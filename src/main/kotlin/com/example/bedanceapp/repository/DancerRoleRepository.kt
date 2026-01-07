package com.example.bedanceapp.repository

import com.example.bedanceapp.model.DancerRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DancerRoleRepository : JpaRepository<DancerRole, UUID> {
}

