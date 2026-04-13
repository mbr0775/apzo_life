package com.example.apzolife.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.AiInsight
import com.example.apzolife.data.model.InsightType
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskCategory
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoInfo
import com.example.apzolife.ui.theme.ApzoPrimary
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.ui.theme.ApzoTertiary
import com.example.apzolife.ui.theme.ApzoWarning
import com.example.apzolife.ui.theme.CategoryCustom
import com.example.apzolife.ui.theme.CategoryDiet
import com.example.apzolife.ui.theme.CategoryExercise
import com.example.apzolife.ui.theme.CategoryReading
import com.example.apzolife.ui.theme.CategorySoftware
import com.example.apzolife.ui.theme.CategoryStudy

@Composable
fun CircularProgressIndicatorCustom(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    label: String = "${(progress * 100).toInt()}%"
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = stroke
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PercentageScrollSelector(
    selectedPercentage: Int,
    onPercentageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { value ->
            FilterChip(
                selected = selectedPercentage == value,
                onClick = { onPercentageSelected(value) },
                label = { Text("$value%") }
            )
        }
    }
}

@Composable
fun CategoryChip(
    category: TaskCategory,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val bgColor = getCategoryColor(category)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "${category.emoji} ${category.displayName}",
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = bgColor.copy(alpha = 0.18f),
            selectedLabelColor = bgColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = bgColor
        )
    )
}

fun getCategoryColor(category: TaskCategory): Color = when (category) {
    TaskCategory.SOFTWARE_DEVELOPMENT -> CategorySoftware
    TaskCategory.STUDY -> CategoryStudy
    TaskCategory.READING -> CategoryReading
    TaskCategory.WORK -> ApzoPrimary
    TaskCategory.PERSONAL -> ApzoTertiary
    TaskCategory.DIET -> CategoryDiet
    TaskCategory.EXERCISE -> CategoryExercise
    TaskCategory.CUSTOM -> CategoryCustom
}

@Composable
fun TaskCard(
    task: MainTask,
    onDone: () -> Unit,
    onNotDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onProgressChange: (Int) -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = TaskCategory.fromString(task.category)
    val catColor = getCategoryColor(category)
    val statusColor = when (task.status) {
        TaskStatus.DONE.name -> ApzoSuccess
        TaskStatus.NOT_DONE.name -> ApzoError
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar with gradient
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(IntrinsicSize.Max)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(catColor, catColor.copy(alpha = 0.3f))
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Category badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(catColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${category.emoji} ${category.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = catColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete",
                                tint = ApzoError.copy(alpha = 0.8f),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        CircularProgressIndicatorCustom(
                            progress = task.progressPercentage / 100f,
                            size = 44.dp,
                            strokeWidth = 4.dp,
                            color = catColor,
                            trackColor = catColor.copy(alpha = 0.15f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buildString {
                            append(task.dateScheduled)
                            if (task.timeScheduled.isNotBlank()) {
                                append("  •  ")
                                append(task.timeScheduled)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }

                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress label and bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${task.progressPercentage}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = catColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { task.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = catColor,
                    trackColor = catColor.copy(alpha = 0.12f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                PercentageScrollSelector(
                    selectedPercentage = task.progressPercentage,
                    onPercentageSelected = onProgressChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (task.status == TaskStatus.PENDING.name || task.status == TaskStatus.IN_PROGRESS.name) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onNotDone,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            border = BorderStroke(1.dp, ApzoError.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ApzoError),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Not Done",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = onDone,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ApzoSuccess,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Done",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (task.status == TaskStatus.DONE.name) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.Cancel
                                },
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (task.status == TaskStatus.DONE.name) "Completed" else "Not Done",
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SubtaskItem
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SubtaskItem(
    subtask: SubTask,
    onDone: () -> Unit,
    onNotDone: () -> Unit,
    onDelete: () -> Unit,
    onProgressChange: (Int) -> Unit,
    onEdit: (newTitle: String, newDescription: String, newTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = subtask.status == TaskStatus.DONE.name
    val statusColor = when (subtask.status) {
        TaskStatus.DONE.name -> ApzoSuccess
        TaskStatus.NOT_DONE.name -> ApzoError
        else -> MaterialTheme.colorScheme.outline
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(subtask.title) }
    var editDescription by remember { mutableStateOf(subtask.description) }
    var editTime by remember { mutableStateOf(subtask.timeScheduled) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Subtask", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Description (optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("Time (HH:mm, optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        placeholder = { Text("e.g. 09:30") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            onEdit(editTitle.trim(), editDescription.trim(), editTime.trim())
                        }
                        showEditDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) {
                ApzoSuccess.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, statusColor, CircleShape)
                    .background(if (isDone) ApzoSuccess else Color.Transparent)
                    .clickable { if (!isDone) onDone() else onNotDone() },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subtask.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontSize = 14.sp
                )

                if (subtask.description.isNotBlank()) {
                    Text(
                        text = subtask.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = 12.sp
                    )
                }

                if (subtask.timeScheduled.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = subtask.timeScheduled,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${subtask.progressPercentage}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                PercentageScrollSelector(
                    selectedPercentage = subtask.progressPercentage,
                    onPercentageSelected = onProgressChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        editTitle = subtask.title
                        editDescription = subtask.description
                        editTime = subtask.timeScheduled
                        showEditDialog = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit subtask",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete subtask",
                        tint = ApzoError.copy(alpha = 0.65f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modern InsightCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun InsightCard(
    insight: AiInsight,
    modifier: Modifier = Modifier
) {
    val (bgColor, borderColor, iconGradient, icon) = when (insight.type) {
        InsightType.ACHIEVEMENT -> Quadruple(
            Color(0xFFEAFDF5),
            ApzoSuccess.copy(alpha = 0.25f),
            listOf(Color(0xFF00B894), Color(0xFF00C9A7)),
            Icons.Rounded.EmojiEvents
        )
        InsightType.MOTIVATION -> Quadruple(
            Color(0xFFE8F4FF),
            ApzoInfo.copy(alpha = 0.3f),
            listOf(Color(0xFF1D546C), Color(0xFF2E7A9A)),
            Icons.Rounded.Bolt
        )
        InsightType.WARNING -> Quadruple(
            Color(0xFFFFF9EC),
            ApzoWarning.copy(alpha = 0.35f),
            listOf(Color(0xFFE6A817), Color(0xFFFDCB6E)),
            Icons.Rounded.Warning
        )
        InsightType.SUGGESTION -> Quadruple(
            Color(0xFFF0F4FF),
            ApzoPrimary.copy(alpha = 0.2f),
            listOf(Color(0xFF1D546C), Color(0xFF4A8FA8)),
            Icons.Rounded.Lightbulb
        )
    }

    val darkBgColor = when (insight.type) {
        InsightType.ACHIEVEMENT -> Color(0xFF0D2A20)
        InsightType.MOTIVATION -> Color(0xFF0D1E2C)
        InsightType.WARNING -> Color(0xFF2A1F0D)
        InsightType.SUGGESTION -> Color(0xFF0D1A2A)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Gradient icon pill
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(iconGradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 3.dp),
                    lineHeight = 20.sp,
                    fontSize = 13.sp
                )
            }

            insight.actionQuestion?.let { question ->
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(borderColor)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = iconGradient.first(),
                        modifier = Modifier
                            .size(15.dp)
                            .padding(top = 1.dp)
                    )
                    Text(
                        text = question,
                        style = MaterialTheme.typography.bodySmall,
                        color = iconGradient.first(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

// Helper data class for destructuring 4 values
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 12.sp
                )
            }
        }
        action?.invoke()
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = color.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}