package com.mohanbuilds.focus.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mohanbuilds.focus.CalendarViewModel
import com.mohanbuilds.focus.data.CalendarRepository
import com.mohanbuilds.focus.data.Goal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarViewModelTest {
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = CalendarRepository(context)
        viewModel = CalendarViewModel(repository)
    }

    @Test fun `saveGoal updates goal state`() {
        val goal = Goal("Test", "2026-01-01", "2026-12-31")
        viewModel.saveGoal("Test", "2026-01-01", "2026-12-31")
        assertEquals(goal, viewModel.goal.value)
    }
}
