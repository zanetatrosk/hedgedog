package com.example.bedanceapp.model

/**
 * Application summary statistics
 */
data class AppSummaryDto(
    val totalDancers: Long,  // Total number of registered users with profiles
    val totalRegistrations: Long,  // Total number of event registrations
    val totalEvents: Int  // Total number of events
)

