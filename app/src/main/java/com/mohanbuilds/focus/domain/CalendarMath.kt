package com.mohanbuilds.focus.domain

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.mohanbuilds.focus.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Converts a local date to the epoch-millis the Material3 DatePicker expects.
 * The picker treats millis as the *UTC* midnight of the chosen date, so the
 * conversion must stay on UTC. Using the device zone here drifts the date by
 * one day for any timezone with a non-zero UTC offset.
 */
fun localDateToPickerMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Converts the picker's UTC-midnight millis back to the intended local date. */
fun pickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

/** The visual intensity used by the contribution grid. */
enum class Intensity(val label: String) { NONE("No tasks"), LOW("Getting started"), MEDIUM("Making progress"), HIGH("Strong day"), FULL("All done") }

fun Intensity.color() = when (this) {
    Intensity.NONE -> Color(0xFFE2E6E1)
    Intensity.LOW -> Color(0xFFB7D9C5)
    Intensity.MEDIUM -> Color(0xFF72B58A)
    Intensity.HIGH -> Color(0xFF3E8B61)
    Intensity.FULL -> Color(0xFF145A3A)
}

fun Intensity.lockScreenColor(): Int = when (this) {
    Intensity.NONE -> AndroidColor.parseColor("#21262D")
    Intensity.LOW -> AndroidColor.parseColor("#0E4429")
    Intensity.MEDIUM -> AndroidColor.parseColor("#006D32")
    Intensity.HIGH -> AndroidColor.parseColor("#26A641")
    Intensity.FULL -> AndroidColor.parseColor("#39D353")
}

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
        completed == 0 -> Intensity.NONE
        completed == total -> Intensity.FULL
        completed.toDouble() / total >= 0.66 -> Intensity.HIGH
        completed.toDouble() / total >= 0.33 -> Intensity.MEDIUM
        else -> Intensity.LOW
    }
    return DaySummary(date, total, completed, intensity)
}

/**
 * Same intensity rules as [summarize], but computed for a whole range in one
 * pass so the calendar grid can look up each cell instead of re-filtering the
 * task list once per day (a full year of cells would otherwise scan the task
 * list hundreds of times per recomposition).
 */
fun summarizeByDate(calendarDates: List<LocalDate>, tasks: List<Task>): Map<LocalDate, Intensity> {
    val byDate = tasks.groupBy { it.date }
    return calendarDates.associateWith { date ->
        val dayTasks = byDate[date.toString()].orEmpty()
        val completed = dayTasks.count { it.completed }
        when {
            // A scheduled day with nothing done yet (e.g. future occurrences of a
            // recurring task) must render as the empty grey cell, never green.
            completed == 0 -> Intensity.NONE
            completed == dayTasks.size -> Intensity.FULL
            completed.toDouble() / dayTasks.size >= 0.66 -> Intensity.HIGH
            completed.toDouble() / dayTasks.size >= 0.33 -> Intensity.MEDIUM
            else -> Intensity.LOW
        }
    }
}

fun datesBetween(start: LocalDate, end: LocalDate): List<LocalDate> {
    if (end.isBefore(start)) return emptyList()
    return (0..ChronoUnit.DAYS.between(start, end).toInt()).map { start.plusDays(it.toLong()) }
}

fun daysRemaining(today: LocalDate, end: LocalDate): Long =
    if (today.isAfter(end)) 0 else ChronoUnit.DAYS.between(today, end) + 1
