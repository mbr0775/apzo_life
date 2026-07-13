package com.example.apzolife.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.ai.AiInsightsResult
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.components.statusColor
import com.example.apzolife.ui.components.statusLabel
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.viewmodel.AiInsightsUiState
import com.example.apzolife.viewmodel.ApzoViewModel

enum class AnalyticsFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    DONE("Done"),
    PENDING("Pending"),
    MISSED("Missed")
}

@Composable
fun AnalyticsScreen(
    viewModel: ApzoViewModel,
    onTaskClick: (String) -> Unit
) {
    val state by viewModel.homeState.collectAsState()
    val insightsState by viewModel.insightsState.collectAsState()
    var selectedFilter by remember { mutableStateOf(AnalyticsFilter.ALL) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = ApzoError) },
            title = { Text("Reset all data?") },
            text = { Text("This will permanently delete all main tasks and subtasks from Apzo Life. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        selectedFilter = AnalyticsFilter.ALL
                        viewModel.resetAllData()
                    }
                ) { Text("Yes, delete all", color = ApzoError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    val allTasks = state.allTasks
    val todayTasks = state.todayTasks
    val completedTasks = allTasks.filter { it.status == TaskStatus.DONE.name }
    val pendingTasks = allTasks.filter { it.status == TaskStatus.PENDING.name }
    val missedTasks = allTasks.filter { it.status == TaskStatus.NOT_DONE.name }
    val todayCompleted = todayTasks.count { it.status == TaskStatus.DONE.name }
    val completionRate = if (allTasks.isEmpty()) 0f else completedTasks.size.toFloat() / allTasks.size.toFloat()
    val todayRate = if (todayTasks.isEmpty()) 0f else todayCompleted.toFloat() / todayTasks.size.toFloat()

    val visibleTasks = when (selectedFilter) {
        AnalyticsFilter.ALL -> allTasks
        AnalyticsFilter.TODAY -> todayTasks
        AnalyticsFilter.DONE -> completedTasks
        AnalyticsFilter.PENDING -> pendingTasks
        AnalyticsFilter.MISSED -> missedTasks
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderCard(
                onRefresh = { viewModel.loadHomeData() },
                onReset = { showResetDialog = true }
            )
        }

        item {
            AiCoachCard(
                state = insightsState,
                onGenerate = { viewModel.generateInsights() }
            )
        }

        item {
            CompletionCard(
                title = "Overall completion",
                subtitle = "${completedTasks.size} of ${allTasks.size} tasks completed",
                progress = completionRate,
                icon = Icons.Rounded.AutoGraph,
                onClick = { selectedFilter = AnalyticsFilter.ALL }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Today",
                    value = todayTasks.size.toString(),
                    subtitle = "${(todayRate * 100).toInt()}% done",
                    icon = Icons.Rounded.Today,
                    selected = selectedFilter == AnalyticsFilter.TODAY,
                    onClick = { selectedFilter = AnalyticsFilter.TODAY }
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total",
                    value = allTasks.size.toString(),
                    subtitle = "all tasks",
                    icon = Icons.Rounded.Checklist,
                    selected = selectedFilter == AnalyticsFilter.ALL,
                    onClick = { selectedFilter = AnalyticsFilter.ALL }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Done",
                    value = completedTasks.size.toString(),
                    subtitle = "completed",
                    icon = Icons.Rounded.CheckCircle,
                    accent = ApzoSuccess,
                    selected = selectedFilter == AnalyticsFilter.DONE,
                    onClick = { selectedFilter = AnalyticsFilter.DONE }
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending",
                    value = pendingTasks.size.toString(),
                    subtitle = "to finish",
                    icon = Icons.Rounded.PendingActions,
                    selected = selectedFilter == AnalyticsFilter.PENDING,
                    onClick = { selectedFilter = AnalyticsFilter.PENDING }
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Missed",
                    value = missedTasks.size.toString(),
                    subtitle = "not done",
                    icon = Icons.Rounded.Cancel,
                    accent = ApzoError,
                    selected = selectedFilter == AnalyticsFilter.MISSED,
                    onClick = { selectedFilter = AnalyticsFilter.MISSED }
                )
            }
        }

        item {
            FilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it }
            )
        }

        item {
            TaskListHeader(
                selectedFilter = selectedFilter,
                count = visibleTasks.size
            )
        }

        if (visibleTasks.isEmpty()) {
            item {
                EmptyFilteredTasksCard(selectedFilter)
            }
        } else {
            items(visibleTasks, key = { it.id }) { task ->
                AnalyticsTaskRow(
                    task = task,
                    onClick = { onTaskClick(task.id) }
                )
            }
        }

        item {
            StatusBreakdownCard(
                total = allTasks.size,
                done = completedTasks.size,
                pending = pendingTasks.size,
                missed = missedTasks.size,
                selectedFilter = selectedFilter,
                onFilterChange = { selectedFilter = it }
            )
        }

        item {
            SmartTipCard(
                pendingCount = pendingTasks.size,
                completionRate = completionRate,
                onClick = { selectedFilter = AnalyticsFilter.PENDING }
            )
        }
    }
}

