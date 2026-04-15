package com.example.bedanceapp.service.event

import com.example.bedanceapp.model.RecurrenceType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals

@DisplayName("RecurringEventGenerator Tests")
class RecurringEventGeneratorTest {

    private val generator = RecurringEventGenerator()

    @Test
    fun `generateDates returns daily inclusive range`() {
        val start = LocalDate.of(2026, 4, 1)
        val end = LocalDate.of(2026, 4, 3)

        val dates = generator.generateDates(start, end, RecurrenceType.DAILY)

        assertEquals(
            listOf(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 3)
            ),
            dates
        )
    }

    @Test
    fun `generateDates returns weekly inclusive range when end is same weekday`() {
        val start = LocalDate.of(2026, 4, 4)
        val end = LocalDate.of(2026, 4, 18)

        val dates = generator.generateDates(start, end, RecurrenceType.WEEKLY)

        assertEquals(
            listOf(
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 11),
                LocalDate.of(2026, 4, 18)
            ),
            dates
        )
    }

    @Test
    fun `generateDates throws when start date is after end date`() {
        val exception = assertThrows<IllegalArgumentException> {
            generator.generateDates(
                LocalDate.of(2026, 4, 12),
                LocalDate.of(2026, 4, 11),
                RecurrenceType.DAILY
            )
        }

        assertEquals("Start date must be before or equal to end date", exception.message)
    }

    @Test
    fun `generateDates throws when weekly end does not align with recurrence day`() {
        val exception = assertThrows<IllegalArgumentException> {
            generator.generateDates(
                LocalDate.of(2026, 4, 4),
                LocalDate.of(2026, 4, 13),
                RecurrenceType.WEEKLY
            )
        }

        assertEquals("For weekly recurrence, end date must fall on the same day of week as start date. Start: SATURDAY, End: MONDAY", exception.message)
    }

    @Test
    fun `generateDates throws when range exceeds max occurrence window`() {
        val exception = assertThrows<IllegalArgumentException> {
            generator.generateDates(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 2),
                RecurrenceType.DAILY
            )
        }

        assertEquals("Date range is too large. Maximum 365 days allowed", exception.message)
    }
}

