package com.example.apzolife.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.data.model.AiInsight
import com.example.apzolife.data.model.DailyStats
import com.example.apzolife.data.model.InsightType
import com.example.apzolife.ui.components.TaskCard
import com.example.apzolife.ui.theme.ApzoSuccess
import com.example.apzolife.ui.theme.ApzoTertiary
import com.example.apzolife.ui.theme.ApzoWarning
import com.example.apzolife.viewmodel.ApzoViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: ApzoViewModel,
    onAddTask: () -> Unit,
    onTaskClick: (String) -> Unit
) {
    val state by viewModel.homeState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadHomeData()
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTask,
                text = {
                    Text("Add Task", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                },
                icon = {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.height(52.dp)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {

            // ── Hero Header ─────────────────────────────────────────────────────
            item {
                ModernHomeHeader(
                    progress = state.stats.overallProgress,
                    onRefresh = { viewModel.loadHomeData() }
                )
            }

            // ── Stats Row ───────────────────────────────────────────────────────
            item {
                ModernStatsRow(stats = state.stats)
            }

            // ── Task Insights ───────────────────────────────────────────────────
            if (state.aiInsights.isNotEmpty()) {
                item {
                    InsightsSectionHeader(count = state.aiInsights.size)
                }

                itemsIndexed(state.aiInsights) { index, insight ->
                    ModernInsightCard(
                        insight = insight,
                        index = index,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)
                    )
                }
            }

            // ── Divider ─────────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.height(20.dp))
                TodayTasksHeader(count = state.todayTasks.size)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Today Tasks ─────────────────────────────────────────────────────
            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            } else if (state.todayTasks.isEmpty()) {
                item {
                    ModernEmptyTasksPlaceholder(hasUpcomingTasks = state.allTasks.isNotEmpty())
                }
            } else {
                items(state.todayTasks) { task ->
                    TaskCard(
                        task = task,
                        onDone = { viewModel.markTaskDone(task.id) },
                        onNotDone = { viewModel.markTaskNotDone(task.id) },
                        onEdit = { onTaskClick(task.id) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onProgressChange = { progress ->
                            viewModel.updateTaskProgressManual(task.id, progress)
                        },
                        onCardClick = { onTaskClick(task.id) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HERO HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernHomeHeader(
    progress: Int,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
    val currentHour = LocalTime.now().hour
    val greeting = when (currentHour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }
    val greetingEmoji = when (currentHour) {
        in 0..11 -> "☀️"
        in 12..16 -> "👋"
        else -> "🌙"
    }

    // Subtle pulse animation on the ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1F30),
                        Color(0xFF112840),
                        Color(0xFF143352)
                    )
                )
            )
    ) {
        // Decorative orbs
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = 200.dp, y = (-80).dp)
                .clip(CircleShape)
                .background(Color(0xFF1D546C).copy(alpha = pulseAlpha + 0.04f))
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 270.dp, y = 20.dp)
                .clip(CircleShape)
                .background(Color(0xFF00B894).copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(80.dp)
                .offset(x = (-20).dp, y = 80.dp)
                .clip(CircleShape)
                .background(Color(0xFF6C5CE7).copy(alpha = 0.07f))
        )

        // Mesh gradient overlay
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E7A9A).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = size.width * 0.5f
                )
            )
        }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 24.dp, end = 22.dp, top = 22.dp, bottom = 78.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Greeting pill
                    Surface(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "$greetingEmoji  $greeting",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Let's be\nproductive!",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.8).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date with subtle line accent
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF00E5B0), Color(0xFF1D9FBF))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = today.format(formatter),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Right: Refresh + Progress Ring
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    ModernProgressRing(
                        progress = progress / 100f,
                        size = 78
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernProgressRing(progress: Float, size: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.5.dp.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Track
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            // Glow layer
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00E5B0).copy(alpha = 0.3f),
                        Color(0xFF00E5B0)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth + 3.dp.toPx(), cap = StrokeCap.Round)
            )
            // Main arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0xFF0BD8A4), Color(0xFF00E5B0))
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = stroke
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "done",
                fontSize = 9.sp,
                color = Color(0xFF00E5B0).copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATS ROW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernStatsRow(stats: DailyStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .offset(y = (-36).dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernStatItem("All Tasks", "${stats.totalTasks}", Icons.Rounded.TaskAlt,
                Color(0xFF1D7FC4), Color(0xFFE3F2FF))
            StatDivider()
            ModernStatItem("Completed", "${stats.completedTasks}", Icons.Rounded.CheckCircle,
                ApzoSuccess, Color(0xFFE6FBF4))
            StatDivider()
            ModernStatItem("Pending", "${stats.pendingTasks}", Icons.Rounded.PendingActions,
                ApzoWarning, Color(0xFFFFF8E6))
            StatDivider()
            ModernStatItem("Progress", "${stats.overallProgress}%",
                Icons.AutoMirrored.Rounded.TrendingUp, ApzoTertiary, Color(0xFFE6F9F5))
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(44.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    )
}

@Composable
private fun ModernStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// INSIGHTS SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InsightsSectionHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Icon with gradient bg
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1D546C), Color(0xFF6C5CE7))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column {
                Text(
                    text = "Task Insights",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Smart suggestions for you",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }

        // Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1D546C).copy(alpha = 0.15f), Color(0xFF6C5CE7).copy(alpha = 0.15f))
                    )
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B5CF0)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MODERN INSIGHT CARD
// ─────────────────────────────────────────────────────────────────────────────

