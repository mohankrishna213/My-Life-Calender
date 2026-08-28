package com.mylifecalendar.ui.intervention

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mylifecalendar.CalendarViewModel
import com.mylifecalendar.data.Task
import java.time.LocalDate
import kotlinx.coroutines.delay

private data class WatermarkWord(
    val text: String,
    val leftFraction: Float,
    val topFraction: Float,
    val rotationDegrees: Float,
    val fontSizeSp: Float,
    val alpha: Float,
)

private val WATERMARK_WORDS = listOf(
    WatermarkWord("read a page", 0.03f, 0.06f, -14f, 12f, 0.07f),
    WatermarkWord("move your body", 0.55f, 0.04f, 12f, 16f, 0.055f),
    WatermarkWord("journal", 0.12f, 0.16f, -6f, 10f, 0.09f),
    WatermarkWord("meditate", 0.52f, 0.13f, 22f, 13f, 0.065f),
    WatermarkWord("10,000 steps", 0.60f, 0.27f, -9f, 10f, 0.085f),
    WatermarkWord("drink water", 0.02f, 0.30f, 4f, 15f, 0.05f),
    WatermarkWord("call someone", 0.05f, 0.40f, -16f, 11f, 0.075f),
    WatermarkWord("stretch", 0.65f, 0.36f, 18f, 9f, 0.10f),
    WatermarkWord("deep work", 0.03f, 0.50f, -22f, 14f, 0.055f),
    WatermarkWord("go outside", 0.60f, 0.56f, 7f, 11f, 0.08f),
    WatermarkWord("plan tomorrow", 0.10f, 0.65f, -4f, 12f, 0.065f),
    WatermarkWord("gratitude", 0.55f, 0.72f, 13f, 15f, 0.055f),
    WatermarkWord("side project", 0.04f, 0.78f, -11f, 9f, 0.09f),
    WatermarkWord("learn something", 0.52f, 0.84f, 6f, 10f, 0.075f),
    WatermarkWord("no phone dinner", 0.15f, 0.91f, -8f, 13f, 0.055f),
)

@Composable
fun InterventionHostScreen(
    targetPackage: String,
    targetAppName: String,
    viewModel: CalendarViewModel,
    onStayFocused: () -> Unit,
    onOpenAnyway: () -> Unit,
    onAddTask: () -> Unit,
    onMoreTasks: () -> Unit,
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val todayIncomplete = remember(tasks) {
        tasks.filter { it.date == LocalDate.now().toString() && !it.completed }
    }

    var pendingSnapshot by remember { mutableStateOf<List<Task>>(emptyList()) }
    LaunchedEffect(todayIncomplete) {
        if (todayIncomplete.isNotEmpty()) pendingSnapshot = todayIncomplete
    }

    var celebrateGrace by remember { mutableStateOf(false) }
    LaunchedEffect(todayIncomplete.isEmpty(), celebrateGrace) {
        if (todayIncomplete.isEmpty() && celebrateGrace) {
            delay(800)
            celebrateGrace = false
        }
    }

    if (todayIncomplete.isNotEmpty() || celebrateGrace) {
        InterventionPendingScreen(
            tasks = pendingSnapshot,
            targetAppName = targetAppName,
            onComplete = viewModel::toggleTask,
            onCompletedLast = { celebrateGrace = true },
            onStayFocused = onStayFocused,
            onOpenAnyway = onOpenAnyway,
            onMoreTasks = onMoreTasks,
        )
    } else {
        InterventionAllDoneScreen(
            targetAppName = targetAppName,
            onStayFocused = onStayFocused,
            onContinueToApp = onOpenAnyway,
            onAddTask = onAddTask,
        )
    }
}

@Composable
private fun InterventionPendingScreen(
    tasks: List<Task>,
    targetAppName: String,
    onComplete: (Long) -> Unit,
    onCompletedLast: () -> Unit,
    onStayFocused: () -> Unit,
    onOpenAnyway: () -> Unit,
    onMoreTasks: () -> Unit,
) {
    val displayTasks = tasks.take(4)
    val hiddenCount = tasks.size - displayTasks.size
    val completedIds = remember(tasks) { mutableStateListOf<Long>() }
    val allVisibleDone = completedIds.size == displayTasks.size && displayTasks.isNotEmpty()

    LaunchedEffect(allVisibleDone) {
        if (allVisibleDone) onCompletedLast()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F4EE),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = "MY LIFE CALENDAR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
                color = Color(0xFF2D6A4F),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Before you scroll —\nfinish these first?",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF173B2D),
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "tap to mark done",
                fontSize = 12.sp,
                color = Color(0xFF5B665F),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(20.dp))

            displayTasks.forEach { task ->
                val isDone = task.id in completedIds
                TaskPill(
                    title = task.title,
                    isDone = isDone,
                    onTap = {
                        if (!isDone) {
                            completedIds.add(task.id)
                            onComplete(task.id)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            if (hiddenCount > 0 && !allVisibleDone) {
                Text(
                    text = "$hiddenCount more task${if (hiddenCount > 1) "s" else ""} →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2D6A4F),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onMoreTasks)
                        .padding(vertical = 4.dp),
                )
            }

            if (allVisibleDone) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EC)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = "All done — great work! ✓",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D6A4F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            InterventionFooter(
                primaryLabel = "Stay focused, skip $targetAppName",
                secondaryLabel = "Open $targetAppName anyway",
                onPrimary = onStayFocused,
                onSecondary = onOpenAnyway,
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TaskPill(title: String, isDone: Boolean, onTap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(0.5.dp, Color(0xFFE0DDD6)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isDone) Color(0xFF2D6A4F) else Color.Transparent)
                    .border(1.5.dp, if (isDone) Color(0xFF2D6A4F) else Color(0xFF9B9B8A), CircleShape),
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = if (isDone) Color(0xFF9B9B8A) else Color(0xFF173B2D),
                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
            )
        }
    }
}

@Composable
private fun InterventionAllDoneScreen(
    targetAppName: String,
    onStayFocused: () -> Unit,
    onContinueToApp: () -> Unit,
    onAddTask: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF7F4EE),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                val density = LocalDensity.current
                WATERMARK_WORDS.forEach { word ->
                    Text(
                        text = word.text,
                        fontSize = word.fontSizeSp.sp,
                        color = Color(0xFF2D6A4F).copy(alpha = word.alpha),
                        modifier = Modifier
                            .offset(
                                x = with(density) { (word.leftFraction * w).toDp() },
                                y = with(density) { (word.topFraction * h).toDp() },
                            )
                            .rotate(word.rotationDegrees),
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2D6A4F)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "MY LIFE CALENDAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF2D6A4F),
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "All tasks done —\nadd one more?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF173B2D),
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Small commitments compound.\nEven one more thing counts.",
                    fontSize = 14.sp,
                    color = Color(0xFF5B665F),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF2D6A4F))
                        .clickable(onClick = onAddTask)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = "Add a task", fontSize = 14.sp, color = Color.White)
                }

                Spacer(Modifier.weight(1f))

                InterventionFooter(
                    primaryLabel = "Stay focused, skip $targetAppName",
                    secondaryLabel = "Continue to $targetAppName",
                    onPrimary = onStayFocused,
                    onSecondary = onContinueToApp,
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InterventionFooter(
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Color(0xFFE0DDD6))
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onPrimary,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
        ) {
            Text(
                primaryLabel,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = secondaryLabel,
            fontSize = 13.sp,
            color = Color(0xFF5B665F),
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onSecondary)
                .padding(4.dp),
        )
    }
}
