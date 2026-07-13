package com.example.apzolife.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apzolife.ui.components.TaskCard
import com.example.apzolife.viewmodel.ApzoViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: ApzoViewModel, onTaskClick: (String) -> Unit) {
    val state by viewModel.homeState.collectAsState()
    var selectedDate   by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth   by remember { mutableStateOf(YearMonth.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    val filteredTasks = state.allTasks
        .filter { it.startDate == selectedDate.toString() }
        .sortedBy { it.startTime }

    val datesWithTasks = state.allTasks.map { it.startDate }.toSet()

    if (showDatePicker) {
        val ms = try { selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        catch (_: Exception) { System.currentTimeMillis() }
        val dpState = rememberDatePickerState(initialSelectedDateMillis = ms)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        currentMonth = YearMonth.of(selectedDate.year, selectedDate.month)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().background(
                        Brush.verticalGradient(listOf(
                            Color(0xFF0A1628), Color(0xFF0F2744),
                            Color(0xFF1D546C).copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        ))
                    )
                ) {
                    Column(
                        modifier = Modifier.statusBarsPadding()
                            .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column {
                                Text("Calendar", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("Browse tasks by date", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.CalendarMonth, null,
                                        tint = Color(0xFF8DCDE5), modifier = Modifier.size(15.dp))
                                    Text(
                                        try { selectedDate.format(DateTimeFormatter.ofPattern("dd MMM")) }
                                        catch (_: Exception) { "Pick" },
                                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Mini month calendar
            item {
                MonthCalendar(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    datesWithTasks = datesWithTasks,
                    onDateSelected = { selectedDate = it },
                    onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
                )
            }

            // Date label
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            try { selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }
                            catch (_: Exception) { "" },
                            fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Text("${filteredTasks.size} task(s)", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface).padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📅", fontSize = 28.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No tasks this day", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Pick another date or add a task.", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
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
    }
}

@Composable
private fun MonthCalendar(
    currentMonth: YearMonth, selectedDate: LocalDate,
    datesWithTasks: Set<String>,
    onDateSelected: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit, onNextMonth: () -> Unit
) {
    val today = LocalDate.now()
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
    val dayNames = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Rounded.ChevronLeft, null, modifier = Modifier.size(18.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text(currentMonth.year.toString(), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                IconButton(onClick = onNextMonth, modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                dayNames.forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                }
            }
            Spacer(Modifier.height(8.dp))
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val dayNum = row * 7 + col - firstDayOfWeek + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Box(Modifier.weight(1f).height(40.dp))
                        } else {
                            val date = try { currentMonth.atDay(dayNum) } catch (_: Exception) { null }
                            if (date == null) Box(Modifier.weight(1f).height(40.dp))
                            else {
                                val isSelected = date == selectedDate
                                val isToday = date == today
                                val hasTask = datesWithTasks.contains(date.toString())
                                Box(modifier = Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier.size(30.dp).clip(CircleShape)
                                                .then(
                                                    if (isSelected) Modifier.background(
                                                        Brush.linearGradient(listOf(Color(0xFF1D546C), Color(0xFF00B894)))
                                                    )
                                                    else if (isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                                                    else Modifier
                                                )
                                                .clickable { onDateSelected(date) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(dayNum.toString(), fontSize = 12.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when { isSelected -> Color.White; isToday -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurface })
                                        }
                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape)
                                            .background(when { hasTask && isSelected -> Color.White.copy(0.9f); hasTask -> Color(0xFF00B894); else -> Color.Transparent }))
                                    }
                                }
                            }
                        }
                    }
                }
                if (row < rows - 1) Spacer(Modifier.height(2.dp))
            }
        }
    }
}