private data class InsightStyle(
    val gradientColors: List<Color>,
    val bgLight: Color,
    val bgDark: Color,
    val accentColor: Color,
    val borderColor: Color,
    val icon: ImageVector,
    val label: String
)

private fun getInsightStyle(type: InsightType): InsightStyle = when (type) {
    InsightType.ACHIEVEMENT -> InsightStyle(
        gradientColors = listOf(Color(0xFF00B894), Color(0xFF00CEC9)),
        bgLight = Color(0xFFEAFDF8),
        bgDark = Color(0xFF0A1F1C),
        accentColor = Color(0xFF00B894),
        borderColor = Color(0xFF00B894).copy(alpha = 0.2f),
        icon = Icons.Rounded.EmojiEvents,
        label = "Achievement"
    )
    InsightType.MOTIVATION -> InsightStyle(
        gradientColors = listOf(Color(0xFF1D546C), Color(0xFF2E7A9A)),
        bgLight = Color(0xFFEAF4FB),
        bgDark = Color(0xFF0A1920),
        accentColor = Color(0xFF2E9AC4),
        borderColor = Color(0xFF2E9AC4).copy(alpha = 0.2f),
        icon = Icons.Rounded.Bolt,
        label = "Motivation"
    )
    InsightType.WARNING -> InsightStyle(
        gradientColors = listOf(Color(0xFFE6A817), Color(0xFFFDCB6E)),
        bgLight = Color(0xFFFFFAED),
        bgDark = Color(0xFF201A08),
        accentColor = Color(0xFFE6A817),
        borderColor = Color(0xFFE6A817).copy(alpha = 0.2f),
        icon = Icons.Rounded.Warning,
        label = "Warning"
    )
    InsightType.SUGGESTION -> InsightStyle(
        gradientColors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE)),
        bgLight = Color(0xFFF2F0FF),
        bgDark = Color(0xFF120F20),
        accentColor = Color(0xFF7B5CF0),
        borderColor = Color(0xFF7B5CF0).copy(alpha = 0.2f),
        icon = Icons.Rounded.Lightbulb,
        label = "Suggestion"
    )
}

@Composable
fun ModernInsightCard(
    insight: AiInsight,
    index: Int,
    modifier: Modifier = Modifier
) {
    val style = getInsightStyle(insight.type)
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBg = if (isDark) style.bgDark else style.bgLight

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            style.accentColor.copy(alpha = if (isDark) 0.06f else 0.04f),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = 400f
                    )
                )
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(100.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                    .background(
                        Brush.verticalGradient(style.gradientColors)
                    )
            )

            Column(modifier = Modifier.padding(start = 18.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Icon container with gradient
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(style.gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        // Label pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(style.accentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = style.label.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = style.accentColor,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        Text(
                            text = insight.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 21.sp,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Action question
                insight.actionQuestion?.let { question ->
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(style.accentColor.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(style.accentColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Psychology,
                                    contentDescription = null,
                                    tint = style.accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodySmall,
                                color = style.accentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TODAY TASKS HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayTasksHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Today's Tasks",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.3).sp
            )
            Text(
                text = "$count task(s) scheduled today",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernEmptyTasksPlaceholder(hasUpcomingTasks: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFE3F2FF), Color(0xFFCCE8F8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📋", fontSize = 34.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No tasks for today",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasUpcomingTasks) {
                    "You have tasks in your app, but\nnone are scheduled for today."
                } else {
                    "Tap Add Task to create your\nfirst task and get started."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}