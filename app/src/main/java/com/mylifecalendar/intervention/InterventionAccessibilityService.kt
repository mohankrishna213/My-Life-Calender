package com.mylifecalendar.intervention

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent

class InterventionAccessibilityService : AccessibilityService() {

    private companion object {
        val SYSTEM_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher2",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.sec.android.app.launcher",
            "com.miui.home",
            "com.huawei.android.launcher",
            "com.coloros.launcher",
            "com.vivo.launcher",
            "android",
            "com.android.settings"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (SYSTEM_PACKAGES.any { packageName.startsWith(it) }) return

        if (packageName == applicationContext.packageName) return

        if (!InterventionPreferences.isEnabled(applicationContext)) return

        val monitored = InterventionPreferences.getMonitoredPackages(applicationContext)
        if (packageName !in monitored) return

        if (InterventionPreferences.isRecentlyTriggered(applicationContext, packageName)) return

        if (InterventionPreferences.isInCooldown(applicationContext, packageName)) return

        InterventionPreferences.recordTriggeredAt(applicationContext, packageName)

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }

        val intent = Intent(applicationContext, InterventionActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra("target_package", packageName)
            putExtra("target_app_name", appName)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // No-op: required by AccessibilityService
    }
}
