package com.focus.domain

import com.focus.data.Task
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMathTest {
    @Test fun `date range includes both endpoints`() {
        assertEquals(3, datesBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3)).size)
    }

    @Test fun `intensity reflects completion ratio`() {
        val date = LocalDate.of(2026, 1, 1)
        val tasks = listOf(Task(1, "one", date.toString(), true), Task(2, "two", date.toString()))
        assertEquals(Intensity.MEDIUM, summarize(date, tasks).intensity)
    }

    @Test fun `empty day has no task intensity`() {
        assertEquals(Intensity.NONE, summarize(LocalDate.now(), emptyList()).intensity)
    }

    @Test fun `days remaining includes today`() {
        assertEquals(30, daysRemaining(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 9, 21)))
    }

    @Test fun `grid balances a short goal`() {
        val grid = calculateGridSpec(32, 320f)
        assertEquals(8, grid.columns)
        assertEquals(4, grid.rows)
        assert(grid.gapDp >= 3f)
    }

    @Test fun `grid stays within container for a year`() {
        val grid = calculateGridSpec(365, 320f)
        assert(grid.columns * grid.cellSizeDp + (grid.columns - 1) * grid.gapDp <= 320.01f)
    }

    @Test fun `picker millis round-trip keeps the exact local date in any zone`() {
        val dates = listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 8, 28),
            LocalDate.of(2027, 1, 1),
        )
        dates.forEach { date ->
            assertEquals("round trip drifted for $date", date, pickerMillisToLocalDate(localDateToPickerMillis(date)))
        }
    }

    @Test fun `picker millis are UTC midnights of the intended date`() {
        assertEquals(0L, localDateToPickerMillis(LocalDate.of(1970, 1, 1)))
        assertEquals(86_400_000L, localDateToPickerMillis(LocalDate.of(1970, 1, 2)))
        assertEquals(LocalDate.of(1970, 1, 2), pickerMillisToLocalDate(86_400_000L))
    }
}
