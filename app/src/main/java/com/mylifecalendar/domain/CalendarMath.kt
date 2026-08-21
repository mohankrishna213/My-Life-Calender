package com.mylifecalendar.domain

import com.mylifecalendar.data.Task
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The visual intensity used by the contribution grid. */
enum class Intensity(val label: String) { NONE("No tasks"), LOW("Getting started"), MEDIUM("Making progress"), HIGH("Strong day"), FULL("All done") }

data class DaySummary(
    val date: LocalDate,
    val total: Int,
    val completed: Int,
    val intensity: Intensity,
)

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

fun daysRemaining(today: LocalDate, end: LocalDate): Long = maxOf(0, ChronoUnit.DAYS.between(today, end))
