package com.mohanbuilds.focus.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohanbuilds.focus.notification.NotificationHelper
import com.mohanbuilds.focus.notification.NotificationPreferences
import com.mohanbuilds.focus.notification.TaskCheckWorker

private const val PRIVACY_POLICY_URL = "https://mohankrishna213.github.io/My-Life-Calender/privacy-policy.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var notificationsEnabled by remember { mutableStateOf(NotificationPreferences.isEnabled(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            notificationsEnabled = true
            NotificationPreferences.setEnabled(context, true)
            TaskCheckWorker.scheduleAll(context)
        } else {
            notificationsEnabled = false
            NotificationPreferences.setEnabled(context, false)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Medium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFF7F4EE),
                titleContentColor = Color(0xFF173B2D),
                navigationIconContentColor = Color(0xFF173B2D),
            ),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).background(Color(0xFFE8F2EC), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Notifications", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF173B2D))
                Text("Daily task reminders", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5B665F))
            }
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (NotificationHelper.hasNotificationPermission(context)) {
                            notificationsEnabled = true
                            NotificationPreferences.setEnabled(context, true)
                            TaskCheckWorker.scheduleAll(context)
                        } else {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        notificationsEnabled = false
                        NotificationPreferences.setEnabled(context, false)
                        TaskCheckWorker.cancelAll(context)
                        NotificationHelper.cancelAll(context)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF2D6A4F),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD1CCC4),
                ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
            }.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).background(Color(0xFFE8F2EC), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Privacy Policy", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF173B2D))
                Text("How Focus handles your data", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5B665F))
            }
            Text("↗", color = Color(0xFF5B665F))
        }
    }
}
