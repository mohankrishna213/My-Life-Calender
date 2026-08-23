package com.mylifecalendar.domain

import com.mylifecalendar.data.Task
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/** The visual intensity used by the contribution grid. */
enum class Intensity(val label: String) { NONE("No tasks"), LOW("Getting started"), MEDIUM("Making progress"), HIGH("Strong day"), FULL("All done") }

data class DaySummary(
    val date: LocalDate,
    val total: Int,
    val completed: Int,
    val intensity: Intensity,
)

data class GridSpec(
    val columns: Int,
    val rows: Int,
    val cellSizeDp: Float,
    val gapDp: Float,
)

fun calculateGridSpec(dayCount: Int, containerWidthDp: Float): GridSpec {
    val safeDayCount = dayCount.coerceAtLeast(1)
    val minimumCellDp = 12f
    val maximumCellDp = 26f
    val minimumGapDp = 3f
    val maximumGapDp = 8f
    val targetColumns = ceil(sqrt(safeDayCount * 2f)).toInt().coerceAtLeast(1)
    val fittingColumns = floor((containerWidthDp + minimumGapDp) / (minimumCellDp + minimumGapDp))
        .toInt()
        .coerceAtLeast(1)
    val columns = minOf(targetColumns, fittingColumns)
    val rows = ceil(safeDayCount.toFloat() / columns).toInt()
    val gap = if (columns == 1) 0f else {
        ((containerWidthDp - maximumCellDp * columns) / (columns - 1))
            .coerceIn(minimumGapDp, maximumGapDp)
    }
    val cellSize = ((containerWidthDp - gap * (columns - 1)) / columns)
        .coerceIn(minimumCellDp, maximumCellDp)
    return GridSpec(columns, rows, cellSize, gap)
}

fun summarize(date: LocalDate, tasks: List<Task>): DaySummary {
    val dayTasks = tasks.filter { it.date == date.toString() }
    val total = dayTasks.size
    val completed = dayTasks.count { it.completed }
    val intensity = when {
        total == 0 -> Intensity.NONE
        completed == total -> Intensity.FULL
        completed.toDouble() / total >= 0.66 -> Intensity.HIGH
        completed.toDouble() / total >= 0.33 -> Intensity.MEDIUM
        else -> Intensity.LOW
    }
    return DaySummary(date, total, completed, intensity)
}

fun datesBetween(start: LocalDate, end: LocalDate): List<LocalDate> {
    if (end.isBefore(start)) return emptyList()
    return (0..ChronoUnit.DAYS.between(start, end).toInt()).map { start.plusDays(it.toLong()) }
}

fun daysRemaining(today: LocalDate, end: LocalDate): Long =
    if (today.isAfter(end)) 0 else ChronoUnit.DAYS.between(today, end) + 1
