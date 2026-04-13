package com.example.apzolife.ui.screens.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.TaskCategory
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.components.CircularProgressIndicatorCustom
import com.example.apzolife.ui.components.PercentageScrollSelector
import com.example.apzolife.ui.components.SectionHeader
import com.example.apzolife.ui.components.SubtaskItem
import com.example.apzolife.ui.components.getCategoryColor
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.viewmodel.ApzoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: ApzoViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit      // ← navigates to EditTaskScreen
) {
    val state by viewModel.taskDetailState.collectAsState()

    // Add-subtask sheet
    var showAddSubtask by remember { mutableStateOf(false) }
    var subtaskTitle   by remember { mutableStateOf("") }
    var subtaskDesc    by remember { mutableStateOf("") }
    var subtaskTime    by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    // Delete confirmation
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        viewModel.loadTaskDetail(taskId)
    }

    // ── Delete confirm ─────────────────────────────────────────────────────
    if (showDeleteDialog && state.task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task") },
            text  = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(state.task!!.id)
                    showDeleteDialog = false
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Subtask time picker ────────────────────────────────────────────────
    if (showTimePicker) {
        val tpState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text  = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    subtaskTime    = String.format("%02d:%02d", tpState.hour, tpState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    // ── Main scaffold ──────────────────────────────────────────────────────
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    state.task?.let { task ->
                        val cat = TaskCategory.fromString(task.category)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.emoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(task.title, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    } ?: Text("Task Detail", fontWeight = FontWeight.Bold)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { showAddSubtask = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = Color.White
            ) {
                Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Add Subtask")
            }
        }
    ) { padding ->

        if (state.isLoading || state.task == null) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val task     = state.task!!
        val category = TaskCategory.fromString(task.category)
        val catColor = getCategoryColor(category)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Task summary card ─────────────────────────────────────────
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = catColor.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.SpaceBetween,
                        modifier               = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = catColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text     = "${category.emoji} ${category.displayName}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = catColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text       = task.title,
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )

                            if (task.timeScheduled.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier          = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector        = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint               = MaterialTheme.colorScheme.outline,
                                        modifier           = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text  = "${task.dateScheduled}  •  ${task.timeScheduled}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        CircularProgressIndicatorCustom(
                            progress    = task.progressPercentage / 100f,
                            size        = 72.dp,
                            strokeWidth = 7.dp,
                            color       = catColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Edit & Delete buttons ─────────────────────────────
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Edit → full EditTaskScreen (like AddTaskScreen)
                        OutlinedButton(onClick = onEdit) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }

                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors  = ButtonDefaults.outlinedButtonColors(contentColor = ApzoError),
                            border  = BorderStroke(1.dp, ApzoError.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress   = { task.progressPercentage / 100f },
                        modifier   = Modifier.fillMaxWidth().height(8.dp),
                        color      = catColor,
                        trackColor = catColor.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text       = "Task Progress: ${task.progressPercentage}%",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = catColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PercentageScrollSelector(
                        selectedPercentage   = task.progressPercentage,
                        onPercentageSelected = { progress ->
                            viewModel.updateTaskProgressManual(task.id, progress)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (task.status == TaskStatus.PENDING.name || task.status == TaskStatus.IN_PROGRESS.name) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick  = { viewModel.markTaskNotDone(task.id) },
                                modifier = Modifier.weight(1f),
                                border   = BorderStroke(1.dp, ApzoError.copy(alpha = 0.6f)),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = ApzoError)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Not Done")
                            }

                            Button(
                                onClick  = { viewModel.markTaskDone(task.id) },
                                modifier = Modifier.weight(1f),
                                colors   = ButtonDefaults.buttonColors(
                                    containerColor = ApzoSuccess,
                                    contentColor   = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mark Done")
                            }
                        }
                    } else {
                        val isDone = task.status == TaskStatus.DONE.name
                        Surface(
                            color = (if (isDone) ApzoSuccess else ApzoError).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector        = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                    contentDescription = null,
                                    tint               = if (isDone) ApzoSuccess else ApzoError
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text       = if (isDone) "Task Completed!" else "Marked as Not Done",
                                    color      = if (isDone) ApzoSuccess else ApzoError,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (task.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = catColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector        = Icons.Rounded.Notes,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.outline,
                                modifier           = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text  = task.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Subtasks ──────────────────────────────────────────────────
            SectionHeader(
                title    = "Subtasks",
                subtitle = "${state.subtasks.count { it.status == TaskStatus.DONE.name }}/${state.subtasks.size} completed"
            )

            if (state.subtasks.isEmpty()) {
                Card(
                    shape  = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📋", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text  = "No subtasks yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text  = "Tap + to break this task into steps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                state.subtasks.forEach { subtask ->
                    SubtaskItem(
                        subtask          = subtask,
                        onDone           = { viewModel.markSubtaskDone(subtask.id, taskId) },
                        onNotDone        = { viewModel.markSubtaskNotDone(subtask.id, taskId) },
                        onDelete         = { viewModel.deleteSubtask(subtask.id, taskId) },
                        onProgressChange = { progress ->
                            viewModel.updateSubtaskProgress(subtask.id, taskId, progress)
                        },
                        onEdit = { newTitle, newDesc, newTime ->
                            viewModel.updateSubtask(
                                subtaskId      = subtask.id,
                                taskId         = taskId,
                                newTitle       = newTitle,
                                newDescription = newDesc,
                                newTime        = newTime
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // ── Add-subtask bottom sheet ───────────────────────────────────────
        if (showAddSubtask) {
            ModalBottomSheet(onDismissRequest = { showAddSubtask = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(20.dp)
                        .padding(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text       = "Add Subtask",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value         = subtaskTitle,
                        onValueChange = { subtaskTitle = it },
                        label         = { Text("Subtask Title *") },
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value         = subtaskDesc,
                        onValueChange = { subtaskDesc = it },
                        label         = { Text("Description (optional)") },
                        modifier      = Modifier.fillMaxWidth().height(80.dp),
                        maxLines      = 3,
                        shape         = RoundedCornerShape(12.dp)
                    )

                    OutlinedButton(
                        onClick  = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier           = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (subtaskTime.isBlank()) "Set Time (optional)" else subtaskTime)
                    }

                    Button(
                        onClick = {
                            if (subtaskTitle.isNotBlank()) {
                                viewModel.addSubtask(taskId, subtaskTitle, subtaskDesc, subtaskTime)
                                subtaskTitle   = ""
                                subtaskDesc    = ""
                                subtaskTime    = ""
                                showAddSubtask = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Subtask")
                    }
                }
            }
        }
    }
}