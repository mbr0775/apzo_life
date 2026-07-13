package com.example.apzolife.ui.screens.completed

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.components.TaskCard
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.viewmodel.ApzoViewModel

@Composable
fun CompletedScreen(viewModel: ApzoViewModel, onTaskClick: (String) -> Unit) {
    val state by viewModel.homeState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
        viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
    }

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
                        viewModel.resetAllData()
                    }
                ) { Text("Yes, delete all", color = ApzoError, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    val completedTasks = state.allTasks
        .filter { it.status == TaskStatus.DONE.name }
        .sortedByDescending { "${it.startDate} ${it.startTime}" }

    val total = state.allTasks.size
    val rate  = if (total > 0) completedTasks.size.toFloat() / total else 0f

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Box(
                modifier = Modifier.fillMaxWidth().background(
                    Brush.verticalGradient(listOf(Color(0xFF1D546C), Color(0xFF0F3246), Color(0xFF0A1F2E)))
                )
            ) {
                Box(modifier = Modifier.size(140.dp).align(Alignment.TopEnd)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.04f)))
                Column(
                    modifier = Modifier.statusBarsPadding()
                        .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CheckCircle, null,
                                    tint = Color(0xFF00E5B0), modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text("Completed", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("${completedTasks.size} tasks done", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }

                        IconButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Reset all data", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    when {
                                        rate >= 0.8f -> "Outstanding!"
                                        rate >= 0.5f -> "Good progress!"
                                        completedTasks.isNotEmpty() -> "Keep going!"
                                        else -> "Start completing!"
                                    },
                                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("${completedTasks.size} of $total tasks completed",
                                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                            Box(
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp))
                                    .background(ApzoSuccess.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.EmojiEvents, null, tint = ApzoSuccess, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Completion rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(rate * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ApzoSuccess)
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { rate.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                            color = ApzoSuccess, trackColor = ApzoSuccess.copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Completed Tasks", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Tap any task to open details", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                    }
                    Surface(color = ApzoSuccess.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                        Text("${completedTasks.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ApzoSuccess)
                    }
                }
            }

            if (completedTasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆", fontSize = 36.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("No completed tasks yet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("Complete your first task and\nit will appear here.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center, lineHeight = 20.sp)
                        }
                    }
                }
            } else {
                items(completedTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onDone = {},
                        onNotDone = { viewModel.markTaskNotDone(task.id) },
                        onDelete = {},
                        onCardClick = { onTaskClick(task.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        showDelete = false
                    )
                }
            }
        }
    }
}
