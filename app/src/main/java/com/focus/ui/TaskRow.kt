package com.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.focus.data.Recurrence
import com.focus.data.Task

private val AppGreen = Color(0xFF2D6A4F)
private val AppInk = Color(0xFF173B2D)
private val AppDelete = Color(0xFF8B5E5E)

@Composable
fun TaskRow(task: Task, viewModel: CalendarViewModel, onEdit: (Task) -> Unit = {}) {
    var expanded by remember(task.id) { mutableStateOf(false) }
    var showRecurringDelete by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.toggleTask(task.id) }) {
                Icon(
                    if (task.completed) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    "Toggle ${task.title}",
                    tint = if (task.completed) AppGreen else Color.LightGray,
                )
            }
            Text(
                task.title,
                Modifier.weight(1f),
                color = if (task.completed) Color.Gray else AppInk,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (task.subtasks.isNotEmpty()) {
                Text(
                    "${task.subtasks.count { it.completed }}/${task.subtasks.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF5B665F),
                )
                Spacer(Modifier.width(6.dp))
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    "Details for ${task.title}",
                    tint = Color(0xFF5B665F),
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 52.dp, bottom = 8.dp)) {
                task.subtasks.forEach { subtask ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(30.dp).clickable { viewModel.toggleSubtask(task.id, subtask.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (subtask.completed) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                "Toggle ${subtask.title}",
                                tint = if (subtask.completed) AppGreen else Color.LightGray,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            subtask.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (subtask.completed) Color.Gray else AppInk,
                        )
                    }
                }
                Row {
                    TextButton(
                        onClick = { expanded = false; onEdit(task) },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            if (task.recurrence == Recurrence.NONE) viewModel.deleteTask(task) else showRecurringDelete = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppDelete),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
    if (showRecurringDelete) {
        RecurringChangeDialog(
            taskTitle = task.title,
            action = "Delete",
            onConfirm = { scope ->
                viewModel.deleteTask(task, scope)
                showRecurringDelete = false
            },
            onDismiss = { showRecurringDelete = false },
        )
    }
}
