package com.mohanbuilds.focus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CalendarRepository(context: Context) {
    private val preferences: SharedPreferences = createEncryptedPrefs(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val _goal = MutableStateFlow(loadGoal())
    private val _tasks = MutableStateFlow(loadTasks())
    val goal: StateFlow<Goal?> = _goal.asStateFlow()
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    init {
        migrateFromPlaintext(context)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "calendar_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            // Guard against OEM Keystore failures (KeyStoreException on some
            // Samsung/Xiaomi devices): fall back to plain prefs so the app
            // stays usable instead of crashing.
            context.getSharedPreferences("calendar_fallback", Context.MODE_PRIVATE)
        }
    }

    private fun migrateFromPlaintext(context: Context) {
        val oldPrefs = context.getSharedPreferences("calendar", Context.MODE_PRIVATE)
        val goalJson = oldPrefs.getString("goal", null)
        val tasksJson = oldPrefs.getString("tasks", null)

        if (goalJson != null || tasksJson != null) {
            val editor = preferences.edit()
            goalJson?.let { editor.putString("goal", it) }
            tasksJson?.let { editor.putString("tasks", it) }
            editor.apply()

            oldPrefs.edit().clear().apply()

            _goal.value = loadGoal()
            _tasks.value = loadTasks()
        }
    }

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
    fun toggleTask(id: String) {
        saveTasks(_tasks.value.map { task ->
            if (task.id != id) task
            else if (task.subtasks.isEmpty()) task.copy(completed = !task.completed)
            else {
                val target = !task.completed
                task.copy(completed = target, subtasks = task.subtasks.map { it.copy(completed = target) })
            }
        })
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        saveTasks(_tasks.value.map { task ->
            if (task.id != taskId) task
            else {
                val subtasks = task.subtasks.map { if (it.id == subtaskId) it.copy(completed = !it.completed) else it }
                task.copy(subtasks = subtasks, completed = subtasks.isNotEmpty() && subtasks.all { it.completed })
            }
        })
    }

    fun deleteSubtask(taskId: String, subtaskId: String) {
        saveTasks(_tasks.value.map { task ->
            if (task.id != taskId) task
            else {
                val subtasks = task.subtasks.filterNot { it.id == subtaskId }
                task.copy(subtasks = subtasks, completed = subtasks.isNotEmpty() && subtasks.all { it.completed })
            }
        })
    }

    fun deleteTask(id: String) = saveTasks(_tasks.value.filterNot { it.id == id })

    fun deleteTasksWhere(predicate: (Task) -> Boolean) = saveTasks(_tasks.value.filterNot(predicate))

    fun replaceTasks(tasks: List<Task>) = saveTasks(tasks)

    private fun saveTasks(tasks: List<Task>) {
        preferences.edit().putString("tasks", json.encodeToString(tasks)).apply()
        _tasks.value = tasks
    }

    private fun loadGoal(): Goal? = preferences.getString("goal", null)?.let { json.decodeFromString<Goal>(it) }
    private fun loadTasks(): List<Task> = preferences.getString("tasks", null)?.let { json.decodeFromString<List<Task>>(it) } ?: emptyList()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun encryptedPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
                EncryptedSharedPreferences.create(
                    context, "calendar_secure", masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (e: Exception) {
                // OEM Keystore failure guard — see createEncryptedPrefs().
                context.getSharedPreferences("calendar_fallback", Context.MODE_PRIVATE)
            }
        }

        fun loadGoalSync(context: Context): Goal? {
            val prefs = encryptedPrefs(context)
            return prefs.getString("goal", null)
                ?.let { runCatching { json.decodeFromString<Goal>(it) }.getOrNull() }
        }

        fun loadTasksSync(context: Context): List<Task> {
            val prefs = encryptedPrefs(context)
            return prefs.getString("tasks", null)
                ?.let { runCatching { json.decodeFromString<List<Task>>(it) }.getOrNull() }
                ?: emptyList()
        }
    }
}