@Composable
private fun HeaderCard(
    onRefresh: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Insights",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Track your focus and progress",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ApzoError.copy(alpha = 0.10f))
                    .clickable { onReset() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Reset all data",
                    tint = ApzoError,
                    modifier = Modifier.size(23.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Refresh analytics",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun AiCoachCard(
    state: AiInsightsUiState,
    onGenerate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Coach", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        "Weekly summary and what to do next",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!state.isLoading) {
                    IconButton(onClick = onGenerate) {
                        Icon(
                            Icons.Rounded.Refresh, "Generate insights",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when {
                state.isLoading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }

                state.error != null -> {
                    Text(
                        state.error,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onGenerate, shape = RoundedCornerShape(12.dp)) {
                        Text("Try Again")
                    }
                }

                state.insights != null -> {
                    AiCoachContent(state.insights)
                }

                else -> {
                    Text(
                        "Get an AI summary of your week, why tasks were missed, and what to focus on next.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onGenerate,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Insights", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCoachContent(insights: AiInsightsResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InsightRow(
            icon = Icons.Rounded.AutoGraph,
            label = "This week",
            text = insights.weeklySummary,
            accent = MaterialTheme.colorScheme.primary
        )
        if (insights.missedReason.isNotBlank()) {
            InsightRow(
                icon = Icons.Rounded.Cancel,
                label = "Why tasks were missed",
                text = insights.missedReason,
                accent = ApzoError
            )
        }
        if (insights.productivityTip.isNotBlank()) {
            InsightRow(
                icon = Icons.Rounded.Lightbulb,
                label = "Tip",
                text = insights.productivityTip,
                accent = ApzoSuccess
            )
        }
        if (insights.nextBestAction.isNotBlank()) {
            InsightRow(
                icon = Icons.Rounded.TaskAlt,
                label = "Next best action",
                text = insights.nextBestAction,
                accent = MaterialTheme.colorScheme.primary
            )
        }
        if (insights.tomorrowSuggestion.isNotBlank()) {
            InsightRow(
                icon = Icons.Rounded.Today,
                label = "Tomorrow",
                text = insights.tomorrowSuggestion,
                accent = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    label: String,
    text: String,
    accent: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(9.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CompletionCard(
    title: String,
    subtitle: String,
    progress: Float,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.84f)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            subtitle,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.18f)
                )
            }
        }
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color = MaterialTheme.colorScheme.primary,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = MaterialTheme.colorScheme.surface
    val softBorderColor = if (selected) accent.copy(alpha = 0.18f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)

    Card(
        modifier = modifier
            .heightIn(min = 118.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (selected) 2.dp else 1.dp),
        border = BorderStroke(1.dp, softBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(
                subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: AnalyticsFilter,
    onFilterChange: (AnalyticsFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

@Composable
private fun TaskListHeader(selectedFilter: AnalyticsFilter, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${selectedFilter.label} tasks",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tap any task to open details",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f)
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AnalyticsTaskRow(
    task: MainTask,
    onClick: () -> Unit
) {
    val sc = statusColor(task.status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(sc)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(sc.copy(alpha = 0.11f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusLabel(task.status),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = sc
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = task.startDate.ifBlank { "No date" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = task.title.ifBlank { "Untitled task" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = "Open task",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun StatusBreakdownCard(
    total: Int,
    done: Int,
    pending: Int,
    missed: Int,
    selectedFilter: AnalyticsFilter,
    onFilterChange: (AnalyticsFilter) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Status breakdown", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            StatusBarRow(
                label = "Done",
                count = done,
                total = total,
                color = ApzoSuccess,
                selected = selectedFilter == AnalyticsFilter.DONE,
                onClick = { onFilterChange(AnalyticsFilter.DONE) }
            )
            StatusBarRow(
                label = "Pending",
                count = pending,
                total = total,
                color = MaterialTheme.colorScheme.primary,
                selected = selectedFilter == AnalyticsFilter.PENDING,
                onClick = { onFilterChange(AnalyticsFilter.PENDING) }
            )
            StatusBarRow(
                label = "Missed",
                count = missed,
                total = total,
                color = ApzoError,
                selected = selectedFilter == AnalyticsFilter.MISSED,
                onClick = { onFilterChange(AnalyticsFilter.MISSED) }
            )
        }
    }
}

@Composable
private fun StatusBarRow(
    label: String,
    count: Int,
    total: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val progress = if (total == 0) 0f else count.toFloat() / total.toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) color.copy(alpha = 0.07f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("$count", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = color,
            trackColor = color.copy(alpha = 0.12f)
        )
    }
}

@Composable
private fun SmartTipCard(
    pendingCount: Int,
    completionRate: Float,
    onClick: () -> Unit
) {
    val message = when {
        pendingCount == 0 -> "Great job. You have no pending tasks right now."
        completionRate >= 0.7f -> "You are doing well. Focus on the remaining $pendingCount pending task${if (pendingCount == 1) "" else "s"}."
        else -> "Try splitting large tasks into smaller subtasks and complete the first small step today."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Smart tip", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    message,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyFilteredTasksCard(selectedFilter: AnalyticsFilter) {
    val message = when (selectedFilter) {
        AnalyticsFilter.ALL -> "Create a task to start seeing live analytics here."
        AnalyticsFilter.TODAY -> "No tasks are scheduled for today."
        AnalyticsFilter.DONE -> "No completed tasks yet. Finish one task and it will appear here."
        AnalyticsFilter.PENDING -> "No pending tasks. Nice work."
        AnalyticsFilter.MISSED -> "No missed tasks right now."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.QueryStats,
                    null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No ${selectedFilter.label.lowercase()} tasks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
        }
    }
}