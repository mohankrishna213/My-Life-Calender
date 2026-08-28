package com.focus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.unit.dp
import com.focus.domain.localDateToPickerMillis
import com.focus.domain.pickerMillisToLocalDate
import java.time.LocalDate

/**
 * Suppresses the keyboard's suggestion strip ("bubble") that Gboard shows
 * below the focused input box. Unknown/other keyboards simply ignore it.
 */
internal val noSuggestionsKeyboardOptions = KeyboardOptions(
    platformImeOptions = PlatformImeOptions(
        privateImeOptions = "com.google.android.inputmethod.latin.noSuggestion",
    ),
)

/** Colors that make a read-only field look identical regardless of
 *  focus state (no "shadow") and show no cursor. */
@Composable
private fun readOnlyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFFCBD3CB),
    unfocusedBorderColor = Color(0xFFCBD3CB),
    cursorColor = Color.Transparent,
    focusedLabelColor = Color(0xFF5B665F),
    unfocusedLabelColor = Color(0xFF5B665F),
    focusedContainerColor = Color(0xFFF7F4EE),
    unfocusedContainerColor = Color(0xFFF7F4EE),
)

/** Goal-field colors: green cursor, no handle knob. */
@Composable
private fun goalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF2D6A4F),
    unfocusedBorderColor = Color(0xFFCBD3CB),
    cursorColor = Color(0xFF2D6A4F),
    focusedLabelColor = Color(0xFF2D6A4F),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(viewModel: CalendarViewModel, onGoalCreated: () -> Unit = {}) {
    var title by remember { mutableStateOf("My next chapter") }
    var start by remember { mutableStateOf(LocalDate.now()) }
    var end by remember { mutableStateOf(LocalDate.now().plusDays(365)) }

    // Don't auto-focus any field when the screen first appears.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focusManager.clearFocus() }

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("Focus", style = androidx.compose.material3.MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = Color(0xFF173B2D))
        Spacer(Modifier.height(12.dp))
        Text("Turn the days ahead into something you can see.", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            title,
            { title = it },
            label = { Text("Goal") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = noSuggestionsKeyboardOptions,
            colors = goalFieldColors(),
        )
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
    val focusManager = LocalFocusManager.current
    Box(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        OutlinedTextField(
            value = date.format(dateFormatter),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            colors = readOnlyFieldColors(),
        )
        // Overlay captures taps: clear any other field's focus, then open
        // the date picker. The tap never reaches the OutlinedTextField
        // underneath so it never gains focus itself.
        Box(
            Modifier
                .fillMaxSize()
                .clickable {
                    focusManager.clearFocus()
                    showPicker = true
                },
        )
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToPickerMillis(date),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDateSelected(pickerMillisToLocalDate(it))
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}
