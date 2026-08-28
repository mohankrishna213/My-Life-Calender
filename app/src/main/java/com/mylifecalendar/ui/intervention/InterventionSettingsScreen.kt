package com.mylifecalendar.ui.intervention

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mylifecalendar.intervention.InterventionPreferences

private enum class SettingsUiState { NeedAccessibility, Ready }

private val OEM_MANUFACTURERS = listOf(
    "samsung", "xiaomi", "redmi", "huawei", "oppo", "vivo", "oneplus", "realme"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterventionSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val isOemDevice = Build.MANUFACTURER.lowercase() in OEM_MANUFACTURERS

    var accessibilityGranted by remember {
        mutableStateOf(InterventionPreferences.isAccessibilityServiceEnabled(context))
    }
    var batteryExempt by remember {
        mutableStateOf(InterventionPreferences.isBatteryOptimizationExempt(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityGranted =
                    InterventionPreferences.isAccessibilityServiceEnabled(context)
                batteryExempt = InterventionPreferences.isBatteryOptimizationExempt(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val uiState = when {
        !accessibilityGranted -> SettingsUiState.NeedAccessibility
        else -> SettingsUiState.Ready
    }

    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = Color(0xFFF7F4EE),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text("Intervention", fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (uiState) {
                SettingsUiState.NeedAccessibility -> NeedAccessibilityContent(context)
                SettingsUiState.Ready -> ReadyContent(
                    context = context,
                    isOemDevice = isOemDevice,
                    batteryExempt = batteryExempt,
                )
            }
        }
    }
}

@Composable
private fun NeedAccessibilityContent(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Focus on what matters",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF173B2D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "When you open Instagram (or any app you choose), My Life Calendar " +
                        "shows a brief pause — your tasks, a chance to stay on track.",
                    fontSize = 14.sp,
                    color = Color(0xFF5B665F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE0DDD6))
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "What this permission does",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF173B2D),
                )
                Spacer(Modifier.height(8.dp))

                BulletRow(Icons.Outlined.Check, "Detects which app you're opening", Color(0xFF2D6A4F))
                BulletRow(Icons.Outlined.Check, "Shows a pause screen from My Life Calendar", Color(0xFF2D6A4F))
                BulletRow(Icons.Outlined.Close, "Does NOT read your screen content", Color(0xFF9B9B8A))
                BulletRow(Icons.Outlined.Close, "Does NOT collect or share any data", Color(0xFF9B9B8A))

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "How to enable:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF173B2D),
                )
                Spacer(Modifier.height(8.dp))

                NumberedStep(1, "Tap the button below to open Android's Accessibility Settings")
                NumberedStep(2, "Scroll to \"Downloaded services\" at the bottom and tap \"My Life Calendar\"")
                NumberedStep(3, "Turn on \"Use My Life Calendar\". Leave the shortcut OFF.")
                NumberedStep(4, "Come back to this screen — it will update automatically.")

                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E7)),
                    border = BorderStroke(0.5.dp, Color(0xFFF5D978)),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFFB7890C),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Android will show a security warning about accessibility. " +
                                "This is a standard Android notice shown for all such apps — " +
                                "My Life Calendar does not read or store your screen content.",
                            fontSize = 12.sp,
                            color = Color(0xFF7A5C0A),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                ) {
                    Text("Enable in Accessibility Settings", color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "After enabling, come back here — the screen will update automatically.",
            fontSize = 12.sp,
            color = Color(0xFF5B665F),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ReadyContent(
    context: Context,
    isOemDevice: Boolean,
    batteryExempt: Boolean,
) {
    var refreshKey by remember { mutableStateOf(0) }
    var isEnabled by remember(refreshKey) {
        mutableStateOf(InterventionPreferences.isEnabled(context))
    }
    val monitoredPackages = remember(refreshKey) {
        InterventionPreferences.getMonitoredPackages(context)
    }
    var cooldownMinutes by remember(refreshKey) {
        mutableStateOf(InterventionPreferences.getCooldownMinutes(context))
    }
    var showAppPicker by remember { mutableStateOf(false) }
    var showCooldownPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = cardColorsWhite()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Enable intervention",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF173B2D),
                        )
                        Text(
                            text = "Pause screen shows when you open monitored apps",
                            fontSize = 12.sp,
                            color = Color(0xFF5B665F),
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            InterventionPreferences.setEnabled(context, it)
                            isEnabled = it
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (isOemDevice) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = cardColorsWhite()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = if (batteryExempt) Icons.Outlined.Check else Icons.Outlined.BatteryAlert,
                            contentDescription = null,
                            tint = if (batteryExempt) Color(0xFF2D6A4F) else Color(0xFFB7890C),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Background running",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF173B2D),
                            )
                            Text(
                                text = if (batteryExempt) "Exempt — intervention will work reliably ✓"
                                else "Not exempt — intervention may stop after a while",
                                fontSize = 12.sp,
                                color = if (batteryExempt) Color(0xFF2D6A4F) else Color(0xFFB7890C),
                            )
                            if (!batteryExempt) {
                                Text(
                                    text = "Tap Fix, or open system Settings → Apps → My Life Calendar → " +
                                        "Battery and set \"Allow background usage\" / Unrestricted" +
                                        " (plus Autostart on Xiaomi).",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9B9B8A),
                                )
                            }
                        }
                        if (!batteryExempt) {
                            TextButton(onClick = { requestIgnoreBatteryOptimizations(context) }) {
                                Text("Fix", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        item {
            SectionHeader("MONITORED APPS")
            Spacer(Modifier.height(8.dp))
        }

        val sortedMonitored = monitoredPackages.sortedBy { pkg ->
            resolveAppName(context, pkg).lowercase()
        }
        items(sortedMonitored, key = { it }) { packageName ->
            MonitoredAppRow(
                context = context,
                packageName = packageName,
                onRemove = {
                    InterventionPreferences.removeMonitoredPackage(context, packageName)
                    refreshKey++
                },
            )
            Spacer(Modifier.height(4.dp))
        }

        item {
            Spacer(Modifier.height(8.dp))
            Card(
                border = BorderStroke(1.dp, Color(0xFF2D6A4F)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAppPicker = true },
            ) {
                Text(
                    text = "+ Add an app",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D6A4F),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SectionHeader("PAUSE AFTER DISMISSAL")
            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = cardColorsWhite(),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCooldownPicker = true },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Don't show again for",
                        fontSize = 14.sp,
                        color = Color(0xFF173B2D),
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = "${cooldownMinutes} min", fontSize = 14.sp, color = Color(0xFF2D6A4F))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF9B9B8A),
                    )
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            context = context,
            monitoredPackages = monitoredPackages,
            onDismiss = { showAppPicker = false },
            onSelect = { packageName ->
                InterventionPreferences.addMonitoredPackage(context, packageName)
                showAppPicker = false
                refreshKey++
            },
        )
    }

    if (showCooldownPicker) {
        CooldownPickerDialog(
            currentMinutes = cooldownMinutes,
            onDismiss = { showCooldownPicker = false },
            onSelect = { minutes ->
                InterventionPreferences.setCooldownMinutes(context, minutes)
                cooldownMinutes = minutes
                showCooldownPicker = false
            },
        )
    }
}

