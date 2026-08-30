package com.focus.data

import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    val title: String,
    val startDate: String,
    val endDate: String,
)

/** How often a task repeats. [NONE] is a one-off task for a single date. */
enum class Recurrence(val label: String, val icon: String) {
    NONE("No Repeat", "🚫"),
    DAILY("Daily", "📅"),
    WEEKLY("Weekly", "🗓️"),
}

@Serializable
data class Subtask(
    val id: Long,
    val title: String,
    val completed: Boolean = false,
)

@Serializable
data class Task(
    val id: Long,
    val title: String,
    val date: String,
    val completed: Boolean = false,
    val recurrence: Recurrence = Recurrence.NONE,
    /** Inclusive last date the recurrence runs; null when [recurrence] is [Recurrence.NONE]. */
    val recurrenceUntil: String? = null,
    /** Id shared by every occurrence of one recurring series; null for one-off and legacy rows. */
    val seriesId: Long? = null,
    val subtasks: List<Subtask> = emptyList(),
)
