package com.example.apzolife.ui.screens.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.ai.AiScheduleSuggestion
import com.example.apzolife.data.ai.AiSmartTaskSuggestionResult
import com.example.apzolife.data.ai.AiSmartTaskSuggestionService
import com.example.apzolife.viewmodel.ApzoViewModel
import com.example.apzolife.viewmodel.CreateSubtaskInput
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    viewModel: ApzoViewModel,
    onBack: () -> Unit,
    onTaskCreated: () -> Unit
) {
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate   by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var startTime   by remember { mutableStateOf("") }
    var endDate     by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_DATE)) }
    var endTime     by remember { mutableStateOf("") }
    var titleError  by remember { mutableStateOf(false) }
    val draftSubtasks = remember { mutableStateListOf<DraftSubtaskInput>() }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker   by remember { mutableStateOf(false) }
    var showEndTimePicker   by remember { mutableStateOf(false) }

    // AI Smart Task Creator ("✨ Suggest details") state
    var isSuggestLoading by remember { mutableStateOf(false) }
    var suggestError     by remember { mutableStateOf<String?>(null) }
    var suggestCooldownUntil by remember { mutableLongStateOf(0L) }
    var pendingSchedule  by remember { mutableStateOf<AiScheduleSuggestion?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var showResetDialog by remember { mutableStateOf(false) }

    val suggestButtonEnabled = !isSuggestLoading &&
            System.currentTimeMillis() >= suggestCooldownUntil

    LaunchedEffect(Unit) {
        viewModel.taskCreated.collect { onTaskCreated() }
    }

    // Date/Time pickers
    if (showStartDatePicker) {
        val dpState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let {
                        startDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                            .toLocalDate().format(DateTimeFormatter.ISO_DATE)
                        endDate = startDate
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }

    if (showStartTimePicker) {
        val now = LocalTime.now()
        val tpState = rememberTimePickerState(now.hour, now.minute, true)
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
        val now = LocalTime.now()
        val tpState = rememberTimePickerState(now.hour, now.minute, true)
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

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.RestartAlt, contentDescription = null) },
            title = { Text("Reset this task?") },
            text = {
                Text("This clears the title, description, schedule, and all generated sub tasks so you can start fresh. This does not affect any task you've already saved.")
            },
            confirmButton = {
                TextButton(onClick = {
                    title = ""
                    description = ""
                    startDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    startTime = ""
                    endDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    endTime = ""
                    titleError = false
                    suggestError = null
                    pendingSchedule = null
                    draftSubtasks.clear()
                    showResetDialog = false
                }) { Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Schedule suggestion confirm dialog — never silently overwrites dates/times.
    pendingSchedule?.let { schedule ->
        ScheduleSuggestionDialog(
            schedule = schedule,
            onApply = {
                applySchedule(schedule, startDate) { newStart, newEnd ->
                    startTime = newStart
                    endTime = newEnd
                }
                pendingSchedule = null
            },
            onDismiss = { pendingSchedule = null }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("New Task", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Back")
                    }
                },
                actions = {
                    val hasContent = title.isNotBlank() || description.isNotBlank() ||
                            draftSubtasks.isNotEmpty()
                    IconButton(
                        onClick = { showResetDialog = true },
                        enabled = hasContent
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "Reset task",
                            tint = if (hasContent) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .imePadding().verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Hero header
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
                Box(modifier = Modifier.size(100.dp).align(Alignment.TopEnd)
                    .clip(CircleShape).background(Color(0xFF00B894).copy(alpha = 0.1f)))
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text("Create a Task", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("Fill in the details below", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
            }

            // Form
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title + compact "✨ Suggest details" trigger
                FormCard {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; titleError = false },
                        label = { Text("Task Title *") },
                        placeholder = { Text("e.g. Prepare for DevOps interview") },
                        isError = titleError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    if (titleError) {
                        Text("Title is required", color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                    }

                    Spacer(Modifier.height(10.dp))

                    SuggestDetailsButton(
                        enabled = suggestButtonEnabled,
                        isLoading = isSuggestLoading,
                        onClick = {
                            val cleanTitle = title.trim()
                            when {
                                cleanTitle.isBlank() -> {
                                    titleError = true
                                    suggestError = "Enter a task title first"
                                    return@SuggestDetailsButton
                                }
                                cleanTitle.length < 2 -> {
                                    suggestError = "Title is too short for AI to work with"
                                    return@SuggestDetailsButton
                                }
                                cleanTitle.length > 120 -> {
                                    suggestError = "Title is too long. Try something under 120 characters."
                                    return@SuggestDetailsButton
                                }
                            }

                            suggestError = null
                            isSuggestLoading = true
                            coroutineScope.launch {
                                val result = AiSmartTaskSuggestionService.suggestDetails(
                                    cleanTitle,
                                    description.trim().take(500)
                                )
                                isSuggestLoading = false
                                // Cooldown regardless of outcome so rapid repeat taps
                                // can't hammer the free-tier quota.
                                suggestCooldownUntil = System.currentTimeMillis() + 2500

                                result
                                    .onSuccess { suggestion ->
                                        applySuggestion(
                                            suggestion = suggestion,
                                            description = description,
                                            onDescription = { description = it },
                                            draftSubtasks = draftSubtasks
                                        )
                                        if (suggestion.scheduleSuggestion.totalMinutes > 0) {
                                            pendingSchedule = suggestion.scheduleSuggestion
                                        }
                                    }
                                    .onFailure { e ->
                                        suggestError = AiSmartTaskSuggestionService.friendlyErrorMessage(e)
                                    }
                            }
                        }
                    )

                    suggestError?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }

                // Description — grows with content instead of clipping it.
                FormCard {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("Add details about this task...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 10,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Schedule
                FormCard {
                    Text("Schedule", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    Text("Start", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PickerButton(
                            label = "Date",
                            value = formatDate(startDate),
                            icon = Icons.Rounded.DateRange,
                            modifier = Modifier.weight(1f),
                            onClick = { showStartDatePicker = true }
                        )
                        PickerButton(
                            label = "Time",
                            value = startTime.ifBlank { "Pick time" },
                            icon = Icons.Rounded.Schedule,
                            modifier = Modifier.weight(1f),
                            onClick = { showStartTimePicker = true }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("End", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PickerButton(
                            label = "Date",
                            value = formatDate(endDate),
                            icon = Icons.Rounded.DateRange,
                            modifier = Modifier.weight(1f),
                            onClick = { showEndDatePicker = true }
                        )
                        PickerButton(
                            label = "Time",
                            value = endTime.ifBlank { "Pick time" },
                            icon = Icons.Rounded.Schedule,
                            modifier = Modifier.weight(1f),
                            onClick = { showEndTimePicker = true }
                        )
                    }
                }

                // Subtask tree
                FormCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Sub Tasks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Split the main task into smaller steps",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                draftSubtasks.add(DraftSubtaskInput().apply { isOpen = true })
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add sub task")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (draftSubtasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Tap + to add a sub task, or use ✨ Suggest details above.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            draftSubtasks.forEach { subtask ->
                                DraftSubtaskEditor(
                                    item = subtask,
                                    level = 0,
                                    onRemove = { draftSubtasks.remove(subtask) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = {
                        titleError = title.isBlank()
                        if (!titleError) {
                            viewModel.createTaskWithSubtasks(
                                title = title.trim(),
                                description = description.trim(),
                                startDate = startDate, startTime = startTime,
                                endDate = endDate, endTime = endTime,
                                subtasks = draftSubtasks.mapNotNull { it.toCreateSubtaskInputOrNull() }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create Task", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Small, opt-in "✨ Suggest details" trigger — never fires automatically. */
@Composable
private fun SuggestDetailsButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.10f else 0.05f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Thinking...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Rounded.AutoAwesome, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Suggest details",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ScheduleSuggestionDialog(
    schedule: AiScheduleSuggestion,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
        title = { Text("Suggested schedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val hours = schedule.totalMinutes / 60
                val mins = schedule.totalMinutes % 60
                val totalLabel = buildString {
                    if (hours > 0) append("${hours}h ")
                    if (mins > 0 || hours == 0) append("${mins}m")
                }
                Text(
                    "$totalLabel starting at ${schedule.startTime}, split into ${schedule.blocks.size} blocks:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    schedule.blocks.forEach { block ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(block.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${block.durationMinutes} min",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onApply) { Text("Apply to schedule", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

@Composable
private fun DraftSubtaskEditor(
    item: DraftSubtaskInput,
    level: Int,
    onRemove: () -> Unit
) {
    val childCount = item.children.size
    val cardAlpha = if (level == 0) 0.16f else 0.08f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (level == 0) 0.dp else 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = cardAlpha)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (level == 0) 12.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (level == 0) "Sub task" else "Child sub task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            item.children.add(DraftSubtaskInput().apply { isOpen = true })
                            item.isOpen = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add child sub task",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (childCount > 0) {
                        IconButton(
                            onClick = { item.isOpen = !item.isOpen },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (item.isOpen) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
                                contentDescription = "Show child subtasks",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Remove sub task",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            SoftTaskTextField(
                value = item.title,
                onValueChange = { item.title = it },
                label = "Sub task heading",
                placeholder = if (level == 0) "e.g. Git basics" else "e.g. Branching",
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            SoftTaskTextField(
                value = item.description,
                onValueChange = { item.description = it },
                label = "Description (optional)",
                placeholder = "Add details for this sub task...",
                singleLine = false,
                minLines = 2,
                maxLines = 6
            )

            if (item.isOpen && item.children.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.children.forEach { child ->
                        DraftSubtaskEditor(
                            item = child,
                            level = level + 1,
                            onRemove = { item.children.remove(child) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftTaskTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        },
        placeholder = { Text(placeholder, fontSize = 14.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,

            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f),

            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
            unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),

            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    )
}

@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
fun PickerButton(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
                 modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = Color(0xFF1D546C)
    Box(
        modifier = modifier.clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.07f))
            .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
                Text(label, fontSize = 11.sp, color = accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

private class DraftSubtaskInput {
    val id: String = UUID.randomUUID().toString()
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var isOpen by mutableStateOf(false)
    val children = mutableStateListOf<DraftSubtaskInput>()
}

private fun DraftSubtaskInput.toCreateSubtaskInputOrNull(): CreateSubtaskInput? {
    val cleanTitle = title.trim()
    val cleanChildren = children.mapNotNull { it.toCreateSubtaskInputOrNull() }
    if (cleanTitle.isBlank()) return null
    return CreateSubtaskInput(
        title = cleanTitle,
        description = description.trim(),
        children = cleanChildren
    )
}

/** Applies an AI Smart Task Creator result: fills description (if blank) and appends subtasks. */
private fun applySuggestion(
    suggestion: AiSmartTaskSuggestionResult,
    description: String,
    onDescription: (String) -> Unit,
    draftSubtasks: MutableList<DraftSubtaskInput>
) {
    if (description.isBlank() && suggestion.description.isNotBlank()) {
        onDescription(suggestion.description)
    }
    val generated = suggestion.subtasks
        .filter { it.title.isNotBlank() }
        .map { sub ->
            DraftSubtaskInput().apply {
                title = sub.title
                this.description = sub.description
            }
        }
    draftSubtasks.addAll(generated)
}

/** Computes start/end times from a schedule suggestion and hands them back via the callback. */
private fun applySchedule(
    schedule: AiScheduleSuggestion,
    startDate: String,
    onTimes: (start: String, end: String) -> Unit
) {
    val parsedStart = try {
        LocalTime.parse(schedule.startTime, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        LocalTime.now()
    }
    val parsedEnd = parsedStart.plusMinutes(schedule.totalMinutes.toLong())
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    onTimes(parsedStart.format(fmt), parsedEnd.format(fmt))
}

private fun formatDate(value: String): String = try {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
} catch (_: Exception) { value }