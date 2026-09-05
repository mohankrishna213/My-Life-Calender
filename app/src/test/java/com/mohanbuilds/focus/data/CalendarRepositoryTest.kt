package com.mohanbuilds.focus.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarRepositoryTest {
    private lateinit var repository: CalendarRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = CalendarRepository(context)
    }

    @Test fun `saveGoal persists and reloads correctly`() {
        val goal = Goal("Test", "2026-01-01", "2026-12-31")
        repository.saveGoal(goal)
        assertEquals(goal, repository.goal.value)
    }

    @Test fun `addTasks creates tasks with unique IDs`() {
        val tasks = listOf(
            Task("t-1", "Task 1", "2026-01-01"),
            Task("t-2", "Task 2", "2026-01-01"),
        )
        repository.addTasks(tasks)
        assertEquals(2, repository.tasks.value.size)
        assertEquals(2, repository.tasks.value.map { it.id }.distinct().size)
    }

    @Test fun `toggleTask flips completion`() {
        repository.addTasks(listOf(Task("t-1", "Task", "2026-01-01")))
        repository.toggleTask("t-1")
        assertTrue(repository.tasks.value.first().completed)
        repository.toggleTask("t-1")
        assertFalse(repository.tasks.value.first().completed)
    }

    @Test fun `deleteTask removes correct task`() {
        repository.addTasks(listOf(
            Task("t-1", "A", "2026-01-01"),
            Task("t-2", "B", "2026-01-01"),
        ))
        repository.deleteTask("t-1")
        assertEquals(1, repository.tasks.value.size)
        assertEquals("t-2", repository.tasks.value.first().id)
    }
}
