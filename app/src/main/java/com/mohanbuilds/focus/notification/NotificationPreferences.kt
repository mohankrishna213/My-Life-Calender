package com.mohanbuilds.focus.notification

import android.content.Context
import android.content.SharedPreferences

object NotificationPreferences {
    private const val PREFS = "notifications"
    private const val KEY_ENABLED = "enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
