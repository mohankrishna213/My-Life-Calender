package com.mohanbuilds.focus.domain

import com.mohanbuilds.focus.data.Recurrence
import com.mohanbuilds.focus.data.Subtask
import com.mohanbuilds.focus.data.Task
import java.time.LocalDate
import java.util.UUID

fun expandTask(
    title: String,
    start: LocalDate,
    recurrence: Recurrence,
    until: LocalDate?,
    subtasks: List<Subtask> = emptyList(),
    maxOccurrences: Int = 730,
): List<Task> {
    val seriesId = UUID.randomUUID().toString()
    val base = Task(
        id = UUID.randomUUID().toString(),
        title = title,
        date = start.toString(),
        subtasks = subtasks,
        seriesId = seriesId,
    )
    if (recurrence == Recurrence.NONE || until == null || until.isBefore(start)) {
        return listOf(base.copy(seriesId = null))
    }
    val stepDays = if (recurrence == Recurrence.DAILY) 1L else 7L
    return generateSequence(start) { it.plusDays(stepDays) }
        .takeWhile { !it.isAfter(until) }
        .take(maxOccurrences)
        .map {
            base.copy(
                id = UUID.randomUUID().toString(),
                date = it.toString(),
                recurrence = recurrence,
                recurrenceUntil = until.toString(),
            )
        }
        .toList()
}

fun Task.isSameSeriesAs(other: Task): Boolean {
    if (recurrence == Recurrence.NONE || other.recurrence == Recurrence.NONE) return false
    return if (seriesId != null && other.seriesId != null) seriesId == other.seriesId
    else title == other.title && recurrence == other.recurrence && recurrenceUntil == other.recurrenceUntil
}
