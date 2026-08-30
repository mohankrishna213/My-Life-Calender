package com.focus.data

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

    fun addTasks(tasks: List<Task>) = saveTasks(_tasks.value + tasks)

    fun updateTask(task: Task) = saveTasks(_tasks.value.map { if (it.id == task.id) task else it })

    /**
     * Toggles a task. A task with subtasks derives its completion from them:
     * toggling the parent completes (or reopens) every subtask at once.
     */
    fun toggleTask(id: Long) {
        saveTasks(_tasks.value.map { task ->
            if (task.id != id) task
            else if (task.subtasks.isEmpty()) task.copy(completed = !task.completed)
            else {
                val target = !task.completed
                task.copy(completed = target, subtasks = task.subtasks.map { it.copy(completed = target) })
            }
        })
    }

    /** Toggles one subtask and re-derives the parent's completion from the ratio. */
    fun toggleSubtask(taskId: Long, subtaskId: Long) {
        saveTasks(_tasks.value.map { task ->
            if (task.id != taskId) task
            else {
                val subtasks = task.subtasks.map { if (it.id == subtaskId) it.copy(completed = !it.completed) else it }
                task.copy(subtasks = subtasks, completed = subtasks.isNotEmpty() && subtasks.all { it.completed })
            }
        })
    }

    fun deleteTask(id: Long) = saveTasks(_tasks.value.filterNot { it.id == id })

    fun deleteTasksWhere(predicate: (Task) -> Boolean) = saveTasks(_tasks.value.filterNot(predicate))

    fun replaceTasks(tasks: List<Task>) = saveTasks(tasks)

    private fun saveTasks(tasks: List<Task>) {
        preferences.edit().putString("tasks", json.encodeToString(tasks)).apply()
        _tasks.value = tasks
    }

    private fun loadGoal(): Goal? = preferences.getString("goal", null)?.let { json.decodeFromString<Goal>(it) }
    private fun loadTasks(): List<Task> = preferences.getString("tasks", null)?.let { json.decodeFromString<List<Task>>(it) } ?: emptyList()
}
