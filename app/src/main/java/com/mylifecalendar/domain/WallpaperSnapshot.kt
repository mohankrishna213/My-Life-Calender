package com.mylifecalendar.domain

import com.mylifecalendar.data.Goal
import com.mylifecalendar.data.Task
import java.time.LocalDate

/**
 * Pure, testable snapshot of everything the lock-screen wallpaper needs.
 * No Android dependencies so it can be unit tested on the JVM.
 */
data class WallpaperSnapshot(
    val goalTitle: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val today: LocalDate,
    /** 1-based index of today within the inclusive goal range, or null if outside. */
    val todayIndex: Int?,
    val totalDays: Int,
    val daysElapsed: Int,
    val daysRemaining: Long,
    val progressPercent: Float,
    /** One intensity level (0-4) per day of the range, in date order. */
    val intensities: List<Int>,
    val tasksTodayTotal: Int,
    val tasksTodayCompleted: Int,
)

object WallpaperSnapshotFactory {

    fun create(goal: Goal, tasks: List<Task>, today: LocalDate): WallpaperSnapshot? {
        val start = runCatching { LocalDate.parse(goal.startDate) }.getOrNull() ?: return null
        val end = runCatching { LocalDate.parse(goal.endDate) }.getOrNull() ?: return null
        if (end.isBefore(start)) return null

        val dates = datesBetween(start, end)
        val totalDays = dates.size
        val todayIndex = dates.indexOf(today).takeIf { it >= 0 }?.plus(1)
        val daysElapsed = when {
            today.isBefore(start) -> 0
            today.isAfter(end) -> totalDays
            else -> todayIndex ?: 0
        }
        val daysRemaining = daysRemaining(today, end)
        val progressPercent = if (totalDays == 0) 0f else (daysElapsed.toFloat() / totalDays) * 100f

        val intensities = dates.map { date ->
            val summary = summarize(date, tasks)
            when (summary.intensity) {
                Intensity.NONE -> 0
                Intensity.LOW -> 1
                Intensity.MEDIUM -> 2
                Intensity.HIGH -> 3
                Intensity.FULL -> 4
            }
        }

        val todayTasks = tasks.filter { it.date == today.toString() }
        return WallpaperSnapshot(
            goalTitle = goal.title,
            startDate = start,
            endDate = end,
            today = today,
            todayIndex = todayIndex,
            totalDays = totalDays,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining,
            progressPercent = progressPercent,
            intensities = intensities,
            tasksTodayTotal = todayTasks.size,
            tasksTodayCompleted = todayTasks.count { it.completed },
        )
    }
}
