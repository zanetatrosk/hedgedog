package com.example.bedanceapp.controller

import com.example.bedanceapp.model.User
import com.example.bedanceapp.service.GoogleFormsService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/google-forms")
class GoogleFormsController(
    private val googleFormsService: GoogleFormsService
) {

    /**
     * Check if user has granted Google Forms access
     */
    @GetMapping("/access-status")
    fun checkFormsAccess(@AuthenticationPrincipal user: User): ResponseEntity<Map<String, Boolean>> {
        val hasAccess = googleFormsService.hasFormsAccess(user)
        return ResponseEntity.ok(mapOf("hasAccess" to hasAccess))
    }
}

