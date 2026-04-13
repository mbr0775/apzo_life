package com.example.apzolife.ui.screens.task

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DraftSubtask(val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: ApzoViewModel,
    onBack: () -> Unit,
    onTaskCreated: () -> Unit
) {
    // ── State ──────────────────────────────────────────────────────────────
    var title            by remember { mutableStateOf("") }
    var details          by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.SOFTWARE_DEVELOPMENT) }

    var startDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var startTime by remember { mutableStateOf("") }
    var endDate   by remember { mutableStateOf("") }
    var endTime   by remember { mutableStateOf("") }

    var progress by remember { mutableIntStateOf(0) }

    // Draft subtasks
    val draftSubtasks        = remember { mutableStateListOf<DraftSubtask>() }
    var newSubtaskTitle      by remember { mutableStateOf("") }
    var showSubtaskInput     by remember { mutableStateOf(false) }

    // Edit draft subtask
    var editingSubtaskIndex  by remember { mutableStateOf(-1) }
    var editSubtaskTitle     by remember { mutableStateOf("") }
    var showEditSubtaskDialog by remember { mutableStateOf(false) }

    var titleError by remember { mutableStateOf(false) }

    // Pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    val catColor          = getCategoryColor(selectedCategory)
    val percentageOptions = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

    LaunchedEffect(Unit) {
        viewModel.taskCreated.collect { onTaskCreated() }
    }

    // ── Edit draft-subtask dialog ──────────────────────────────────────────
    if (showEditSubtaskDialog) {
        AlertDialog(
            onDismissRequest = { showEditSubtaskDialog = false },
            title = { Text("Edit Subtask", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value         = editSubtaskTitle,
                    onValueChange = { editSubtaskTitle = it },
                    label         = { Text("Subtask title") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(10.dp),
                    singleLine    = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editSubtaskTitle.isNotBlank() && editingSubtaskIndex >= 0) {
                        draftSubtasks[editingSubtaskIndex] =
                            DraftSubtask(editSubtaskTitle.trim())
                    }
                    showEditSubtaskDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditSubtaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Start Date Picker ──────────────────────────────────────────────────
    if (showStartDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        startDate = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ISO_DATE)
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    // ── Start Time Picker ──────────────────────────────────────────────────
    if (showStartTimePicker) {
        val state = rememberTimePickerState(LocalTime.now().hour, LocalTime.now().minute)
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Start Time") },
            text  = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    startTime = String.format(Locale.getDefault(), "%02d:%02d", state.hour, state.minute)
                    showStartTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Cancel") } }
        )
    }

    // ── End Date Picker ────────────────────────────────────────────────────
    if (showEndDatePicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        endDate = Instant.ofEpochMilli(ms)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ISO_DATE)
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    // ── End Time Picker ────────────────────────────────────────────────────
    if (showEndTimePicker) {
        val state = rememberTimePickerState(LocalTime.now().hour, LocalTime.now().minute)
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("End Time") },
            text  = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    endTime = String.format(Locale.getDefault(), "%02d:%02d", state.hour, state.minute)
                    showEndTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("Cancel") } }
        )
    }

    // ── Screen ─────────────────────────────────────────────────────────────
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Add New Task", fontWeight = FontWeight.Bold) },
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
                SectionLabel2(text = "Task Title")
                OutlinedTextField(
                    value         = title,
                    onValueChange = { title = it; titleError = false },
                    label         = { Text("Title *") },
                    placeholder   = { Text("e.g., Finish the report") },
                    modifier      = Modifier.fillMaxWidth(),
                    isError       = titleError,
                    supportingText = { if (titleError) Text("Title is required") },
                    leadingIcon   = { Icon(Icons.Rounded.Title, contentDescription = null) },
                    shape         = RoundedCornerShape(12.dp),
                    singleLine    = true
                )

                // ── 2. Details ─────────────────────────────────────────────
                SectionLabel2(text = "Add Details")
                OutlinedTextField(
                    value         = details,
                    onValueChange = { details = it },
                    label         = { Text("Details (optional)") },
                    placeholder   = { Text("Describe your task...") },
                    modifier      = Modifier.fillMaxWidth().height(110.dp),
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

                // ── 3. Starting Date & Time ────────────────────────────────
                SectionLabel2(text = "Starting Date & Time")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(startDate.ifBlank { "Start Date" }, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick  = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(startTime.ifBlank { "Start Time" }, maxLines = 1)
                    }
                }

                // ── 4. End Date & Time ─────────────────────────────────────
                SectionLabel2(text = "End Date & Time")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick  = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(endDate.ifBlank { "End Date" }, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick  = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(endTime.ifBlank { "End Time" }, maxLines = 1)
                    }
                }

                // ── 5. Category ────────────────────────────────────────────
                SectionLabel2(text = "Category")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TaskCategory.entries) { category ->
                        CategoryChip(
                            category = category,
                            selected = selectedCategory == category,
                            onClick  = { selectedCategory = category }
                        )
                    }
                }

                // ── 6. Subtasks ────────────────────────────────────────────
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text       = "Subtasks",
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text  = "${draftSubtasks.size} added",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Surface(
                                color    = catColor,
                                shape    = CircleShape,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable { showSubtaskInput = !showSubtaskInput }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier         = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector        = if (showSubtaskInput) Icons.Rounded.Close else Icons.Rounded.Add,
                                        contentDescription = "Add subtask",
                                        tint               = Color.White,
                                        modifier           = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Input row
                        if (showSubtaskInput) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value         = newSubtaskTitle,
                                    onValueChange = { newSubtaskTitle = it },
                                    label         = { Text("Subtask title") },
                                    modifier      = Modifier.weight(1f),
                                    shape         = RoundedCornerShape(10.dp),
                                    singleLine    = true
                                )
                                Surface(
                                    color    = catColor,
                                    shape    = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (newSubtaskTitle.isNotBlank()) {
                                                draftSubtasks.add(DraftSubtask(newSubtaskTitle.trim()))
                                                newSubtaskTitle  = ""
                                                showSubtaskInput = false
                                            }
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier         = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Add", tint = Color.White)
                                    }
                                }
                            }
                        }

                        // Draft subtask list — each row has edit ✏ + delete ×
                        if (draftSubtasks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            draftSubtasks.forEachIndexed { index, subtask ->
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Bullet dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Title
                                    Text(
                                        text     = subtask.title,
                                        style    = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Edit pencil
                                    IconButton(
                                        onClick  = {
                                            editingSubtaskIndex  = index
                                            editSubtaskTitle     = subtask.title
                                            showEditSubtaskDialog = true
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Rounded.Edit,
                                            contentDescription = "Edit subtask",
                                            tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier           = Modifier.size(15.dp)
                                        )
                                    }

                                    // Delete ×
                                    IconButton(
                                        onClick  = { draftSubtasks.removeAt(index) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            tint               = MaterialTheme.colorScheme.outline,
                                            modifier           = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 7. Completion Percentage ───────────────────────────────
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.06f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
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
                            Surface(color = catColor, shape = RoundedCornerShape(20.dp)) {
                                Text(
                                    text       = "$progress%",
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
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
                                thumbColor         = catColor,
                                activeTrackColor   = catColor,
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
                                        Text("$value%", style = MaterialTheme.typography.labelMedium)
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

                // ── 8. Create Task ─────────────────────────────────────────
                Button(
                    onClick = {
                        if (title.isBlank()) { titleError = true; return@Button }

                        val extraData = Json.encodeToString(
                            mapOf(
                                "details"  to details,
                                "endDate"  to endDate,
                                "endTime"  to endTime,
                                "subtasks" to draftSubtasks.joinToString("|") { it.title }
                            )
                        )

                        viewModel.createTask(
                            title         = title,
                            category      = selectedCategory,
                            dateScheduled = startDate,
                            timeScheduled = startTime,
                            notes         = details,
                            categoryData  = extraData
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = catColor)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Task", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel2(text: String) {
    Text(
        text       = text,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurface
    )
}