@Composable
private fun MonitoredAppRow(
    context: Context,
    packageName: String,
    onRemove: () -> Unit,
) {
    val appName = remember(packageName) { resolveAppName(context, packageName) }
    val appIconBitmap: Bitmap? = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName).toBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Card(shape = RoundedCornerShape(12.dp), colors = cardColorsWhite()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            AppIcon(bitmap = appIconBitmap, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = appName,
                fontSize = 14.sp,
                color = Color(0xFF173B2D),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Remove $appName",
                    tint = Color(0xFFCC4444),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun AppPickerDialog(
    context: Context,
    monitoredPackages: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val pm = context.packageManager
    val installedApps: List<ApplicationInfo> = remember {
        try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .filterNot { it.packageName == context.packageName }
                .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add an app", fontWeight = FontWeight.Bold, color = Color(0xFF173B2D)) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(installedApps, key = { it.packageName }) { appInfo ->
                    val alreadyAdded = appInfo.packageName in monitoredPackages
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !alreadyAdded) {
                                if (!alreadyAdded) onSelect(appInfo.packageName)
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        val iconBitmap: Bitmap? = remember(appInfo.packageName) {
                            try {
                                pm.getApplicationIcon(appInfo.packageName).toBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        AppIcon(bitmap = iconBitmap, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = pm.getApplicationLabel(appInfo).toString(),
                            fontSize = 14.sp,
                            color = Color(0xFF173B2D),
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        if (alreadyAdded) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Already added",
                                tint = Color(0xFF2D6A4F),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun CooldownPickerDialog(
    currentMinutes: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val options = listOf(
        15L to "15 minutes",
        30L to "30 minutes",
        60L to "1 hour",
        120L to "2 hours",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Don't show again for", fontWeight = FontWeight.Bold, color = Color(0xFF173B2D)) },
        text = {
            Column {
                options.forEach { (minutes, label) ->
                    RadioButtonRow(
                        label = label,
                        selected = currentMinutes == minutes,
                        onClick = { onSelect(minutes) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun RadioButtonRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text = label, fontSize = 14.sp, color = Color(0xFF173B2D))
    }
}

@Composable
private fun BulletRow(icon: ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = text, fontSize = 13.sp, color = Color(0xFF5B665F))
    }
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF2D6A4F)),
        ) {
            Text(text = number.toString(), fontSize = 11.sp, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(text = text, fontSize = 13.sp, color = Color(0xFF5B665F))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        color = Color(0xFF9B9B8A),
    )
}

@Composable
private fun AppIcon(bitmap: Bitmap?, size: androidx.compose.ui.unit.Dp) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE0DDD6)),
        )
    }
}

@Composable
private fun cardColorsWhite() = CardDefaults.cardColors(containerColor = Color.White)

private fun resolveAppName(context: Context, packageName: String): String {
    return try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}
