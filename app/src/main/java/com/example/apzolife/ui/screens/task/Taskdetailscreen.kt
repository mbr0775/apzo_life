package com.example.apzolife.ui.screens.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.components.SectionHeader
import com.example.apzolife.ui.components.SubtaskItem
import com.example.apzolife.ui.components.statusColor
import com.example.apzolife.ui.components.statusLabel
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.viewmodel.ApzoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    viewModel: ApzoViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val state by viewModel.taskDetailState.collectAsState()

    var showAddSubtask by remember { mutableStateOf(false) }
    var parentSubtaskId by remember { mutableStateOf<String?>(null) }
    var parentSubtaskTitle by remember { mutableStateOf<String?>(null) }
    var subtaskTitle   by remember { mutableStateOf("") }
    var subtaskDesc    by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val expandedSubtaskIds = remember { mutableStateListOf<String>() }

    fun openAddSubtaskSheet(parentId: String?, parentTitle: String?) {
        parentSubtaskId = parentId
        parentSubtaskTitle = parentTitle
        subtaskTitle = ""
        subtaskDesc = ""
        showAddSubtask = true
    }

    LaunchedEffect(taskId) { viewModel.loadTaskDetail(taskId) }

    if (showDeleteDialog && state.task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Task?") },
            text = { Text("This will permanently delete the task and all its subtasks.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(state.task!!.id)
                    showDeleteDialog = false
                    onBack()
                }) { Text("Delete", color = ApzoError) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        state.task?.title ?: "Task Detail",
                        fontWeight = FontWeight.Bold, maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            // navigationBarsPadding() is required here because the Scaffold's
            // contentWindowInsets is zeroed out above (so screen content can draw
            // edge-to-edge). Without this, the FAB sits underneath the system
            // gesture/navigation bar on edge-to-edge devices and becomes
            // unreachable instead of just visually tight to the bottom.
            FloatingActionButton(
                onClick = { openAddSubtaskSheet(parentId = null, parentTitle = null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.navigationBarsPadding()
            ) { Icon(Icons.Rounded.Add, "Add Subtask") }
        }
    ) { padding ->

        if (state.isLoading || state.task == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val task = state.task!!
        val sc   = statusColor(task.status)

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(sc.copy(alpha = 0.07f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Status badge
                    Surface(color = sc.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                        Text(statusLabel(task.status),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = sc)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Title
                    Text(task.title, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)

                    // Description
                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(task.description, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = sc.copy(alpha = 0.2f))
                    Spacer(Modifier.height(14.dp))

                    // Dates
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DateTimeChip("Start", task.startDate, task.startTime)
                        DateTimeChip("End", task.endDate, task.endTime)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Edit / Delete
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onEdit) {
                            Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Edit")
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ApzoError),
                            border = BorderStroke(1.dp, ApzoError.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Delete")
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Action buttons
                    if (task.status == TaskStatus.PENDING.name) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.markTaskNotDone(task.id) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, ApzoError.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ApzoError)
                            ) {
                                Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp)); Text("Not Done")
                            }
                            Button(
                                onClick = { viewModel.markTaskDone(task.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(ApzoSuccess, Color.White)
                            ) {
                                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp)); Text("Done")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.markTaskPending(task.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp)); Text("Reset to Pending")
                        }
                    }
                }
            }

            // Subtasks
            SectionHeader(
                title = "Subtasks",
                subtitle = "${state.subtasks.count { it.status == TaskStatus.DONE.name }}/${state.subtasks.size} completed"
            )

            if (state.subtasks.isEmpty()) {
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📋", fontSize = 28.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("No subtasks yet", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline)
                        Text("Tap + to add sub tasks", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                val rootSubtasks = state.subtasks
                    .filter { it.parentSubtaskId == null }
                    .sortedBy { it.orderIndex }
                rootSubtasks.forEach { subtask ->
                    SubtaskTreeItem(
                        subtask = subtask,
                        allSubtasks = state.subtasks,
                        taskId = taskId,
                        level = 0,
                        expandedIds = expandedSubtaskIds,
                        viewModel = viewModel,
                        onOpenAddSubtask = { target -> openAddSubtaskSheet(target.id, target.title) }
                    )
                }
            }

            // Extra breathing room so the last subtask/"Add child" row is never
            // covered by the FAB or the system navigation bar.
            Spacer(Modifier.height(110.dp))
        }

        // Add subtask bottom sheet
        if (showAddSubtask) {
            ModalBottomSheet(onDismissRequest = { showAddSubtask = false }) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .navigationBarsPadding().padding(20.dp).padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (parentSubtaskId == null) "Add Subtask" else "Add Child Subtask",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    parentSubtaskTitle?.let {
                        Text(
                            "Under: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = subtaskTitle, onValueChange = { subtaskTitle = it },
                        label = { Text("Sub task heading *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true, shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = subtaskDesc, onValueChange = { subtaskDesc = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 6, shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            if (subtaskTitle.isNotBlank()) {
                                viewModel.addSubtask(
                                    taskId = taskId,
                                    parentSubtaskId = parentSubtaskId,
                                    title = subtaskTitle.trim(),
                                    description = subtaskDesc.trim()
                                )
                                parentSubtaskId?.let { parentId ->
                                    if (!expandedSubtaskIds.contains(parentId)) expandedSubtaskIds.add(parentId)
                                }
                                subtaskTitle = ""; subtaskDesc = ""
                                showAddSubtask = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp)); Text("Add Subtask")
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtaskTreeItem(
    subtask: SubTask,
    allSubtasks: List<SubTask>,
    taskId: String,
    level: Int,
    expandedIds: MutableList<String>,
    viewModel: ApzoViewModel,
    onOpenAddSubtask: (SubTask) -> Unit
) {
    val children = allSubtasks
        .filter { it.parentSubtaskId == subtask.id }
        .sortedBy { it.orderIndex }
    val isExpanded = expandedIds.contains(subtask.id)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 14).dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SubtaskItem(
            subtask = subtask,
            onDone = { viewModel.markSubtaskDone(subtask.id, taskId) },
            onNotDone = { viewModel.markSubtaskNotDone(subtask.id, taskId) },
            onPending = { viewModel.markSubtaskPending(subtask.id, taskId) },
            onDelete = { viewModel.deleteSubtask(subtask.id, taskId) },
            onEdit = { newTitle, newDesc ->
                viewModel.updateSubtask(subtask.id, taskId, newTitle, newDesc)
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onOpenAddSubtask(subtask) }) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add child", fontSize = 12.sp)
            }
            if (children.isNotEmpty()) {
                TextButton(onClick = {
                    if (isExpanded) expandedIds.remove(subtask.id) else expandedIds.add(subtask.id)
                }) {
                    Text(if (isExpanded) "Hide ${children.size}" else "Show ${children.size}", fontSize = 12.sp)
                    Icon(
                        if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isExpanded) {
            children.forEach { child ->
                SubtaskTreeItem(
                    subtask = child,
                    allSubtasks = allSubtasks,
                    taskId = taskId,
                    level = level + 1,
                    expandedIds = expandedIds,
                    viewModel = viewModel,
                    onOpenAddSubtask = onOpenAddSubtask
                )
            }
        }
    }
}

@Composable
private fun DateTimeChip(label: String, date: String, time: String) {
    Column {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(3.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(date.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (time.isNotBlank())
                    Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}