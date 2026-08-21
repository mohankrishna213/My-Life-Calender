package com.mylifecalendar.domain

import com.mylifecalendar.data.Task
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
}
