package com.mylifecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {
    val goal = repository.goal
    val tasks = repository.tasks

    private val _wallpaperResult = MutableStateFlow<WallpaperResult?>(null)
    val wallpaperResult: StateFlow<WallpaperResult?> = _wallpaperResult.asStateFlow()

    private val coordinator by lazy { LockScreenWallpaperCoordinator(repository) }

    /** Explicit user action: render and apply the lock-screen wallpaper now. */
    fun applyWallpaper(context: android.content.Context) {
        viewModelScope.launch {
            _wallpaperResult.value = coordinator.apply(context)
            if (_wallpaperResult.value is WallpaperResult.Success) {
                WallpaperPreferences.setWallpaperApplied(context, true)
            }
        }
    }

    /** Silent refresh on mutations/resume; only when the user already applied it. */
    fun refreshWallpaperIfApplied(context: android.content.Context) {
        if (!WallpaperPreferences.isWallpaperApplied(context)) return
        viewModelScope.launch { coordinator.apply(context) }
    }

    fun consumeWallpaperResult() { _wallpaperResult.value = null }

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

@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val wallpaperResult by viewModel.wallpaperResult.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Surface apply success/failure briefly after an explicit action.
    LaunchedEffect(wallpaperResult) {
        if (wallpaperResult != null) {
            kotlinx.coroutines.delay(2500)
            viewModel.consumeWallpaperResult()
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F4EE)) {
            if (goal == null) SetupScreen(viewModel)
            else Dashboard(goal!!, tasks, viewModel, wallpaperResult, onApplyWallpaper = { viewModel.applyWallpaper(context) })
        }
    }
}
