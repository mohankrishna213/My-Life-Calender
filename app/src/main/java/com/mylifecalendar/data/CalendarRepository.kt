package com.mylifecalendar.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CalendarRepository(context: Context) {
    private val preferences = context.getSharedPreferences("calendar", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _goal = MutableStateFlow(loadGoal())
    private val _tasks = MutableStateFlow(loadTasks())
    val goal: StateFlow<Goal?> = _goal.asStateFlow()
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    fun saveGoal(goal: Goal) {
        preferences.edit().putString("goal", json.encodeToString(goal)).apply()
        _goal.value = goal
    }

    fun addTask(title: String, date: String) {
        val task = Task(System.currentTimeMillis(), title, date)
        saveTasks(_tasks.value + task)
    }

    fun toggleTask(id: Long) {
        saveTasks(_tasks.value.map { if (it.id == id) it.copy(completed = !it.completed) else it })
    }

    fun deleteTask(id: Long) = saveTasks(_tasks.value.filterNot { it.id == id })

    private fun saveTasks(tasks: List<Task>) {
        preferences.edit().putString("tasks", json.encodeToString(tasks)).apply()
        _tasks.value = tasks
    }

    private fun loadGoal(): Goal? = preferences.getString("goal", null)?.let { json.decodeFromString<Goal>(it) }
    private fun loadTasks(): List<Task> = preferences.getString("tasks", null)?.let { json.decodeFromString<List<Task>>(it) } ?: emptyList()
}
