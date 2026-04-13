package com.example.bedanceapp.controller

import com.example.bedanceapp.model.AppSummaryDto
import com.example.bedanceapp.service.AppSummaryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for application-wide endpoints
 */
@RestController
@RequestMapping("/api/app")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class AppController(
    private val appSummaryService: AppSummaryService
) {

    /**
     * Get application summary statistics
     * GET /api/app/summary
     *
     * Returns counts of:
     * - Total registered dancers
     * - Total event registrations
     * - Total events
     *
     * @return AppSummary with statistics
     */
    @GetMapping("/summary")
    fun getAppSummary(): ResponseEntity<AppSummaryDto> {
        val summary = appSummaryService.getAppSummary()
        return ResponseEntity.ok(summary)
    }
}

