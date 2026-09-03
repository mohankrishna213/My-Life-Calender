package com.mohanbuilds.focus.wallpaper

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mohanbuilds.focus.App
import com.mohanbuilds.focus.data.CalendarRepository
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
        val repository = (applicationContext as App).repository
        val coordinator = LockScreenWallpaperCoordinator(repository)
        return when (coordinator.apply(applicationContext)) {
            is WallpaperResult.Success -> {
                WallpaperPreferences.setLastRefreshDate(applicationContext, LocalDate.now())
                WallpaperPreferences.setLastAppliedNow(applicationContext)
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
                ExistingPeriodicWorkPolicy.KEEP,
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

    private fun prefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "wallpaper_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private var migrated = false

    private fun ensureMigrated(context: Context) {
        if (migrated) return
        migrated = true

        val oldPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val applied = oldPrefs.getBoolean(KEY_APPLIED, false)
        val lastRefresh = oldPrefs.getString(KEY_LAST_REFRESH, null)
        val lastAppliedAt = oldPrefs.getLong(KEY_LAST_APPLIED_AT, -1L)

        if (applied || lastRefresh != null || lastAppliedAt != -1L) {
            val editor = prefs(context).edit()
            if (applied) editor.putBoolean(KEY_APPLIED, applied)
            lastRefresh?.let { editor.putString(KEY_LAST_REFRESH, it) }
            if (lastAppliedAt != -1L) editor.putLong(KEY_LAST_APPLIED_AT, lastAppliedAt)
            editor.apply()

            oldPrefs.edit().clear().apply()
        }
    }

    fun isWallpaperApplied(context: Context): Boolean {
        ensureMigrated(context)
        return prefs(context).getBoolean(KEY_APPLIED, false)
    }

    fun setWallpaperApplied(context: Context, applied: Boolean) {
        ensureMigrated(context)
        prefs(context).edit().putBoolean(KEY_APPLIED, applied).apply()
    }

    fun setLastRefreshDate(context: Context, date: LocalDate) {
        ensureMigrated(context)
        prefs(context).edit().putString(KEY_LAST_REFRESH, date.toString()).apply()
    }

    fun setLastAppliedNow(context: Context) {
        ensureMigrated(context)
        prefs(context).edit().putLong(KEY_LAST_APPLIED_AT, System.currentTimeMillis()).apply()
    }

    fun getLastAppliedAt(context: Context): Long? {
        ensureMigrated(context)
        val value = prefs(context).getLong(KEY_LAST_APPLIED_AT, -1L)
        return if (value == -1L) null else value
    }
}
