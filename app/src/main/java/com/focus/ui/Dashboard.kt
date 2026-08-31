package com.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focus.data.Goal
import com.focus.data.Recurrence
import com.focus.data.Task
import com.focus.domain.Intensity
import com.focus.domain.calculateGridSpec
import com.focus.domain.color
import com.focus.domain.datesBetween
import com.focus.domain.daysRemaining
import com.focus.domain.summarizeByDate
import java.time.LocalDate
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dashboard(
    goal: Goal,
    tasks: List<Task>,
    viewModel: CalendarViewModel,
    onOpenDrawer: () -> Unit = {},
    onOpenLockScreen: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val startEnd = remember(goal) { LocalDate.parse(goal.startDate) to LocalDate.parse(goal.endDate) }
    val start = startEnd.first
    val end = startEnd.second
    val today = LocalDate.now()
    var selectedDate by remember(start, end) { mutableStateOf(today.coerceIn(start, end)) }
    var showAddTask by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var pendingRecurringEdit by remember { mutableStateOf<Pair<Task, TaskDraft>?>(null) }
    var showEditGoal by remember { mutableStateOf(false) }
    var showWallpaperPrompt by remember { mutableStateOf(false) }
    var expandedTaskId by remember { mutableStateOf<Long?>(null) }

    // Memoize everything the calendar grid needs so scrolling and unrelated
    // recompositions never rebuild the grid or rescan the task list per cell.
    val calendarDates = remember(start, end) { datesBetween(start, end) }
    val intensityByDate = remember(calendarDates, tasks) { summarizeByDate(calendarDates, tasks) }
    val selectedTasks = remember(selectedDate, tasks) { tasks.filter { it.date == selectedDate.toString() } }

    // Keep the applied lock-screen wallpaper in sync after any goal/task change.
    // Debounced so bursts of edits coalesce instead of rendering a full-size
    // wallpaper bitmap on every single task toggle.
    LaunchedEffect(goal, tasks) {
        delay(500)
        viewModel.refreshWallpaperIfApplied(context)
    }

    Scaffold(
        containerColor = Color(0xFFF7F4EE),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, contentDescription = "Open menu") }
                },
                title = {
                    Column {
                        Text("FOCUS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                        Text(goal.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                actions = {
                    IconButton(onClick = { showEditGoal = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit goal") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F4EE),
                    scrolledContainerColor = Color(0xFFF7F4EE),
                ),
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddTask = true }, containerColor = Color(0xFF2D6A4F), contentColor = Color.White) { Icon(Icons.Default.Add, "Add task") } },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("${daysRemaining(today, end)} days left", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(22.dp))
            Text("Your year, one day at a time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gridSpec = calculateGridSpec(calendarDates.size, maxWidth.value)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CalendarGrid(
                        calendarDates = calendarDates,
                        cellsPerRow = gridSpec.columns,
                        cellSize = gridSpec.cellSizeDp.dp,
                        cellGap = gridSpec.gapDp.dp,
                        intensityByDate = intensityByDate,
                        onDateClick = { selectedDate = it },
                    )
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
            selectedTasks.forEachIndexed { index, task ->
                val isExpanded = expandedTaskId == task.id
                TaskRow(
                    task = task,
                    viewModel = viewModel,
                    expanded = isExpanded,
                    onExpandToggle = { newExpanded ->
                        expandedTaskId = if (newExpanded) task.id else null
                    },
                    onEdit = { editingTask = it },
                )
                if (isExpanded && index < selectedTasks.lastIndex) {
                    HorizontalDivider(
                        color = Color(0xFFD0CCC5),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 52.dp),
                    )
                }
            }
        }
    }
    val dialogTask = editingTask
    if (showAddTask || dialogTask != null) TaskDialog(
        date = dialogTask?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() } ?: selectedDate,
        goalEnd = end,
        editing = dialogTask,
        onConfirm = { draft ->
            val target = dialogTask
            when {
                target == null -> viewModel.addTaskFromDraft(selectedDate, draft)
                target.recurrence == Recurrence.NONE -> viewModel.updateTaskFromDraft(target, draft)
                else -> pendingRecurringEdit = target to draft
            }
            showAddTask = false
            editingTask = null
        },
        onDismiss = {
            showAddTask = false
            editingTask = null
        },
    )
    val recurringEdit = pendingRecurringEdit
    if (recurringEdit != null) RecurringChangeDialog(
        taskTitle = recurringEdit.first.title,
        action = "Save",
        onConfirm = { scope ->
            viewModel.updateTaskFromDraft(recurringEdit.first, recurringEdit.second, scope)
            pendingRecurringEdit = null
        },
        onDismiss = { pendingRecurringEdit = null },
    )
    if (showEditGoal) EditGoalDialog(
        goal,
        viewModel,
        onDismiss = { showEditGoal = false },
        onGoalSaved = {
            showEditGoal = false
            showWallpaperPrompt = true
        },
    )
    if (showWallpaperPrompt) WallpaperPromptDialog(
        onConfirm = {
            showWallpaperPrompt = false
            onOpenLockScreen()
        },
        onDismiss = { showWallpaperPrompt = false },
    )
}

/**
 * The contribution grid, isolated in its own composable so it only recomposes
 * when its inputs actually change. Cell values come from the precomputed
 * [intensityByDate] map instead of re-scanning tasks for every cell.
 */
@Composable
private fun CalendarGrid(
    calendarDates: List<LocalDate>,
    cellsPerRow: Int,
    cellSize: Dp,
    cellGap: Dp,
    intensityByDate: Map<LocalDate, Intensity>,
    onDateClick: (LocalDate) -> Unit,
) {
    Column(
        Modifier.width(cellSize * cellsPerRow + cellGap * (cellsPerRow - 1)),
        verticalArrangement = Arrangement.spacedBy(cellGap),
    ) {
        calendarDates.chunked(cellsPerRow).forEach { rowDates ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(cellGap, Alignment.Start)) {
                rowDates.forEach { date ->
                    DayCell(intensityByDate[date] ?: Intensity.NONE, cellSize) { onDateClick(date) }
                }
            }
        }
    }
}

@Composable
fun DayCell(intensity: Intensity, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .background(intensity.color(), RoundedCornerShape(4.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}