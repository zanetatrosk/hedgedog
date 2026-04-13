package com.example.bedanceapp.controller

import com.example.bedanceapp.model.CodebookItem
import com.example.bedanceapp.model.toCodebook
import com.example.bedanceapp.model.toCodebookList
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
    fun getAllDancerRoles(): ResponseEntity<List<CodebookItem>> {
        val roles = dancerRoleService.findAll()
        return ResponseEntity.ok(roles.toCodebookList())
    }
}


