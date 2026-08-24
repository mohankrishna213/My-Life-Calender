package com.mylifecalendar

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.mylifecalendar.data.Goal
import java.time.LocalDate

@Composable
fun AddTaskDialog(date: LocalDate, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add to ${date.format(dateFormatter)}") }, text = { OutlinedTextField(title, { title = it }, label = { Text("Task") }, singleLine = true) }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onAdd(title) }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalDialog(goal: Goal, viewModel: CalendarViewModel, onDismiss: () -> Unit) {
    var title by remember(goal) { mutableStateOf(goal.title) }
    var start by remember(goal) { mutableStateOf(LocalDate.parse(goal.startDate)) }
    var end by remember(goal) { mutableStateOf(LocalDate.parse(goal.endDate)) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit goal") },
        text = {
            Column {
                OutlinedTextField(title, { title = it }, label = { Text("Goal") }, singleLine = true)
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
                    onDismiss()
                }.onFailure { error = it.message ?: "Enter valid dates." }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
