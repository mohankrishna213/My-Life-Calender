package com.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.data.Goal
import com.focus.data.Recurrence
import com.focus.data.Task
import com.focus.domain.localDateToPickerMillis
import com.focus.domain.pickerMillisToLocalDate
import java.time.LocalDate

private val AppGreen = Color(0xFF275A45)
private val AppGreenDisabled = Color(0xFF8DB5A1)
private val AppBackground = Color(0xFFF6F5EF)
private val AppInk = Color(0xFF1F3B2D)
private val AppMuted = Color(0xFF5A6660)
private val AppBorder = Color(0xFFCDD3CE)
private val AppChipBg = Color(0xFFE6E9E4)
private val AppDanger = Color(0xFFB94A48)

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppGreen,
    unfocusedBorderColor = AppBorder,
    cursorColor = AppGreen,
    focusedLabelColor = AppGreen,
)

data class TaskDraft(
    val title: String,
    val recurrence: Recurrence,
    val until: LocalDate?,
)

private data class RoutineChip(val label: String, val recurrence: Recurrence)

private val routineChips = listOf(
    RoutineChip("💧 2L Water", Recurrence.DAILY),
    RoutineChip("💻 LeetCode", Recurrence.DAILY),
    RoutineChip("🏃‍♂️ Run", Recurrence.WEEKLY),
)

private data class RoutineCategory(val title: String, val items: List<RoutineChip>)

private val moreRoutineCategories = listOf(
    RoutineCategory(
        "Health & Fitness",
        listOf(
            RoutineChip("🏋️ Gym", Recurrence.WEEKLY),
            RoutineChip("🧘‍♂️ Meditation", Recurrence.DAILY),
            RoutineChip("🥗 Healthy Meal", Recurrence.DAILY),
        ),
    ),
    RoutineCategory(
        "Study & Work",
        listOf(
            RoutineChip("📖 Read", Recurrence.DAILY),
            RoutineChip("📝 Revision", Recurrence.DAILY),
            RoutineChip("🧩 Aptitude", Recurrence.WEEKLY),
        ),
    ),
)

