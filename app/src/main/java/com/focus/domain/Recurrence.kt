package com.focus.domain

import com.focus.data.Recurrence
import com.focus.data.Task
import java.time.LocalDate

/**
 * Materializes a task definition into concrete per-date [Task] instances.
 *
 * Recurring tasks are expanded at creation time into one stored row per
 * occurrence date. Every downstream consumer (intensity grid, lock-screen
 * wallpaper, per-date task list) already works off a flat, date-filtered task
 * list, so expansion keeps all of them unchanged and makes toggling or
 * editing a single occurrence trivial.
 *
 * Subtasks are day-specific and are not copied across occurrences.
 */
fun expandTask(
    title: String,
    start: LocalDate,
    recurrence: Recurrence,
    until: LocalDate?,
    idSeed: Long = System.currentTimeMillis(),
    maxOccurrences: Int = 730,
): List<Task> {
    val base = Task(id = idSeed, title = title, date = start.toString(), seriesId = idSeed)
    if (recurrence == Recurrence.NONE || until == null || until.isBefore(start)) {
        return listOf(base.copy(seriesId = null))
    }
    val stepDays = if (recurrence == Recurrence.DAILY) 1L else 7L
    return generateSequence(start) { it.plusDays(stepDays) }
        .takeWhile { !it.isAfter(until) }
        .take(maxOccurrences)
        .mapIndexed { index, date ->
            base.copy(
                id = idSeed + index,
                date = date.toString(),
                recurrence = recurrence,
                recurrenceUntil = until.toString(),
            )
        }
        .toList()
}

/** True when both tasks belong to the same recurring series. */
fun Task.isSameSeriesAs(other: Task): Boolean {
    if (recurrence == Recurrence.NONE || other.recurrence == Recurrence.NONE) return false
    return if (seriesId != null && other.seriesId != null) seriesId == other.seriesId
    else title == other.title && recurrence == other.recurrence && recurrenceUntil == other.recurrenceUntil
}
