package com.example.bedanceapp.service

import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.repository.DancerRoleRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DancerRoleService(
    private val dancerRoleRepository: DancerRoleRepository
) {
    fun findAll(): List<DancerRole> = dancerRoleRepository.findAll()

    fun findById(id: UUID): DancerRole {
        return dancerRoleRepository.findById(id)
            .orElseThrow { NoSuchElementException("DancerRole not found with id: $id") }
    }
}





