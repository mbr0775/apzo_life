package com.example.apzolife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoSuccess

fun statusColor(status: String) = when (status) {
    TaskStatus.DONE.name     -> ApzoSuccess
    TaskStatus.NOT_DONE.name -> ApzoError
    else                     -> Color(0xFF8DCDE5)
}

fun statusLabel(status: String) = when (status) {
    TaskStatus.DONE.name     -> "Done"
    TaskStatus.NOT_DONE.name -> "Not Done"
    else                     -> "Pending"
}

@Composable
fun TaskCard(
    task: MainTask,
    onDone: () -> Unit,
    onNotDone: () -> Unit,
    onDelete: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDelete: Boolean = true
) {
    val sc = statusColor(task.status)

    Card(
        modifier = modifier.fillMaxWidth().clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(listOf(sc, sc.copy(alpha = 0.3f))),
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )
            Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sc.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusLabel(task.status),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = sc
                        )
                    }
                    if (showDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.DeleteOutline, null,
                                tint = ApzoError.copy(alpha = 0.7f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )

                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.DateRange, null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = buildString {
                            append(task.startDate.ifBlank { "—" })
                            if (task.startTime.isNotBlank()) append("  ${task.startTime}")
                            append("  →  ")
                            append(task.endDate.ifBlank { "—" })
                            if (task.endTime.isNotBlank()) append("  ${task.endTime}")
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (task.status == TaskStatus.PENDING.name) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onNotDone,
                            modifier = Modifier.weight(1f).height(38.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ApzoError.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ApzoError),
                            contentPadding = PaddingValues(vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Not Done", style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = onDone,
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApzoSuccess,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 0.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Done", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(sc.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (task.status == TaskStatus.DONE.name) Icons.Rounded.CheckCircle
                                else Icons.Rounded.Cancel,
                                null, tint = sc, modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                statusLabel(task.status),
                                style = MaterialTheme.typography.labelMedium,
                                color = sc, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern flat subtask row — thin hairline border instead of a heavy tinted
 * fill + drop shadow, smaller checkbox and icon buttons, and tighter,
 * lighter-weight status chips. Designed to sit comfortably nested inside
 * subtask trees without feeling boxed-in.
 */
@Composable
fun SubtaskItem(
    subtask: SubTask,
    onDone: () -> Unit,
    onNotDone: () -> Unit,
    onPending: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (newTitle: String, newDescription: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = subtask.status == TaskStatus.DONE.name
    val sc = statusColor(subtask.status)
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(subtask.title) }
    var editDesc  by remember { mutableStateOf(subtask.description) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Subtask", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle, onValueChange = { editTitle = it },
                        label = { Text("Title") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    OutlinedTextField(
                        value = editDesc, onValueChange = { editDesc = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2, maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editTitle.isNotBlank()) onEdit(editTitle.trim(), editDesc.trim())
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (isDone) ApzoSuccess.copy(alpha = 0.28f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape)
                    .border(1.6.dp, sc, CircleShape)
                    .background(if (isDone) ApzoSuccess else Color.Transparent)
                    .clickable { if (!isDone) onDone() else onPending() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) Icon(
                    Icons.Rounded.Check, null,
                    tint = Color.White, modifier = Modifier.size(12.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = subtask.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(
                            onClick = {
                                editTitle = subtask.title
                                editDesc = subtask.description
                                showEditDialog = true
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Edit, null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(Modifier.width(2.dp))
                        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                            Icon(
                                Icons.Rounded.DeleteOutline, null,
                                tint = ApzoError.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (subtask.description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtask.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (subtask.status != TaskStatus.DONE.name) {
                        StatusPill(
                            label = "Done",
                            color = ApzoSuccess,
                            onClick = onDone
                        )
                    }
                    if (subtask.status != TaskStatus.NOT_DONE.name) {
                        StatusPill(
                            label = "Not Done",
                            color = ApzoError,
                            onClick = onNotDone
                        )
                    }
                }
            }
        }
    }
}

/** Small, light-weight outlined pill used for subtask quick actions. */
@Composable
private fun StatusPill(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            title, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
        )
        subtitle?.let {
            Text(
                it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline, fontSize = 12.sp
            )
        }
    }
}