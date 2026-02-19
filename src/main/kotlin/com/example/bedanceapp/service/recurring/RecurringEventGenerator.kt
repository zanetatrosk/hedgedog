package com.example.bedanceapp.service.recurring

import com.example.bedanceapp.model.RecurrenceType
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Utility class responsible for generating date sequences based on recurrence patterns.
 */
@Component
class RecurringEventGenerator {

    /**
     * Generates a list of dates based on the recurrence type.
     *
     * @param startDate The starting date of the recurrence
     * @param endDate The ending date of the recurrence (inclusive)
     * @param recurrenceType The type of recurrence (DAILY, WEEKLY, MONTHLY)
     * @return List of LocalDate representing each occurrence
     */
    fun generateDates(
        startDate: LocalDate,
        endDate: LocalDate,
        recurrenceType: RecurrenceType
    ): List<LocalDate> {
        validateDateRange(startDate, endDate)

        return when (recurrenceType) {
            RecurrenceType.DAILY -> generateDailyDates(startDate, endDate)
            RecurrenceType.WEEKLY -> generateWeeklyDates(startDate, endDate)
            RecurrenceType.MONTHLY -> generateMonthlyDates(startDate, endDate)
        }
    }

    /**
     * Validates that the date range is valid.
     */
    private fun validateDateRange(startDate: LocalDate, endDate: LocalDate) {
        if (startDate.isAfter(endDate)) {
            throw IllegalArgumentException("Start date must be before or equal to end date")
        }

        // Optional: Add maximum recurrence limit to prevent abuse
        val maxOccurrences = 365 // Maximum 365 occurrences
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate)
        if (daysBetween > maxOccurrences) {
            throw IllegalArgumentException("Date range is too large. Maximum $maxOccurrences days allowed")
        }
    }

    /**
     * Generates dates for daily recurrence.
     */
    private fun generateDailyDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate)
            currentDate = currentDate.plusDays(1)
        }

        return dates
    }

    /**
     * Generates dates for weekly recurrence (every 7 days).
     */
    private fun generateWeeklyDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate)
            currentDate = currentDate.plusWeeks(1)
        }

        return dates
    }

    /**
     * Generates dates for monthly recurrence.
     * Maintains the same day of the month, adjusting for months with fewer days.
     * For example, if startDate is January 31, February will be adjusted to February 28/29.
     */
    private fun generateMonthlyDates(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate)
            currentDate = currentDate.plusMonths(1)
            // Note: plusMonths already handles months with fewer days by clamping to the last valid day
        }

        return dates
    }

    /**
     * Calculates the total number of occurrences for a given recurrence pattern.
     */
    fun calculateOccurrenceCount(
        startDate: LocalDate,
        endDate: LocalDate,
        recurrenceType: RecurrenceType
    ): Int {
        return generateDates(startDate, endDate, recurrenceType).size
    }
}

