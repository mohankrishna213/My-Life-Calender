package com.mylifecalendar.domain

import com.mylifecalendar.data.Goal
import com.mylifecalendar.data.Task
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperSnapshotTest {

    private val start = LocalDate.of(2026, 1, 1)
    private val end = LocalDate.of(2026, 12, 31)

    @Test fun `snapshot covers inclusive range`() {
        val goal = Goal("Year", start.toString(), end.toString())
        val snapshot = WallpaperSnapshotFactory.create(goal, emptyList(), LocalDate.of(2026, 8, 23))
        assertNotNull(snapshot)
        assertEquals(365, snapshot!!.totalDays)
        assertEquals(365, snapshot.intensities.size)
    }

    @Test fun `today index is one-based within range`() {
        val goal = Goal("Year", start.toString(), end.toString())
        val today = LocalDate.of(2026, 8, 23)
        val snapshot = WallpaperSnapshotFactory.create(goal, emptyList(), today)!!
        assertEquals(235, snapshot.todayIndex)
        assertEquals(235, snapshot.daysElapsed)
    }

    @Test fun `today outside range has null index`() {
        val goal = Goal("Past", "2025-01-01", "2025-06-30")
        val snapshot = WallpaperSnapshotFactory.create(goal, emptyList(), LocalDate.of(2026, 8, 23))!!
        assertNull(snapshot.todayIndex)
        assertEquals(snapshot.totalDays, snapshot.daysElapsed)
    }

    @Test fun `progress percent is elapsed over total`() {
        val goal = Goal("Half year", "2026-01-01", "2026-06-30")
        val snapshot = WallpaperSnapshotFactory.create(goal, emptyList(), LocalDate.of(2026, 4, 1))!!
        assertEquals(50f, snapshot.progressPercent, 0.5f)
    }

    @Test fun `completed tasks map to full intensity`() {
        val date = LocalDate.of(2026, 3, 10)
        val tasks = listOf(
            Task(1, "a", date.toString(), true),
            Task(2, "b", date.toString(), true),
        )
        val goal = Goal("Year", start.toString(), end.toString())
        val snapshot = WallpaperSnapshotFactory.create(goal, tasks, date)!!
        val index = snapshot.todayIndex!! - 1
        assertEquals(4, snapshot.intensities[index])
        assertEquals(2, snapshot.tasksTodayTotal)
        assertEquals(2, snapshot.tasksTodayCompleted)
    }

    @Test fun `no tasks maps to zero intensity`() {
        val date = LocalDate.of(2026, 3, 10)
        val goal = Goal("Year", start.toString(), end.toString())
        val snapshot = WallpaperSnapshotFactory.create(goal, emptyList(), date)!!
        val index = snapshot.todayIndex!! - 1
        assertEquals(0, snapshot.intensities[index])
        assertEquals(0, snapshot.tasksTodayTotal)
    }

    @Test fun `partial completion maps to mid intensity`() {
        val date = LocalDate.of(2026, 3, 10)
        val tasks = listOf(Task(1, "a", date.toString(), true), Task(2, "b", date.toString(), false))
        val goal = Goal("Year", start.toString(), end.toString())
        val snapshot = WallpaperSnapshotFactory.create(goal, tasks, date)!!
        val index = snapshot.todayIndex!! - 1
        assertTrue(snapshot.intensities[index] in 1..3)
    }

    @Test fun `invalid or reversed range returns null`() {
        assertNull(WallpaperSnapshotFactory.create(Goal("Bad", "2026-05-01", "2026-04-01"), emptyList(), LocalDate.now()))
        assertNull(WallpaperSnapshotFactory.create(Goal("Bad", "not-a-date", "2026-04-01"), emptyList(), LocalDate.now()))
    }

    @Test fun `single day goal works`() {
        val day = LocalDate.of(2026, 8, 23)
        val snapshot = WallpaperSnapshotFactory.create(Goal("One", day.toString(), day.toString()), emptyList(), day)!!
        assertEquals(1, snapshot.totalDays)
        assertEquals(1, snapshot.todayIndex)
        assertEquals(100f, snapshot.progressPercent, 0.01f)
    }
}
