package com.mohanbuilds.focus.domain

import com.mohanbuilds.focus.data.Recurrence
import com.mohanbuilds.focus.data.Subtask
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceTest {
    private val start = LocalDate.of(2026, 8, 30)
    private val until = LocalDate.of(2026, 9, 20)

    @Test fun `non-recurring task yields a single occurrence`() {
        val tasks = expandTask("Mock Test", start, Recurrence.NONE, until)
        assertEquals(listOf(start.toString()), tasks.map { it.date })
        assertNull(tasks[0].recurrenceUntil)
    }

    @Test fun `daily recurrence fills every date inclusively`() {
        val tasks = expandTask("2L Water", start, Recurrence.DAILY, until)
        assertEquals(22, tasks.size)
        assertEquals(start.toString(), tasks.first().date)
        assertEquals(until.toString(), tasks.last().date)
        assertTrue(tasks.zipWithNext().all { (a, b) -> LocalDate.parse(b.date) == LocalDate.parse(a.date).plusDays(1) })
    }

    @Test fun `weekly recurrence steps seven days`() {
        val tasks = expandTask("Mock Test", start, Recurrence.WEEKLY, until)
        assertEquals(4, tasks.size)
        assertEquals(listOf("2026-08-30", "2026-09-06", "2026-09-13", "2026-09-20"), tasks.map { it.date })
    }

    @Test fun `until before start falls back to a single occurrence`() {
        val tasks = expandTask("X", start, Recurrence.DAILY, start.minusDays(1))
        assertEquals(1, tasks.size)
        assertEquals(start.toString(), tasks[0].date)
    }

    @Test fun `subtasks and recurrence metadata are copied to every occurrence`() {
        val subtasks = listOf(Subtask("sub-1", "Aptitude"), Subtask("sub-2", "Technical"))
        val tasks = expandTask("Mock Test", start, Recurrence.WEEKLY, until, subtasks)
        assertTrue(tasks.all { it.subtasks == subtasks && it.recurrence == Recurrence.WEEKLY && it.recurrenceUntil == until.toString() })
        assertTrue(tasks.map { it.id }.distinct().size == tasks.size)
    }
}
