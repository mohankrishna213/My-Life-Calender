package com.mylifecalendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mylifecalendar.data.Goal
import java.time.LocalDate

private val AppGreen = Color(0xFF2D6A4F)
private val AppBackground = Color(0xFFF7F4EE)
private val AppInk = Color(0xFF173B2D)
private val AppMuted = Color(0xFF5B665F)

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppGreen,
    unfocusedBorderColor = Color(0xFFCBD3CB),
    cursorColor = AppGreen,
    focusedLabelColor = AppGreen,
)

@Composable
fun AddTaskDialog(date: LocalDate, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = AppInk,
        textContentColor = AppInk,
        title = { Text("Add to ${date.format(dateFormatter)}") },
        text = {
            OutlinedTextField(
                title,
                { title = it },
                label = { Text("Task") },
                singleLine = true,
                colors = appTextFieldColors(),
            )
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onAdd(title) }) {
                Text("Add", color = AppGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppMuted) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalDialog(goal: Goal, viewModel: CalendarViewModel, onDismiss: () -> Unit, onGoalSaved: () -> Unit) {
    var title by remember(goal) { mutableStateOf(goal.title) }
    var start by remember(goal) { mutableStateOf(LocalDate.parse(goal.startDate)) }
    var end by remember(goal) { mutableStateOf(LocalDate.parse(goal.endDate)) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppBackground,
        titleContentColor = AppInk,
        textContentColor = AppInk,
        title = { Text("Edit goal") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Goal") }, singleLine = true, colors = appTextFieldColors())
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
        text = { Text("Your lock screen wallpaper will update automatically. Want to preview it first?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Open Lock Screen", color = AppGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later", color = AppMuted) } },
    )
}
