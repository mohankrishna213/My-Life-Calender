package com.mylifecalendar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mylifecalendar.data.Task

@Composable
fun TaskRow(task: Task, viewModel: CalendarViewModel) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { viewModel.toggleTask(task.id) }) { Icon(if (task.completed) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked, "Toggle ${task.title}", tint = if (task.completed) Color(0xFF2D6A4F) else Color.LightGray) }
        Text(task.title, Modifier.weight(1f), color = if (task.completed) Color.Gray else Color(0xFF173B2D))
        IconButton(onClick = { viewModel.deleteTask(task.id) }) { Icon(Icons.Default.Delete, "Delete ${task.title}", tint = Color(0xFF8B5E5E)) }
    }
}
