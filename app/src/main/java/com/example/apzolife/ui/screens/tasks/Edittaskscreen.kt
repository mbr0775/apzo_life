package com.example.apzolife.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.TaskCategory
import com.example.apzolife.ui.components.CategoryChip
import com.example.apzolife.ui.components.getCategoryColor
import com.example.apzolife.viewmodel.ApzoViewModel
import java.time.Instant
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

    // Load task detail when screen opens
    LaunchedEffect(taskId) {
        viewModel.loadTaskDetail(taskId)
    }

    // Wait until task is loaded before showing form
    val task = state.task ?: run {
        // Show nothing while loading
        return
    }

    // ── Form state initialised from existing task ──────────────────────────
    var title    by remember { mutableStateOf(task.title) }
    var notes    by remember { mutableStateOf(task.notes) }
    var startDate by remember { mutableStateOf(task.dateScheduled) }
    var startTime by remember { mutableStateOf(task.timeScheduled) }
    var selectedCategory by remember {
        mutableStateOf(TaskCategory.fromString(task.category))
    }
    var progress by remember { mutableIntStateOf(task.progressPercentage) }
    var titleError by remember { mutableStateOf(false) }

    // Picker visibility
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }

    val catColor          = getCategoryColor(selectedCategory)
    val percentageOptions = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

    // ── Start Date Picker ──────────────────────────────────────────────────
    if (showStartDatePicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { ms ->
                        startDate = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ISO_DATE)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = dpState) }
    }

    // ── Start Time Picker ──────────────────────────────────────────────────
    if (showStartTimePicker) {
        val tpState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Time") },
            text  = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    startTime = String.format(
                        Locale.getDefault(), "%02d:%02d", tpState.hour, tpState.minute
                    )
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // ── Screen ─────────────────────────────────────────────────────────────
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Task",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── 1. Task Title ──────────────────────────────────────────
                SectionLabel(text = "Task Title")
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it; titleError = false },
                    label         = { Text("Title *") },
                    modifier      = Modifier.fillMaxWidth(),
                    isError       = titleError,
                    supportingText = { if (titleError) Text("Title is required") },
                    leadingIcon   = { Icon(Icons.Rounded.Title, contentDescription = null) },
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true
                )

                // ── 2. Details / Notes ─────────────────────────────────────
                SectionLabel(text = "Notes / Details")
                OutlinedTextField(
                    value         = notes,
                    onValueChange = { notes = it },
                    label         = { Text("Notes (optional)") },
                    placeholder   = { Text("Describe your task...") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    maxLines      = 4,
                    leadingIcon   = {
                        Icon(
                            Icons.Rounded.Notes,
                            contentDescription = null,
                            modifier = Modifier.padding(bottom = 60.dp)
                        )
                    },
                    shape         = RoundedCornerShape(12.dp)
                )

                // ── 3. Scheduled Date & Time ───────────────────────────────
                SectionLabel(text = "Scheduled Date & Time")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(startDate.ifBlank { "Pick Date" }, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick  = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(startTime.ifBlank { "Pick Time" }, maxLines = 1)
                    }
                }

                // ── 4. Category ────────────────────────────────────────────
                SectionLabel(text = "Category")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TaskCategory.entries) { category ->
                        CategoryChip(
                            category = category,
                            selected = selectedCategory == category,
                            onClick  = { selectedCategory = category }
                        )
                    }
                }

                // ── 5. Completion Percentage ───────────────────────────────
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = catColor.copy(alpha = 0.06f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = "Completion",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color  = catColor,
                                shape  = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text       = "$progress%",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                    modifier   = Modifier.padding(
                                        horizontal = 14.dp, vertical = 4.dp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            value         = progress.toFloat(),
                            onValueChange = { progress = it.toInt() },
                            valueRange    = 0f..100f,
                            steps         = 9,
                            modifier      = Modifier.fillMaxWidth(),
                            colors        = SliderDefaults.colors(
                                thumbColor        = catColor,
                                activeTrackColor  = catColor,
                                inactiveTrackColor = catColor.copy(alpha = 0.2f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier              = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            percentageOptions.forEach { value ->
                                val selected = progress == value
                                FilterChip(
                                    selected = selected,
                                    onClick  = { progress = value },
                                    label    = {
                                        Text(
                                            "$value%",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = catColor.copy(alpha = 0.18f),
                                        selectedLabelColor     = catColor
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled             = true,
                                        selected            = selected,
                                        selectedBorderColor = catColor
                                    )
                                )
                            }
                        }
                    }
                }

                // ── 6. Save Button ─────────────────────────────────────────
                Button(
                    onClick = {
                        if (title.isBlank()) { titleError = true; return@Button }
                        viewModel.updateTask(
                            taskId        = taskId,
                            title         = title.trim(),
                            dateScheduled = startDate,
                            timeScheduled = startTime,
                            notes         = notes.trim(),
                            category      = selectedCategory,
                            progress      = progress
                        )
                        onSaved()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape  = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = catColor)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurface
    )
}