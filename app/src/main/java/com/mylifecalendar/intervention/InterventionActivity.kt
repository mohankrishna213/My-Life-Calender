package com.mylifecalendar.intervention

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.mylifecalendar.CalendarViewModel
import com.mylifecalendar.MainActivity
import com.mylifecalendar.ui.intervention.InterventionHostScreen

class InterventionActivity : ComponentActivity() {

    private val viewModel by viewModels<CalendarViewModel> {
        CalendarViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: run {
            finish()
            return
        }
        val targetAppName = intent.getStringExtra(EXTRA_TARGET_APP_NAME) ?: "app"

        setContent {
            MaterialTheme {
                InterventionHostScreen(
                    targetPackage = targetPackage,
                    targetAppName = targetAppName,
                    viewModel = viewModel,
                    onStayFocused = ::stayFocused,
                    onOpenAnyway = { openTargetApp(targetPackage) },
                    onAddTask = { openMainActivity(openTasks = false) },
                    onMoreTasks = { openMainActivity(openTasks = true) },
                )
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            stayFocused()
        }
    }

    private fun stayFocused() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }

    private fun openTargetApp(targetPackage: String) {
        InterventionPreferences.recordDismissedAt(applicationContext, targetPackage)
        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent?.let { startActivity(it) }
        finish()
    }

    private fun openMainActivity(openTasks: Boolean) {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (openTasks) putExtra(EXTRA_OPEN_SCREEN, VALUE_OPEN_SCREEN_TASKS)
        }
        startActivity(appIntent)
        finish()
    }

    private companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_TARGET_APP_NAME = "target_app_name"
        const val EXTRA_OPEN_SCREEN = "open_screen"
        const val VALUE_OPEN_SCREEN_TASKS = "tasks"
    }
}
