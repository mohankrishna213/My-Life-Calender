package com.mohanbuilds.focus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mohanbuilds.focus.CalendarViewModel
import com.mohanbuilds.focus.data.Recurrence
import com.mohanbuilds.focus.data.Task

private val AppGreen = Color(0xFF2D6A4F)
private val AppInk = Color(0xFF173B2D)
private val AppDelete = Color(0xFF8B5E5E)
private val AppBorder = Color(0xFFCDD3CE)
private val AppMuted = Color(0xFF5A6660)

@Composable
fun TaskRow(
    task: Task,
    viewModel: CalendarViewModel,
    expanded: Boolean,
    onExpandToggle: (Boolean) -> Unit,
    onEdit: (Task) -> Unit = {},
) {
    var showRecurringDelete by remember { mutableStateOf(false) }
    var showAddSubtask by remember { mutableStateOf(false) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    val subtaskFocusRequester = remember { FocusRequester() }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val focusManager = LocalFocusManager.current

    LaunchedEffect(showAddSubtask) {
        if (showAddSubtask) {
            subtaskFocusRequester.requestFocus()
        }
    }

    fun dismissSubtaskInput() {
        if (showAddSubtask) {
            showAddSubtask = false
            newSubtaskTitle = ""
            focusManager.clearFocus()
        }
    }

    BackHandler(enabled = showAddSubtask) {
        dismissSubtaskInput()
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { dismissSubtaskInput() },
    ) {
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
                Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { viewModel.toggleTask(task.id) },
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
            IconButton(onClick = {
                val newExpanded = !expanded
                onExpandToggle(newExpanded)
                if (newExpanded) dismissSubtaskInput()
            }) {
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
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { viewModel.toggleSubtask(task.id, subtask.id) },
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
                            Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { viewModel.toggleSubtask(task.id, subtask.id) },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (subtask.completed) Color.Gray else AppInk,
                        )
                        IconButton(
                            onClick = { viewModel.deleteSubtask(task.id, subtask.id) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete subtask",
                                tint = AppMuted,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                if (showAddSubtask) {
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Subtask", style = MaterialTheme.typography.bodySmall) },
                        textStyle = MaterialTheme.typography.bodySmall,
                        singleLine = true,
                        keyboardOptions = noSuggestionsKeyboardOptions.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newSubtaskTitle.isNotBlank()) {
                                viewModel.addSubtask(task.id, newSubtaskTitle)
                                newSubtaskTitle = ""
                                showAddSubtask = false
                                focusManager.clearFocus()
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppGreen,
                            unfocusedBorderColor = AppBorder,
                            cursorColor = AppGreen,
                            focusedLabelColor = AppGreen,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(subtaskFocusRequester),
                    )
                }
                Row {
                    TextButton(
                        onClick = {
                            if (showAddSubtask) {
                                dismissSubtaskInput()
                            } else {
                                showAddSubtask = true
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = AppGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Subtask", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = { onExpandToggle(false); onEdit(task) },
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
