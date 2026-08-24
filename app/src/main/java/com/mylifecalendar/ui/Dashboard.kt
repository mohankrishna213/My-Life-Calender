package com.mylifecalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mylifecalendar.data.Goal
import com.mylifecalendar.data.Task
import com.mylifecalendar.domain.Intensity
import com.mylifecalendar.domain.color
import com.mylifecalendar.domain.calculateGridSpec
import com.mylifecalendar.domain.datesBetween
import com.mylifecalendar.domain.daysRemaining
import com.mylifecalendar.domain.summarize
import com.mylifecalendar.wallpaper.WallpaperResult
import java.time.LocalDate

@Composable
fun Dashboard(
    goal: Goal,
    tasks: List<Task>,
    viewModel: CalendarViewModel,
    wallpaperResult: WallpaperResult? = null,
    onApplyWallpaper: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val start = LocalDate.parse(goal.startDate)
    val end = LocalDate.parse(goal.endDate)
    var selectedDate by remember(goal.startDate, goal.endDate) { mutableStateOf(LocalDate.now().coerceIn(start, end)) }
    var showAddTask by remember { mutableStateOf(false) }
    var showEditGoal by remember { mutableStateOf(false) }
    val calendarDates = datesBetween(start, end)
    val selectedTasks = tasks.filter { it.date == selectedDate.toString() }

    // Keep the applied lock-screen wallpaper in sync after any goal/task change.
    LaunchedEffect(goal, tasks) {
        viewModel.refreshWallpaperIfApplied(context)
    }

    Scaffold(containerColor = Color(0xFFF7F4EE), floatingActionButton = { FloatingActionButton(onClick = { showAddTask = true }, containerColor = Color(0xFF2D6A4F), contentColor = Color.White) { Icon(Icons.Default.Add, "Add task") } }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MY LIFE CALENDAR", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
                    Text(goal.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
                }
                IconButton(onClick = { showEditGoal = true }) { Icon(Icons.Default.Edit, "Edit goal") }
            }
            Text("${daysRemaining(LocalDate.now(), end)} days left", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onApplyWallpaper,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F), contentColor = Color.White),
            ) { Text("Set lock screen wallpaper") }
            when (wallpaperResult) {
                is WallpaperResult.Success -> Text("Lock screen updated", color = Color(0xFF2D6A4F), style = MaterialTheme.typography.labelMedium)
                WallpaperResult.NoGoal -> Text("Set a goal first", color = Color(0xFF5B665F), style = MaterialTheme.typography.labelMedium)
                is WallpaperResult.Failure -> Text((wallpaperResult as WallpaperResult.Failure).message, color = Color.Red, style = MaterialTheme.typography.labelMedium)
                null -> {}
            }
            Spacer(Modifier.height(22.dp))
            Text("Your year, one day at a time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gridSpec = calculateGridSpec(calendarDates.size, maxWidth.value)
                val cellSize = gridSpec.cellSizeDp.dp
                val cellGap = gridSpec.gapDp.dp
                val gridWidth = cellSize * gridSpec.columns + cellGap * (gridSpec.columns - 1)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(Modifier.width(gridWidth), verticalArrangement = Arrangement.spacedBy(cellGap)) {
                        calendarDates.chunked(gridSpec.columns).forEach { rowDates ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cellGap, Alignment.Start)) {
                                rowDates.forEach { date ->
                                    DayCell(summarize(date, tasks).intensity, date == selectedDate, cellSize) { selectedDate = date }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(Intensity.NONE, Intensity.LOW, Intensity.MEDIUM, Intensity.HIGH, Intensity.FULL).forEach { intensity ->
                    Box(Modifier.size(14.dp).background(intensity.color(), RoundedCornerShape(3.dp)))
                }
                Text("less", style = MaterialTheme.typography.labelSmall)
                Text("more", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(24.dp))
            Text(selectedDate.format(dateFormatter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${selectedTasks.count { it.completed }} of ${selectedTasks.size} tasks complete", color = Color(0xFF5B665F))
            Spacer(Modifier.height(10.dp))
            if (selectedTasks.isEmpty()) Text("A clear day. Add one small thing to begin.", color = Color(0xFF5B665F))
            selectedTasks.forEach { task -> TaskRow(task, viewModel) }
        }
    }
    if (showAddTask) AddTaskDialog(selectedDate, { title -> viewModel.addTask(title, selectedDate); showAddTask = false }, { showAddTask = false })
    if (showEditGoal) EditGoalDialog(goal, viewModel, { showEditGoal = false })
}

@Composable
fun DayCell(intensity: Intensity, selected: Boolean, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).background(intensity.color(), RoundedCornerShape(4.dp)).clickable(onClick = onClick).then(if (selected) Modifier.padding(2.dp) else Modifier)) {
        if (selected) Box(Modifier.fillMaxSize().background(Color.Transparent, RoundedCornerShape(3.dp)))
    }
}
