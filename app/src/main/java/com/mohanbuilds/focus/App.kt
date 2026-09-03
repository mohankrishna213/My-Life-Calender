package com.mohanbuilds.focus

import android.app.Application
import com.mohanbuilds.focus.data.CalendarRepository
import com.mohanbuilds.focus.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val repository: CalendarRepository by lazy { CalendarRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        // Pre-warm the repository off the main thread: EncryptedSharedPreferences
        // triggers Android Keystore operations (200-800ms on first launch) which
        // would otherwise block MainActivity.onCreate() and risk an ANR.
        applicationScope.launch(Dispatchers.IO) { repository }
    }
}
