package com.mohanbuilds.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mohanbuilds.focus.data.CalendarRepository
import com.mohanbuilds.focus.data.Goal
import com.mohanbuilds.focus.data.Recurrence
import com.mohanbuilds.focus.data.Subtask
import com.mohanbuilds.focus.data.Task
import com.mohanbuilds.focus.domain.expandTask
import com.mohanbuilds.focus.domain.isSameSeriesAs
import com.mohanbuilds.focus.ui.Dashboard
import com.mohanbuilds.focus.ui.NotificationPermissionDialog
import com.mohanbuilds.focus.ui.RecurringChangeScope
import com.mohanbuilds.focus.ui.SettingsScreen
import com.mohanbuilds.focus.ui.SetupScreen
import com.mohanbuilds.focus.ui.TaskDraft
import com.mohanbuilds.focus.ui.WallpaperPromptDialog
import com.mohanbuilds.focus.ui.WallpaperScreen
import com.mohanbuilds.focus.notification.NotificationHelper
import com.mohanbuilds.focus.notification.NotificationPreferences
import com.mohanbuilds.focus.notification.TaskCheckWorker
import com.mohanbuilds.focus.wallpaper.LockScreenWallpaperCoordinator
import com.mohanbuilds.focus.wallpaper.MidnightWallpaperWorker
import com.mohanbuilds.focus.wallpaper.WallpaperPreferences
import com.mohanbuilds.focus.wallpaper.WallpaperResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CalendarViewModel> { CalendarViewModel.factory(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MidnightWallpaperWorker.schedule(applicationContext)
        if (NotificationPreferences.isEnabled(applicationContext)) {
            TaskCheckWorker.scheduleAll(applicationContext)
        }
        setContent { CalendarApp(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWallpaperIfApplied(this)
    }
}

enum class WallpaperAction { Apply, Remove }

data class WallpaperEvent(val id: Long, val result: WallpaperResult, val action: WallpaperAction)

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {
    val goal = repository.goal
    val tasks = repository.tasks

    private var wallpaperEventId = 0L
    private val _wallpaperEvent = MutableStateFlow<WallpaperEvent?>(null)
    val wallpaperEvent: StateFlow<WallpaperEvent?> = _wallpaperEvent.asStateFlow()

    private val coordinator by lazy { LockScreenWallpaperCoordinator(repository) }

    /** Explicit user action: render and apply the lock-screen wallpaper now. */
    fun applyWallpaper(context: android.content.Context) {
        viewModelScope.launch {
            val result = coordinator.apply(context)
            if (result is WallpaperResult.Success) {
                WallpaperPreferences.setWallpaperApplied(context, true)
                WallpaperPreferences.setLastAppliedNow(context)
            }
            _wallpaperEvent.value = WallpaperEvent(++wallpaperEventId, result, WallpaperAction.Apply)
        }
    }

    /** Silent refresh on mutations/resume; only when the user already applied it. */
    fun refreshWallpaperIfApplied(context: android.content.Context) {
        if (!WallpaperPreferences.isWallpaperApplied(context)) return
        viewModelScope.launch {
            val result = coordinator.apply(context)
            if (result is WallpaperResult.Success) {
                WallpaperPreferences.setLastAppliedNow(context)
            }
        }
    }

    fun removeWallpaper(context: android.content.Context) {
        viewModelScope.launch {
            val result = coordinator.remove(context)
            if (result is WallpaperResult.Success) {
                WallpaperPreferences.setWallpaperApplied(context, false)
            }
            _wallpaperEvent.value = WallpaperEvent(++wallpaperEventId, result, WallpaperAction.Remove)
        }
    }

    fun consumeWallpaperEvent() { _wallpaperEvent.value = null }

    fun saveGoal(title: String, start: String, end: String) = repository.saveGoal(Goal(title.trim(), start, end))

    fun addTaskFromDraft(date: LocalDate, draft: TaskDraft) {
        repository.addTasks(expandTask(draft.title, date, draft.recurrence, draft.until))
    }

    fun updateTaskFromDraft(
        existing: Task,
        draft: TaskDraft,
        scope: RecurringChangeScope = RecurringChangeScope.THIS_TASK,
    ) {
        val completed = existing.subtasks.let { subtasks ->
            if (subtasks.isEmpty()) existing.completed else subtasks.all { it.completed }
        }
        val updated = existing.copy(
            title = draft.title,
            recurrence = draft.recurrence,
            recurrenceUntil = draft.until?.toString(),
            completed = completed,
        )
        val editDate = runCatching { LocalDate.parse(existing.date) }.getOrNull()
        if (existing.recurrence == Recurrence.NONE || scope == RecurringChangeScope.THIS_TASK || editDate == null) {
            repository.updateTask(updated)
            return
        }
        // Segment semantics: everything from the edit date on becomes a new series
        // (new seriesId) reshaped by the draft; earlier occurrences stay untouched.
        val tasks = repository.tasks.value
        val kept = tasks.filterNot { task ->
            task.id == existing.id || (task.isSameSeriesAs(existing) && task.date > editDate.toString())
        }
        val newFuture = if (draft.recurrence == Recurrence.NONE || draft.until == null) {
            emptyList()
        } else {
            expandTask(draft.title, editDate, draft.recurrence, draft.until)
                .filterNot { it.date == editDate.toString() }
        }
        repository.replaceTasks(kept + updated.copy(seriesId = UUID.randomUUID().toString()) + newFuture)
    }

    fun toggleTask(id: String) = repository.toggleTask(id)
    fun toggleSubtask(taskId: String, subtaskId: String) = repository.toggleSubtask(taskId, subtaskId)
    fun deleteSubtask(taskId: String, subtaskId: String) = repository.deleteSubtask(taskId, subtaskId)

    fun addSubtask(taskId: String, title: String) {
        val newSubtask = Subtask(UUID.randomUUID().toString(), title.trim())
        val tasks = repository.tasks.value.map { task ->
            if (task.id != taskId) task
            else task.copy(subtasks = task.subtasks + newSubtask)
        }
        repository.replaceTasks(tasks)
    }
    fun deleteTask(task: Task, scope: RecurringChangeScope = RecurringChangeScope.THIS_TASK) {
        if (task.recurrence == Recurrence.NONE || scope == RecurringChangeScope.THIS_TASK) {
            repository.deleteTask(task.id)
            return
        }
        repository.deleteTasksWhere { it.isSameSeriesAs(task) && it.date >= task.date }
    }
    companion object {
        fun factory(context: android.content.Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                check(modelClass == CalendarViewModel::class.java) {
                    "Unknown ViewModel class: ${modelClass.name}"
                }
                return CalendarViewModel((context.applicationContext as App).repository) as T
            }
        }
    }
}

internal val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

enum class AppScreen { Dashboard, LockScreen, Settings }

@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }
    var showGoalCreatedPrompt by rememberSaveable { mutableStateOf(false) }
    val drawerWidth = LocalConfiguration.current.screenWidthDp.dp * 0.7f

    // Tapping white space or static text must drop input focus (and the soft
    // keyboard) instead of leaving the caret blinking in a text box.
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val context = LocalContext.current

    // First-launch notification permission flow
    var showNotificationPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionRequested = true
        if (granted) {
            NotificationPreferences.setEnabled(context, true)
            TaskCheckWorker.scheduleAll(context)
        }
        showGoalCreatedPrompt = true
    }

    MaterialTheme {
        // Hide the cursor-handle knob ("bubble") on every text field in the
        // app. The handle colour is set to transparent; the selection-
        // highlight colour is kept so the user can still see selected text.
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = Color.Transparent,
                backgroundColor = Color(0xFF2D6A4F).copy(alpha = 0.4f),
            ),
        ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                },
            color = Color(0xFFF7F4EE),
        ) {
            if (goal == null) {
                SetupScreen(viewModel, onGoalCreated = {
                    if (android.os.Build.VERSION.SDK_INT >= 33) {
                        showNotificationPermissionDialog = true
                    } else {
                        showGoalCreatedPrompt = true
                    }
                })
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    drawerContent = {
                        AppDrawerSheet(
                            goalTitle = goal!!.title,
                            currentScreen = currentScreen,
                            drawerWidth = drawerWidth,
                            onNavigate = { screen ->
                                currentScreen = screen
                                scope.launch { drawerState.close() }
                            },
                        )
                    },
                ) {
                    when (currentScreen) {
                        AppScreen.Dashboard -> Dashboard(
                            goal!!,
                            tasks,
                            viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenLockScreen = { currentScreen = AppScreen.LockScreen },
                        )
                        AppScreen.LockScreen -> WallpaperScreen(
                            viewModel,
                            onBack = { currentScreen = AppScreen.Dashboard },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                        )
                        AppScreen.Settings -> SettingsScreen(
                            onBack = { currentScreen = AppScreen.Dashboard },
                        )
                    }
                    if (showNotificationPermissionDialog && !notificationPermissionRequested) {
                        NotificationPermissionDialog(
                            onAllow = {
                                showNotificationPermissionDialog = false
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onDismiss = {
                                showNotificationPermissionDialog = false
                                notificationPermissionRequested = true
                                // "No" is an explicit opt-out — keep notifications
                                // disabled so the Settings toggle stays off.
                                NotificationPreferences.setEnabled(context, false)
                                showGoalCreatedPrompt = true
                            },
                        )
                    }
                    if (showGoalCreatedPrompt) {
                        WallpaperPromptDialog(
                            title = "Goal created",
                            onConfirm = {
                                showGoalCreatedPrompt = false
                                currentScreen = AppScreen.LockScreen
                            },
                            onDismiss = { showGoalCreatedPrompt = false },
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AppDrawerSheet(
    goalTitle: String,
    currentScreen: AppScreen,
    drawerWidth: Dp,
    onNavigate: (AppScreen) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerContainerColor = Color(0xFFFAFAF7),
    ) {
        Column(Modifier.padding(horizontal = 14.dp)) {
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.size(44.dp).background(Color(0xFF2D6A4F), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text("Focus", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
            Text(goalTitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5B665F), maxLines = 1)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFE0DDD6))
            Spacer(Modifier.height(10.dp))
            NavigationDrawerItem(
                label = { Text("Dashboard") },
                icon = { Icon(Icons.Outlined.Dashboard, contentDescription = null) },
                selected = currentScreen == AppScreen.Dashboard,
                onClick = { onNavigate(AppScreen.Dashboard) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFFE8F2EC),
                    selectedIconColor = Color(0xFF2D6A4F),
                    selectedTextColor = Color(0xFF173B2D),
                ),
            )
            NavigationDrawerItem(
                label = { Text("Lock Screen") },
                icon = { Icon(Icons.Outlined.PhoneAndroid, contentDescription = null) },
                selected = currentScreen == AppScreen.LockScreen,
                onClick = { onNavigate(AppScreen.LockScreen) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = Color(0xFFE0DDD6))
            Spacer(Modifier.height(10.dp))
            NavigationDrawerItem(
                label = { Text("Settings") },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                selected = currentScreen == AppScreen.Settings,
                onClick = { onNavigate(AppScreen.Settings) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}
