package com.mohanbuilds.focus.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import com.mohanbuilds.focus.data.CalendarRepository
import com.mohanbuilds.focus.domain.WallpaperSnapshotFactory
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of an attempt to apply the lock-screen wallpaper. */
sealed class WallpaperResult {
    data object Success : WallpaperResult()
    data object NoGoal : WallpaperResult()
    data class Failure(val message: String) : WallpaperResult()
}

/**
 * Loads current goal/task state, renders the dashboard bitmap, and applies it
 * to the lock screen only (FLAG_LOCK). The home wallpaper is never touched.
 */
class LockScreenWallpaperCoordinator(
    private val repository: CalendarRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    suspend fun apply(context: Context): WallpaperResult = withContext(dispatcher) {
        val goal = repository.goal.value ?: return@withContext WallpaperResult.NoGoal
        val snapshot = WallpaperSnapshotFactory.create(goal, repository.tasks.value, LocalDate.now())
            ?: return@withContext WallpaperResult.NoGoal

        val manager = WallpaperManager.getInstance(context)
        val size = displaySize(context)

        val bitmap = runCatching {
            LockScreenWallpaperRenderer.render(snapshot, size.x, size.y)
        }.getOrElse { return@withContext WallpaperResult.Failure(it.message ?: "Render failed") }

        val setResult = runCatching {
            manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        }
        if (!bitmap.isRecycled) bitmap.recycle()

        setResult.fold(
            onSuccess = { WallpaperResult.Success },
            onFailure = { WallpaperResult.Failure(it.message ?: "Could not set lock wallpaper") },
        )
    }

    suspend fun remove(context: Context): WallpaperResult = withContext(dispatcher) {
        val manager = WallpaperManager.getInstance(context)

        if (!manager.isSetWallpaperAllowed) {
            return@withContext WallpaperResult.Failure(
                "Your device policy doesn't allow changing the wallpaper."
            )
        }

        val clearResult = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.clear(WallpaperManager.FLAG_LOCK)
            }
        }

        clearResult.fold(
            onSuccess = { WallpaperResult.Success },
            onFailure = { cause ->
                when (cause) {
                    is SecurityException -> WallpaperResult.Failure(
                        "Your device (likely Samsung or a custom skin) manages the lock screen " +
                        "separately. Please go to Settings → Wallpaper to remove it manually."
                    )
                    else -> WallpaperResult.Failure(
                        cause.message ?: "Could not remove the lock screen wallpaper."
                    )
                }
            }
        )
    }

    private fun displaySize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            point.x = bounds.width()
            point.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
        }
        return point
    }
}