enum class RecurringChangeScope { THIS_TASK, THIS_AND_FUTURE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    date: LocalDate,
    goalEnd: LocalDate,
    editing: Task?,
    onConfirm: (TaskDraft) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(editing) { mutableStateOf(editing?.title ?: "") }
    var recurrence by remember(editing) { mutableStateOf(editing?.recurrence ?: Recurrence.NONE) }
    var useCustomUntil by remember(editing) {
        mutableStateOf(editing?.recurrenceUntil?.let { it != goalEnd.toString() } ?: false)
    }
    var customUntil by remember(editing) {
        mutableStateOf(editing?.recurrenceUntil?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: date)
    }
    var recurrenceExpanded by remember { mutableStateOf(false) }
    var showMoreRoutines by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun applyRoutine(label: String, routineRecurrence: Recurrence) {
        title = label
        recurrence = routineRecurrence
        useCustomUntil = false
        error = null
    }

    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = AppInk,
        textContentColor = AppInk,
        title = {
            Text(
                if (editing == null) "Add to ${date.format(dateFormatter)}" else "Edit task",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            )
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(3f)) {
                        Text("Task", style = MaterialTheme.typography.labelSmall, color = AppMuted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        OutlinedTextField(
                            title,
                            { title = it },
                            placeholder = { Text("What to do?") },
                            singleLine = true,
                            keyboardOptions = noSuggestionsKeyboardOptions,
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                        )
                    }
                    Column(Modifier.weight(1.2f)) {
                        Text("Repeat", style = MaterialTheme.typography.labelSmall, color = AppMuted, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, AppBorder, RoundedCornerShape(8.dp))
                                    .clickable { recurrenceExpanded = true },
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Text(recurrence.icon, fontSize = 18.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AppMuted, modifier = Modifier.size(12.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = recurrenceExpanded,
                                onDismissRequest = { recurrenceExpanded = false },
                                containerColor = AppBackground,
                            ) {
                                Recurrence.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(option.icon, fontSize = 16.sp)
                                                Spacer(Modifier.width(10.dp))
                                                Text(option.label, color = AppInk)
                                            }
                                        },
                                        onClick = {
                                            recurrence = option
                                            recurrenceExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Suggestions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = AppMuted)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    routineChips.forEach { chip ->
                        SuggestionChip(
                            onClick = { applyRoutine(chip.label, chip.recurrence) },
                            label = { Text(chip.label) },
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = AppChipBg,
                                labelColor = AppInk,
                            ),
                        )
                    }
                    SuggestionChip(
                        onClick = { showMoreRoutines = true },
                        label = { Text("⋮ More") },
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color.Transparent,
                            labelColor = AppInk,
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = AppBorder),
                    )
                }
                AnimatedVisibility(visible = recurrence != Recurrence.NONE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Text("Until:", style = MaterialTheme.typography.labelMedium, color = AppMuted)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppChipBg)
                                .clickable { useCustomUntil = !useCustomUntil }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (useCustomUntil) "Custom Date..." else "Goal End Date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppInk,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AppMuted, modifier = Modifier.size(10.dp))
                            }
                        }
                        if (useCustomUntil) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppChipBg)
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(customUntil.format(dateFormatter), style = MaterialTheme.typography.labelSmall, color = AppInk)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                error?.let { Text(it, color = Color.Red) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = AppGreen, disabledContentColor = AppGreenDisabled),
                onClick = {
                    val until = when {
                        recurrence == Recurrence.NONE -> null
                        useCustomUntil -> customUntil
                        else -> goalEnd
                    }
                    if (until != null && until.isBefore(date)) {
                        error = "Until date must be on or after the task date."
                    } else {
                        onConfirm(TaskDraft(title.trim(), recurrence, until))
                    }
                },
            ) { Text(if (editing == null) "Add" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppMuted) }
        },
    )
    if (showMoreRoutines) {
        MoreRoutinesDialog(
            onSelect = {
                applyRoutine(it.label, it.recurrence)
                showMoreRoutines = false
            },
            onDismiss = { showMoreRoutines = false },
        )
    }
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = localDateToPickerMillis(customUntil))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { customUntil = pickerMillisToLocalDate(it) }
                    showDatePicker = false
                }) { Text("OK", color = AppGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = AppMuted) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreRoutinesDialog(onSelect: (RoutineChip) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = AppInk,
        textContentColor = AppInk,
        title = { Text("More Routines", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                moreRoutineCategories.forEachIndexed { index, category ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    Text(
                        category.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        category.items.forEach { item ->
                            SuggestionChip(
                                onClick = { onSelect(item) },
                                label = { Text(item.label) },
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = AppChipBg,
                                    labelColor = AppInk,
                                ),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = AppMuted) } },
    )
}

@Composable
fun RecurringChangeDialog(
    taskTitle: String,
    action: String,
    onConfirm: (RecurringChangeScope) -> Unit,
    onDismiss: () -> Unit,
) {
    var scope by remember { mutableStateOf(RecurringChangeScope.THIS_TASK) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = AppInk,
        textContentColor = AppInk,
        title = {
            Text(
                if (action == "Delete") "Delete Recurring Task" else "Apply this change to future dates?",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column {
                Text(
                    if (action == "Delete") "\"$taskTitle\" is a repeating task. How do you want to delete it?"
                    else "\"$taskTitle\" is a repeating task. How do you want to save changes?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMuted,
                )
                Spacer(Modifier.height(12.dp))
                listOf(
                    "Only this task" to RecurringChangeScope.THIS_TASK,
                    "This and all future tasks" to RecurringChangeScope.THIS_AND_FUTURE,
                ).forEach { (label, value) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { scope = value }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = scope == value, onClick = { scope = value })
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = AppInk)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { scope.let(onConfirm) }) {
                Text(
                    action,
                    color = if (action == "Delete") AppDanger else AppGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppMuted) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalDialog(goal: Goal, viewModel: CalendarViewModel, onDismiss: () -> Unit, onGoalSaved: () -> Unit) {
    var title by remember(goal) { mutableStateOf(goal.title) }
    var start by remember(goal) { mutableStateOf(LocalDate.parse(goal.startDate)) }
    var end by remember(goal) { mutableStateOf(LocalDate.parse(goal.endDate)) }
    var error by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = AppInk,
        textContentColor = AppInk,
        title = { Text("Edit goal") },
        text = {
            Column(Modifier.pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }) {
                OutlinedTextField(title, { title = it }, label = { Text("Goal") }, singleLine = true, keyboardOptions = noSuggestionsKeyboardOptions, colors = appTextFieldColors())
                DatePickerField("Start date", start) { start = it }
                DatePickerField("End date", end) { end = it }
                error?.let { Text(it, color = Color.Red) }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                runCatching {
                    require(!end.isBefore(start)) { "End date must be on or after start date." }
                    viewModel.saveGoal(title, start.toString(), end.toString())
                    onGoalSaved()
                }.onFailure { error = it.message ?: "Enter valid dates." }
            }) { Text("Save", color = AppGreen) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AppMuted) } },
    )
}

@Composable
fun WallpaperPromptDialog(
    title: String = "Goal updated",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = AppInk,
        textContentColor = AppMuted,
        title = { Text(title) },
        text = { Text("Your lock screen wallpaper updates to match your goal. Preview it now, or it will update automatically on its own.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Preview it", color = AppGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now", color = AppMuted) } },
    )
}
