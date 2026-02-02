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

    /**
     * Get a Google Form by ID
     * Requires user to have granted forms permissions
     */
    @GetMapping("/{formId}")
    fun getForm(
        @AuthenticationPrincipal user: User,
        @PathVariable formId: String
    ): ResponseEntity<Any> {
        return try {
            val form = googleFormsService.getForm(user, formId)
            ResponseEntity.ok(form)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(403).body(mapOf("error" to e.message))
        }
    }

    /**
     * Get form responses
     * Requires user to have granted forms permissions
     */
    @GetMapping("/{formId}/responses")
    fun getFormResponses(
        @AuthenticationPrincipal user: User,
        @PathVariable formId: String
    ): ResponseEntity<Any> {
        return try {
            val responses = googleFormsService.getFormResponses(user, formId)
            ResponseEntity.ok(responses)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(403).body(mapOf("error" to e.message))
        }
    }
}

