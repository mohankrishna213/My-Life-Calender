package com.mohanbuilds.focus.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mohanbuilds.focus.MainActivity
import com.mohanbuilds.focus.R

object NotificationHelper {
    const val CHANNEL_ID = "focus_notifications"

    /** True when this app may post notifications: always true on API < 33
     *  (no runtime permission exists), otherwise requires POST_NOTIFICATIONS
     *  to have been granted by the system dialog. */
    fun hasNotificationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily task reminders and motivation"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        buttonText: String? = null,
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.focus)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (buttonText != null) {
            builder.addAction(0, buttonText, pendingIntent)
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, builder.build())
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }
}
