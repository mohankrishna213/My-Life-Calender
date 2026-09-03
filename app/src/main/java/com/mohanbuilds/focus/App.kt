package com.mohanbuilds.focus

import android.app.Application
import com.mohanbuilds.focus.data.CalendarRepository
import com.mohanbuilds.focus.notification.NotificationHelper

class App : Application() {
    val repository: CalendarRepository by lazy { CalendarRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
