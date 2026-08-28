package com.focus.data

import kotlinx.serialization.Serializable

@Serializable
data class Goal(
    val title: String,
    val startDate: String,
    val endDate: String,
)

@Serializable
data class Task(
    val id: Long,
    val title: String,
    val date: String,
    val completed: Boolean = false,
)
