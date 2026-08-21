package com.mylifecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mylifecalendar.data.CalendarRepository
import com.mylifecalendar.data.Goal
import com.mylifecalendar.data.Task
import com.mylifecalendar.domain.Intensity
import com.mylifecalendar.domain.daysRemaining
import com.mylifecalendar.domain.datesBetween
import com.mylifecalendar.domain.summarize
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<CalendarViewModel> { CalendarViewModel.factory(applicationContext) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalendarApp(viewModel) }
    }
}

class CalendarViewModel(private val repository: CalendarRepository) : ViewModel() {
    val goal = repository.goal
    val tasks = repository.tasks
    fun saveGoal(title: String, start: String, end: String) = repository.saveGoal(Goal(title.trim(), start, end))
    fun addTask(title: String, date: LocalDate) = repository.addTask(title.trim(), date.toString())
    fun toggleTask(id: Long) = repository.toggleTask(id)
    fun deleteTask(id: Long) = repository.deleteTask(id)
    companion object {
        fun factory(context: android.content.Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(CalendarRepository(context)) as T
        }
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun CalendarApp(viewModel: CalendarViewModel) {
    val goal by viewModel.goal.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    MaterialTheme { Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F4EE)) { if (goal == null) SetupScreen(viewModel) else Dashboard(goal!!, tasks, viewModel) } }
}

@Composable
private fun SetupScreen(viewModel: CalendarViewModel) {
    var title by remember { mutableStateOf("My next chapter") }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var end by remember { mutableStateOf(LocalDate.now().plusDays(365).toString()) }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("My Life Calendar", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
        Spacer(Modifier.height(12.dp))
        Text("Turn the days ahead into something you can see.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(title, { title = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(start, { start = it }, label = { Text("Start date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(end, { end = it }, label = { Text("End date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Button(onClick = { runCatching { if (!LocalDate.parse(end).isBefore(LocalDate.parse(start))) viewModel.saveGoal(title, start, end) } }, modifier = Modifier.fillMaxWidth()) { Text("Begin the count") }
    }
}

@Composable
private fun Dashboard(goal: Goal, tasks: List<Task>, viewModel: CalendarViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now().coerceIn(LocalDate.parse(goal.startDate), LocalDate.parse(goal.endDate))) }
    var showAddTask by remember { mutableStateOf(false) }
    val start = LocalDate.parse(goal.startDate)
    val end = LocalDate.parse(goal.endDate)
    val dates = datesBetween(start, end)
    val selectedTasks = tasks.filter { it.date == selectedDate.toString() }
    Scaffold(containerColor = Color(0xFFF7F4EE), floatingActionButton = { FloatingActionButton(onClick = { showAddTask = true }, containerColor = Color(0xFF2D6A4F), contentColor = Color.White) { Icon(Icons.Default.Add, "Add task") } }) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("MY LIFE CALENDAR", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
            Text(goal.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
            Text("${daysRemaining(LocalDate.now(), end)} days left", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(22.dp))
            Text("Your year, one day at a time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                dates.forEach { date ->
                    val summary = summarize(date, tasks)
                    DayCell(summary.intensity, date == selectedDate) { selectedDate = date }
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
}

@Composable
private fun DayCell(intensity: Intensity, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(30.dp).background(intensity.color(), RoundedCornerShape(5.dp)).clickable(onClick = onClick).then(if (selected) Modifier.padding(2.dp) else Modifier)) {
        if (selected) Box(Modifier.fillMaxSize().background(Color.Transparent, RoundedCornerShape(3.dp)))
    }
}

private fun Intensity.color() = when (this) {
    Intensity.NONE -> Color(0xFFE2E6E1)
    Intensity.LOW -> Color(0xFFB7D9C5)
    Intensity.MEDIUM -> Color(0xFF72B58A)
    Intensity.HIGH -> Color(0xFF3E8B61)
    Intensity.FULL -> Color(0xFF145A3A)
}

@Composable
private fun TaskRow(task: Task, viewModel: CalendarViewModel) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { viewModel.toggleTask(task.id) }) { Icon(if (task.completed) Icons.Default.Check else Icons.Default.Check, "Toggle ${task.title}", tint = if (task.completed) Color(0xFF2D6A4F) else Color.LightGray) }
        Text(task.title, Modifier.weight(1f), color = if (task.completed) Color.Gray else Color(0xFF173B2D))
        IconButton(onClick = { viewModel.deleteTask(task.id) }) { Icon(Icons.Default.Delete, "Delete ${task.title}", tint = Color(0xFF8B5E5E)) }
    }
}

@Composable
private fun AddTaskDialog(date: LocalDate, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add to ${date.format(dateFormatter)}") }, text = { OutlinedTextField(title, { title = it }, label = { Text("Task") }, singleLine = true) }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onAdd(title) }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
