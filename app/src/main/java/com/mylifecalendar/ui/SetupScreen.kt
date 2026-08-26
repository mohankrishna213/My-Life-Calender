package com.mylifecalendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: CalendarViewModel, onGoalCreated: () -> Unit = {}) {
    var title by remember { mutableStateOf("My next chapter") }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var end by remember { mutableStateOf(LocalDate.now().plusDays(365)) }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("My Life Calendar", style = androidx.compose.material3.MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
        Spacer(Modifier.height(12.dp))
        Text("Turn the days ahead into something you can see.", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(title, { title = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
        DatePickerField("Start date", start) { start = it }
        DatePickerField("End date", end) { end = it }
        Spacer(Modifier.height(20.dp))
        Button(onClick = { if (!end.isBefore(start)) { viewModel.saveGoal(title, start.toString(), end.toString()); onGoalCreated() } }, modifier = Modifier.fillMaxWidth()) { Text("Begin the count") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(label: String, date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        OutlinedTextField(
            value = date.format(dateFormatter),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2D6A4F),
                unfocusedBorderColor = Color(0xFFCBD3CB),
                cursorColor = Color(0xFF2D6A4F),
                focusedLabelColor = Color(0xFF2D6A4F),
            ),
        )
        Box(Modifier.fillMaxSize().clickable { showPicker = true })
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}
