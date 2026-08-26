package com.mylifecalendar

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mylifecalendar.data.Goal
import com.mylifecalendar.data.Task
import com.mylifecalendar.domain.WallpaperSnapshotFactory
import com.mylifecalendar.wallpaper.LockScreenWallpaperRenderer
import com.mylifecalendar.wallpaper.WallpaperPreferences
import com.mylifecalendar.wallpaper.WallpaperResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val goalState = viewModel.goal.collectAsStateWithLifecycle()
    val tasks = viewModel.tasks.collectAsStateWithLifecycle().value
    val goal = goalState.value ?: return
    val event by viewModel.wallpaperEvent.collectAsStateWithLifecycle()

    var applied by remember { mutableStateOf(WallpaperPreferences.isWallpaperApplied(context)) }
    var lastAppliedAt by remember { mutableStateOf(WallpaperPreferences.getLastAppliedAt(context)) }

    BackHandler(onBack = onBack)

    LaunchedEffect(event?.id) {
        val result = event?.result ?: return@LaunchedEffect
        applied = WallpaperPreferences.isWallpaperApplied(context)
        lastAppliedAt = WallpaperPreferences.getLastAppliedAt(context)
        val message = when (result) {
            WallpaperResult.Success -> "Lock screen updated ✓"
            WallpaperResult.NoGoal -> "Set a goal first"
            is WallpaperResult.Failure -> result.message
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeWallpaperEvent()
    }

    Scaffold(
        containerColor = Color(0xFFF7F4EE),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF173B2D),
                    contentColor = Color.White,
                    actionColor = Color(0xFF74C69D),
                    dismissActionContentColor = Color(0xFF74C69D),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                title = { Text("Lock Screen", fontWeight = FontWeight.Bold, color = Color(0xFF173B2D)) },
                actions = {
                    StatusChip(applied)
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F4EE),
                    scrolledContainerColor = Color(0xFFF7F4EE),
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(lastUpdatedLabel(applied, lastAppliedAt), style = MaterialTheme.typography.labelMedium, color = Color(0xFF9B9B8A), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PhonePreview(goal, tasks)
            Spacer(Modifier.height(12.dp))
            Text("Preview updates automatically when tasks change", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9B9B8A), textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.applyWallpaper(context) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F), contentColor = Color.White),
            ) { Text("Apply to Lock Screen") }
            if (applied) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.removeWallpaper(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFFCC6666)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF884444)),
                ) { Text("Remove wallpaper") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PhonePreview(goal: Goal, tasks: List<Task>) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val previewWidth = min(maxWidth * 0.55f, 220.dp)
        val previewHeight = previewWidth * 2.1f
        val snapshot = WallpaperSnapshotFactory.create(goal, tasks, LocalDate.now())
        val previewWidthPx = with(density) { previewWidth.toPx() }.toInt().coerceAtLeast(1)
        val previewHeightPx = with(density) { previewHeight.toPx() }.toInt().coerceAtLeast(1)
        val bitmapState = produceState<Bitmap?>(null, snapshot, previewWidth) {
            val snap = snapshot ?: return@produceState
            value = withContext(Dispatchers.Default) {
                LockScreenWallpaperRenderer.render(snap, previewWidthPx, previewHeightPx)
            }
        }
        Box(
            Modifier
                .size(previewWidth, previewHeight)
                .border(2.dp, Color(0xFF30363D), RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0D1117)),
            contentAlignment = Alignment.Center,
        ) {
            val bitmap = bitmapState.value
            if (bitmap == null) {
                CircularProgressIndicator(color = Color(0xFF39D353))
            } else {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Lock screen preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun StatusChip(applied: Boolean) {
    Surface(shape = RoundedCornerShape(12.dp), color = if (applied) Color(0xFFE8F2EC) else Color(0xFFEBEAE3)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).background(if (applied) Color(0xFF2D6A4F) else Color(0xFF9B9B8A), CircleShape))
            Spacer(Modifier.width(4.dp))
            Text(
                if (applied) "Active" else "Not set",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (applied) Color(0xFF2D6A4F) else Color(0xFF6B6B60),
            )
        }
    }
}

private fun lastUpdatedLabel(applied: Boolean, lastAppliedAt: Long?): String {
    if (!applied || lastAppliedAt == null) return "Never applied"
    val dateTime = Instant.ofEpochMilli(lastAppliedAt).atZone(ZoneId.systemDefault())
    val date = dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    val time = dateTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    return "Last updated $date · $time"
}
