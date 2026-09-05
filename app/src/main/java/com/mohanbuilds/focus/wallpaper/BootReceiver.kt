package com.mohanbuilds.focus.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohanbuilds.focus.notification.NotificationPreferences
import com.mohanbuilds.focus.notification.TaskCheckWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            MidnightWallpaperWorker.schedule(context)
            if (NotificationPreferences.isEnabled(context)) {
                TaskCheckWorker.scheduleAll(context)
            }
        }
    }
}
