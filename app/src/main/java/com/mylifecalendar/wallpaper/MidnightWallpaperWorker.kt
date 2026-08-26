package com.mylifecalendar.wallpaper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mylifecalendar.data.CalendarRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Reapplies the lock-screen wallpaper once per day so the countdown, date,
 * and today ring advance across midnight. Only refreshes when a wallpaper was
 * previously applied by the user.
 */
class MidnightWallpaperWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!WallpaperPreferences.isWallpaperApplied(applicationContext)) {
            return Result.success()
        }
        val repository = CalendarRepository(applicationContext)
        val coordinator = LockScreenWallpaperCoordinator(repository)
        return when (coordinator.apply(applicationContext)) {
            is WallpaperResult.Success -> {
                WallpaperPreferences.setLastRefreshDate(applicationContext, LocalDate.now())
                WallpaperPreferences.setLastAppliedNow(applicationContext)
                schedule(applicationContext)
                Result.success()
            }
            else -> Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "lock_screen_wallpaper_midnight"

        /** Schedules the daily refresh, replacing any previous schedule. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MidnightWallpaperWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayUntilNextMidnight())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        internal fun initialDelayUntilNextMidnight(now: LocalDateTime = LocalDateTime.now()): Duration {
            val nextMidnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)
            return Duration.between(now, nextMidnight)
        }
    }
}

/** Small SharedPreferences helper tracking whether the user applied the wallpaper. */
object WallpaperPreferences {
    private const val PREFS = "wallpaper"
    private const val KEY_APPLIED = "applied"
    private const val KEY_LAST_REFRESH = "last_refresh"
    private const val KEY_LAST_APPLIED_AT = "last_applied_at"

    fun isWallpaperApplied(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_APPLIED, false)

    fun setWallpaperApplied(context: Context, applied: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_APPLIED, applied).apply()
    }

    fun setLastRefreshDate(context: Context, date: LocalDate) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_REFRESH, date.toString()).apply()
    }

    fun setLastAppliedNow(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_APPLIED_AT, System.currentTimeMillis()).apply()
    }

    fun getLastAppliedAt(context: Context): Long? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_APPLIED_AT, -1L)
        return if (value == -1L) null else value
    }
}
