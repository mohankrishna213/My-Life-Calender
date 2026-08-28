package com.mylifecalendar.intervention

import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings

object InterventionPreferences {
    private const val PREFS_NAME = "intervention_prefs"

    private const val KEY_ENABLED = "enabled"
    private const val KEY_MONITORED_PACKAGES = "monitored_packages"
    private const val KEY_COOLDOWN_MINUTES = "cooldown_minutes"
    private const val KEY_DISMISS_TIME_PREFIX = "dismiss_time_"
    private const val KEY_TRIGGER_DEBOUNCE_PREFIX = "trigger_time_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getMonitoredPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_MONITORED_PACKAGES, emptySet()) ?: emptySet()

    fun setMonitoredPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_MONITORED_PACKAGES, packages.toSet()).apply()
    }

    fun addMonitoredPackage(context: Context, packageName: String) {
        val updated = getMonitoredPackages(context).toMutableSet()
        updated.add(packageName)
        setMonitoredPackages(context, updated)
    }

    fun removeMonitoredPackage(context: Context, packageName: String) {
        val updated = getMonitoredPackages(context).toMutableSet()
        updated.remove(packageName)
        setMonitoredPackages(context, updated)
    }

    fun getCooldownMinutes(context: Context): Long =
        prefs(context).getLong(KEY_COOLDOWN_MINUTES, 30L)

    fun setCooldownMinutes(context: Context, minutes: Long) {
        prefs(context).edit().putLong(KEY_COOLDOWN_MINUTES, minutes).apply()
    }

    fun recordDismissedAt(context: Context, packageName: String) {
        prefs(context).edit()
            .putLong(KEY_DISMISS_TIME_PREFIX + packageName, System.currentTimeMillis())
            .apply()
    }

    fun isInCooldown(context: Context, packageName: String): Boolean {
        val dismissTime = prefs(context).getLong(KEY_DISMISS_TIME_PREFIX + packageName, -1L)
        if (dismissTime < 0L) return false
        return System.currentTimeMillis() - dismissTime < getCooldownMinutes(context) * 60_000
    }

    fun recordTriggeredAt(context: Context, packageName: String) {
        prefs(context).edit()
            .putLong(KEY_TRIGGER_DEBOUNCE_PREFIX + packageName, System.currentTimeMillis())
            .apply()
    }

    fun isRecentlyTriggered(context: Context, packageName: String): Boolean {
        val lastTriggered = prefs(context).getLong(KEY_TRIGGER_DEBOUNCE_PREFIX + packageName, -1L)
        if (lastTriggered < 0L) return false
        return System.currentTimeMillis() - lastTriggered < 3_000
    }

    fun isBatteryOptimizationExempt(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(
            context,
            InterventionAccessibilityService::class.java
        )
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices
            .split(":")
            .any { it.equals(expectedComponentName.flattenToString(), ignoreCase = true) }
    }
}
