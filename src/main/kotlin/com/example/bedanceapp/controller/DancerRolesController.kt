package com.example.bedanceapp.controller

import com.example.bedanceapp.model.DancerRole
import com.example.bedanceapp.service.DancerRoleService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/dancer-roles")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class DancerRolesController(
    private val dancerRoleService: DancerRoleService
) {

    @GetMapping
    fun getAllDancerRoles(): ResponseEntity<List<DancerRoleResponse>> {
        val roles = dancerRoleService.findAll()
        return ResponseEntity.ok(roles.map { DancerRoleResponse.from(it) })
    }

    @GetMapping("/{id}")
    fun getDancerRoleById(@PathVariable id: UUID): ResponseEntity<DancerRoleResponse> {
        val role = dancerRoleService.findById(id)
        return ResponseEntity.ok(DancerRoleResponse.from(role))
    }
}

data class DancerRoleResponse(
    val id: UUID?,
    val name: String
) {
    companion object {
        fun from(dancerRole: DancerRole) = DancerRoleResponse(
            id = dancerRole.id,
            name = dancerRole.name
        )
    }
}

