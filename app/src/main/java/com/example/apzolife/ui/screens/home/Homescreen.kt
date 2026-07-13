package com.example.apzolife.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.ai.AiDailyPlanResult
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.components.TaskCard
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.ui.theme.ApzoWarning
import com.example.apzolife.viewmodel.ApzoViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ApzoViewModel,
    onAddTask: () -> Unit,
    onTaskClick: (String) -> Unit,
    onOpenChat: () -> Unit
) {
    val state by viewModel.homeState.collectAsState()
    val planState by viewModel.dailyPlanState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPlanSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(planState.plan) {
        if (planState.plan != null) showPlanSheet = true
    }

    LaunchedEffect(planState.error) {
        planState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDailyPlan()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                text = { Text("Add Task", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Rounded.Add, null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item { HomeHeader(onRefresh = { viewModel.loadHomeData() }) }

            // Quick stats
            item {
                val total   = state.allTasks.size
                val done    = state.allTasks.count { it.status == TaskStatus.DONE.name }
                val pending = state.allTasks.count { it.status == TaskStatus.PENDING.name }
                StatsRow(total = total, done = done, pending = pending)
            }

            // Plan My Day (AI)
            item {
                PlanMyDayCard(
                    isLoading = planState.isLoading,
                    onClick = { viewModel.planMyDay() }
                )
                Spacer(Modifier.height(4.dp))
            }

            // Ask Apzo AI (chat assistant)
            item {
                AskApzoAiCard(onClick = onOpenChat)
                Spacer(Modifier.height(4.dp))
            }

            // Today tasks header
            item {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                    .height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)))
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Today's Tasks", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (state.todayTasks.isEmpty()) "Nothing scheduled today"
                            else "${state.todayTasks.size} task(s)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Surface(shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                        Text("${state.todayTasks.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Task list
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (state.todayTasks.isEmpty()) {
                item { EmptyState(modifier = Modifier.padding(horizontal = 20.dp)) }
            } else {
                items(state.todayTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onDone = { viewModel.markTaskDone(task.id) },
                        onNotDone = { viewModel.markTaskNotDone(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onCardClick = { onTaskClick(task.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (showPlanSheet && planState.plan != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showPlanSheet = false
                    viewModel.clearDailyPlan()
                }
            ) {
                DailyPlanSheetContent(
                    plan = planState.plan!!,
                    onTaskClick = { taskId ->
                        showPlanSheet = false
                        viewModel.clearDailyPlan()
                        onTaskClick(taskId)
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(onRefresh: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when { hour < 12 -> "Good Morning ☀️"; hour < 17 -> "Good Afternoon 🌤"; else -> "Good Evening 🌙" }
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))

    Box(
        modifier = Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(
                Color(0xFF0A1628), Color(0xFF0F2744),
                Color(0xFF1D546C).copy(alpha = 0.7f),
                MaterialTheme.colorScheme.background
            ))
        )
    ) {
        Box(modifier = Modifier.size(200.dp).offset((-50).dp, (-40).dp)
            .clip(CircleShape).background(Color.White.copy(alpha = 0.04f)))
        Box(modifier = Modifier.size(130.dp).align(Alignment.TopEnd)
            .clip(CircleShape).background(Color(0xFF00B894).copy(alpha = 0.12f)))

        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Top) {
                Column {
                    Text(greeting, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Task Dashboard", color = Color.White, fontSize = 28.sp,
                        fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text(dateText, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Rounded.Refresh, "Refresh", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun StatsRow(total: Int, done: Int, pending: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard("Total", total.toString(), Icons.Rounded.TaskAlt,
            MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        StatCard("Done", done.toString(), Icons.Rounded.CheckCircle,
            ApzoSuccess, Modifier.weight(1f))
        StatCard("Pending", pending.toString(), Icons.Rounded.PendingActions,
            ApzoWarning, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun PlanMyDayCard(isLoading: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Plan My Day", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Let AI order today's pending tasks",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Plan")
                }
            }
        }
    }
}

/** Entry point into the "Ask Apzo AI" chat assistant screen. */
@Composable
private fun AskApzoAiCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00B894).copy(alpha = 0.10f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF00B894).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color(0xFF00B894))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Ask Apzo AI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    "Chat about your tasks, get plans, break things down",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                null,
                tint = Color(0xFF00B894)
            )
        }
    }
}

@Composable
private fun DailyPlanSheetContent(
    plan: AiDailyPlanResult,
    onTaskClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Today's Plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (plan.planSummary.isNotBlank()) {
            Text(
                plan.planSummary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (plan.items.isEmpty()) {
            Text(
                "AI couldn't generate a plan this time. Try again.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        plan.items.forEach { item ->
            val clickable = item.taskId != null
            Card(
                modifier = if (clickable) {
                    Modifier.fillMaxWidth()
                        .then(Modifier)
                } else Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (clickable) base.clickable { onTaskClick(item.taskId!!) }
                            else base
                        }
                        .padding(14.dp)
                ) {
                    Box(modifier = Modifier.width(76.dp)) {
                        Text(
                            item.timeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        if (item.note.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                item.note,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (item.durationMinutes > 0) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "${item.durationMinutes} min",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (clickable) {
                        Icon(
                            Icons.Rounded.KeyboardArrowRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp).align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}

/** Small helper so the plan-item row is only clickable when it maps to a real task. */
private fun Modifier.clickableCard(onClick: () -> Unit): Modifier = this.then(
    Modifier
).let { base ->
    // Using composed-free approach: this is applied inside a @Composable scope already
    // via DailyPlanSheetContent, so a direct clickable() call is simpler there in practice.
    base
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(18.dp), modifier = Modifier.size(72.dp)) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Icon(Icons.Rounded.TaskAlt, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("No tasks today", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Tap Add Task to plan your day.", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
        }
    }
}