package com.example.apzolife.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.viewmodel.ApzoViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    taskId: String,
    viewModel: ApzoViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.taskDetailState.collectAsState()
    LaunchedEffect(taskId) { viewModel.loadTaskDetail(taskId) }
    val task = state.task ?: return

    var title       by remember(task.id) { mutableStateOf(task.title) }
    var description by remember(task.id) { mutableStateOf(task.description) }
    var startDate   by remember(task.id) { mutableStateOf(task.startDate) }
    var startTime   by remember(task.id) { mutableStateOf(task.startTime) }
    var endDate     by remember(task.id) { mutableStateOf(task.endDate) }
    var endTime     by remember(task.id) { mutableStateOf(task.endTime) }
    var status      by remember(task.id) { mutableStateOf(task.status) }
    var titleError  by remember(task.id) { mutableStateOf(false) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    val statusOptions = listOf(TaskStatus.PENDING, TaskStatus.DONE, TaskStatus.NOT_DONE)

    // Pickers
    if (showStartDatePicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                            .toLocalDate().format(DateTimeFormatter.ISO_DATE)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
    if (showStartTimePicker) {
        val current = try { if (startTime.isBlank()) LocalTime.now() else LocalTime.parse(startTime) }
        catch (_: Exception) { LocalTime.now() }
        val tpState = rememberTimePickerState(current.hour, current.minute, true)
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Start Time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    startTime = String.format(Locale.getDefault(), "%02d:%02d", tpState.hour, tpState.minute)
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") } }
        )
    }
    if (showEndDatePicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        endDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                            .toLocalDate().format(DateTimeFormatter.ISO_DATE)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
    if (showEndTimePicker) {
        val current = try { if (endTime.isBlank()) LocalTime.now() else LocalTime.parse(endTime) }
        catch (_: Exception) { LocalTime.now() }
        val tpState = rememberTimePickerState(current.hour, current.minute, true)
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("End Time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    endTime = String.format(Locale.getDefault(), "%02d:%02d", tpState.hour, tpState.minute)
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Edit Task", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .imePadding().verticalScroll(rememberScrollState()).navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(
                        Color(0xFF0A1628), Color(0xFF0F2744),
                        Color(0xFF1D546C).copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.background
                    ))
                )
            ) {
                Box(modifier = Modifier.size(160.dp).offset((-30).dp, (-30).dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.05f)))
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text("Edit Task", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Update details below", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                FormCard {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; titleError = false },
                        label = { Text("Task Title *") },
                        isError = titleError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (titleError) Text("Title is required", color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                }

                // Description
                FormCard {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        maxLines = 4, shape = RoundedCornerShape(14.dp)
                    )
                }

                // Schedule
                FormCard {
                    Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Start", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PickerButton("Date", formatDate(startDate), Icons.Rounded.DateRange, Modifier.weight(1f)) { showStartDatePicker = true }
                        PickerButton("Time", startTime.ifBlank { "Pick time" }, Icons.Rounded.Schedule, Modifier.weight(1f)) { showStartTimePicker = true }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("End", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PickerButton("Date", formatDate(endDate), Icons.Rounded.DateRange, Modifier.weight(1f)) { showEndDatePicker = true }
                        PickerButton("Time", endTime.ifBlank { "Pick time" }, Icons.Rounded.Schedule, Modifier.weight(1f)) { showEndTimePicker = true }
                    }
                }

                // Status
                FormCard {
                    Text("Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        statusOptions.forEach { s ->
                            val selected = status == s.name
                            val color = when (s) {
                                TaskStatus.DONE     -> Color(0xFF00B894)
                                TaskStatus.NOT_DONE -> Color(0xFFE17055)
                                else                -> Color(0xFF1D546C)
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { status = s.name },
                                label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.15f),
                                    selectedLabelColor = color
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true, selected = selected, selectedBorderColor = color
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        titleError = title.isBlank()
                        if (!titleError) {
                            viewModel.updateTask(
                                taskId = taskId, title = title.trim(),
                                description = description.trim(),
                                startDate = startDate, startTime = startTime,
                                endDate = endDate, endTime = endTime,
                                status = status
                            )
                            onSaved()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun formatDate(value: String): String = try {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
} catch (_: Exception) { value }