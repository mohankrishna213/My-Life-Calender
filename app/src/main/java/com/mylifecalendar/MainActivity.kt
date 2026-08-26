package com.mylifecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mylifecalendar.data.CalendarRepository
import com.mylifecalendar.data.Goal
import com.mylifecalendar.wallpaper.LockScreenWallpaperCoordinator
import com.mylifecalendar.wallpaper.MidnightWallpaperWorker
import com.mylifecalendar.wallpaper.WallpaperPreferences
import com.mylifecalendar.wallpaper.WallpaperResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CalendarViewModel> { CalendarViewModel.factory(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MidnightWallpaperWorker.schedule(applicationContext)
        setContent { CalendarApp(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWallpaperIfApplied(this)
    }
}

data class WallpaperEvent(val id: Long, val result: WallpaperResult)

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
            _wallpaperEvent.value = WallpaperEvent(++wallpaperEventId, result)
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
            _wallpaperEvent.value = WallpaperEvent(++wallpaperEventId, result)
        }
    }

    fun consumeWallpaperEvent() { _wallpaperEvent.value = null }

    fun saveGoal(title: String, start: String, end: String) = repository.saveGoal(Goal(title.trim(), start, end))
    fun addTask(title: String, date: LocalDate) = repository.addTask(title.trim(), date.toString())
    fun toggleTask(id: Long) = repository.toggleTask(id)
    fun deleteTask(id: Long) = repository.deleteTask(id)
    companion object {
        fun factory(context: android.content.Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(CalendarRepository(context)) as T
        }
    }
}

internal val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

enum class AppScreen { Dashboard, LockScreen }

@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }
    var showGoalCreatedPrompt by rememberSaveable { mutableStateOf(false) }
    val drawerWidth = LocalConfiguration.current.screenWidthDp.dp * 0.7f

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F4EE)) {
            if (goal == null) {
                SetupScreen(viewModel, onGoalCreated = { showGoalCreatedPrompt = true })
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentScreen == AppScreen.Dashboard,
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

@Composable
private fun AppDrawerSheet(goalTitle: String, currentScreen: AppScreen, drawerWidth: Dp, onNavigate: (AppScreen) -> Unit) {
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
            Text("My Life Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
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
            NavigationDrawerItem(
                label = { Text("Settings") },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                selected = false,
                onClick = {},
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedIconColor = Color(0xFFB9B9AC),
                    unselectedTextColor = Color(0xFFB9B9AC),
                ),
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
