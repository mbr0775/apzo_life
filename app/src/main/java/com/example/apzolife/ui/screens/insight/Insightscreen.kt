package com.example.apzolife.ui.screens.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.ui.components.InsightCard
import com.example.apzolife.ui.components.getCategoryColor
import com.example.apzolife.ui.theme.ApzoError
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.ui.theme.ApzoWarning
import com.example.apzolife.viewmodel.ApzoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightScreen(viewModel: ApzoViewModel) {
    val state by viewModel.homeState.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    val totalTasks = state.stats.totalTasks
    val completedTasks = state.stats.completedTasks
    val pendingTasks = state.stats.pendingTasks
    val notDoneTasks = state.allTasks.count { it.status == TaskStatus.NOT_DONE.name }
    val completionRate = if (totalTasks > 0) completedTasks * 100f / totalTasks else 0f

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Hero header ──────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A1628),
                                    Color(0xFF0F2744),
                                    Color(0xFF1A3A5C).copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF6C5CE7).copy(alpha = 0.18f))
                    )
                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Insights, null, tint = Color(0xFF8DCDE5), modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text(
                                    "Task Intelligence",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    "Smart suggestions for you",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Performance overview card ────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Title row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Performance", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("All-time statistics", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            // Completion ring badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF00B894).copy(alpha = 0.15f), Color(0xFF00CEC9).copy(alpha = 0.1f))
                                        )
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "${completionRate.toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00B894)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Completion bar
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Completion rate", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("${completionRate.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ApzoSuccess)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ApzoSuccess.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((completionRate / 100f).coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF00B894), Color(0xFF00CEC9)))
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 4-stat grid
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InsightMiniStat("Total", "$totalTasks", Icons.Rounded.TaskAlt, Color(0xFF8DCDE5), Modifier.weight(1f))
                            InsightMiniStat("Done", "$completedTasks", Icons.Rounded.CheckCircle, ApzoSuccess, Modifier.weight(1f))
                            InsightMiniStat("Pending", "$pendingTasks", Icons.Rounded.PendingActions, ApzoWarning, Modifier.weight(1f))
                            InsightMiniStat("Missed", "$notDoneTasks", Icons.AutoMirrored.Rounded.TrendingUp, ApzoError, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Average progress bar
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Average progress", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("${state.stats.overallProgress}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8DCDE5))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (state.stats.overallProgress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                            color = Color(0xFF1D546C),
                            trackColor = Color(0xFF1D546C).copy(alpha = 0.12f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            // ── Category breakdown ───────────────────────────────────────────
            item {
                if (state.allTasks.isNotEmpty()) {
                    CategoryBreakdownCard(tasks = state.allTasks)
                }
            }

            // ── AI Insights header ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Smart Suggestions",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${state.aiInsights.size} insight(s) for you",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "${state.aiInsights.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (state.aiInsights.isEmpty()) {
                item { InsightEmptyState() }
            } else {
                items(state.aiInsights) { insight ->
                    InsightCard(
                        insight = insight,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightMiniStat(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CategoryBreakdownCard(tasks: List<com.example.apzolife.data.model.MainTask>) {
    val categoryGroups = tasks.groupBy { it.category }.ifEmpty { return }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("By Category", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${categoryGroups.size} categories in use",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            categoryGroups.forEach { (categoryName, catTasks) ->
                val category = com.example.apzolife.data.model.TaskCategory.fromString(categoryName)
                val catColor = getCategoryColor(category)
                val done = catTasks.count { it.status == TaskStatus.DONE.name }
                val ratio = if (catTasks.isNotEmpty()) done.toFloat() / catTasks.size else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(catColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(category.emoji, fontSize = 14.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category.displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("$done/${catTasks.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = catColor)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(catColor.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(catColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1D546C).copy(alpha = 0.15f), Color(0xFF6C5CE7).copy(alpha = 0.1f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Insights, null, tint = Color(0xFF8DCDE5), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("No insights yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Add tasks and complete them\nto unlock smart suggestions.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}