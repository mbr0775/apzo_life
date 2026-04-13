package com.example.apzolife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apzolife.data.model.AiInsight
import com.example.apzolife.data.model.DailyStats
import com.example.apzolife.data.model.InsightType
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskCategory
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.data.repository.ApzoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val todayTasks: List<MainTask> = emptyList(),
    val allTasks: List<MainTask> = emptyList(),
    val stats: DailyStats = DailyStats(0, 0, 0, 0),
    val aiInsights: List<AiInsight> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TaskDetailUiState(
    val task: MainTask? = null,
    val subtasks: List<SubTask> = emptyList(),
    val isLoading: Boolean = false
)

class ApzoViewModel : ViewModel() {

    private val repo = ApzoRepository

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _taskDetailState = MutableStateFlow(TaskDetailUiState())
    val taskDetailState: StateFlow<TaskDetailUiState> = _taskDetailState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _taskCreated = MutableSharedFlow<Unit>()
    val taskCreated: SharedFlow<Unit> = _taskCreated.asSharedFlow()

    init {
        loadHomeData()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Home / all tasks
    // ─────────────────────────────────────────────────────────────────────────

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true) }
            val todayTasks = repo.getTodayTasks()
            val allTasks   = repo.getAllTasks()
            val stats      = repo.getDailyStats()
            val insights   = generateInsights(stats, todayTasks, allTasks)
            _homeState.update {
                it.copy(
                    todayTasks = todayTasks,
                    allTasks   = allTasks,
                    stats      = stats,
                    aiInsights = insights,
                    isLoading  = false,
                    error      = null
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task CRUD
    // ─────────────────────────────────────────────────────────────────────────

    fun createTask(
        title: String,
        category: TaskCategory,
        dateScheduled: String,
        timeScheduled: String,
        notes: String,
        categoryData: String = "{}"
    ) {
        viewModelScope.launch {
            val uid = repo.currentUserId()
            if (uid == null) {
                _snackbarMessage.emit("User not logged in.")
                return@launch
            }
            val task = MainTask(
                id                 = UUID.randomUUID().toString(),
                userId             = uid,
                title              = title,
                category           = category.name,
                dateScheduled      = dateScheduled,
                timeScheduled      = timeScheduled,
                status             = TaskStatus.PENDING.name,
                progressPercentage = 0,
                notes              = notes,
                createdAt          = null,
                categoryData       = if (categoryData.isBlank()) "{}" else categoryData
            )
            val success = repo.insertTask(task)
            if (success) {
                loadHomeData()
                _snackbarMessage.emit("Task created successfully!")
                _taskCreated.emit(Unit)
            } else {
                _snackbarMessage.emit("Failed to create task.")
            }
        }
    }

    /**
     * Full update — called from EditTaskScreen.
     * Updates title, category, date, time, notes AND progress/status in one shot.
     */
    fun updateTask(
        taskId: String,
        title: String,
        dateScheduled: String,
        timeScheduled: String,
        notes: String,
        category: TaskCategory? = null,
        progress: Int? = null
    ) {
        viewModelScope.launch {
            val current = repo.getTaskById(taskId)
            if (current == null) {
                _snackbarMessage.emit("Task not found.")
                return@launch
            }

            // Derive new status from progress if progress was supplied
            val newProgress = progress ?: current.progressPercentage
            val newStatus   = when {
                progress == null               -> current.status
                newProgress >= 100             -> TaskStatus.DONE.name
                newProgress > 0               -> TaskStatus.IN_PROGRESS.name
                else                           -> TaskStatus.PENDING.name
            }

            val updated = current.copy(
                title              = title,
                category           = category?.name ?: current.category,
                dateScheduled      = dateScheduled,
                timeScheduled      = timeScheduled,
                notes              = notes,
                progressPercentage = newProgress,
                status             = newStatus
            )

            val success = repo.updateTask(updated)
            if (success) {
                loadHomeData()
                loadTaskDetail(taskId)
                _snackbarMessage.emit("Task updated")
            } else {
                _snackbarMessage.emit("Failed to update task.")
            }
        }
    }

    fun markTaskDone(taskId: String) {
        viewModelScope.launch {
            repo.updateTaskStatus(taskId, TaskStatus.DONE)
            repo.updateTaskProgress(taskId, 100)
            loadHomeData()
            loadTaskDetail(taskId)
        }
    }

    fun markTaskNotDone(taskId: String) {
        viewModelScope.launch {
            repo.updateTaskStatus(taskId, TaskStatus.NOT_DONE)
            repo.updateTaskProgress(taskId, 0)
            loadHomeData()
            loadTaskDetail(taskId)
        }
    }

    fun updateTaskProgressManual(taskId: String, progress: Int) {
        viewModelScope.launch {
            repo.updateTaskProgress(taskId, progress)
            val status = when {
                progress >= 100 -> TaskStatus.DONE
                progress > 0    -> TaskStatus.IN_PROGRESS
                else            -> TaskStatus.PENDING
            }
            repo.updateTaskStatus(taskId, status)
            loadHomeData()
            loadTaskDetail(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repo.deleteTask(taskId)
            loadHomeData()
            _snackbarMessage.emit("Task deleted")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task detail
    // ─────────────────────────────────────────────────────────────────────────

    fun loadTaskDetail(taskId: String) {
        viewModelScope.launch {
            _taskDetailState.update { it.copy(isLoading = true) }
            val task     = repo.getTaskById(taskId)
            val subtasks = repo.getSubtasksForTask(taskId)
            _taskDetailState.update {
                it.copy(task = task, subtasks = subtasks, isLoading = false)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subtask CRUD
    // ─────────────────────────────────────────────────────────────────────────

    fun addSubtask(taskId: String, title: String, description: String, timeScheduled: String) {
        viewModelScope.launch {
            val uid = repo.currentUserId()
            if (uid == null) {
                _snackbarMessage.emit("User not logged in.")
                return@launch
            }
            val subtask = SubTask(
                id                 = UUID.randomUUID().toString(),
                taskId             = taskId,
                userId             = uid,
                title              = title,
                description        = description,
                status             = TaskStatus.PENDING.name,
                timeScheduled      = timeScheduled,
                progressPercentage = 0,
                orderIndex         = _taskDetailState.value.subtasks.size
            )
            val success = repo.insertSubtask(subtask)
            if (success) {
                loadTaskDetail(taskId)
                recalculateTaskProgress(taskId)
            } else {
                _snackbarMessage.emit("Failed to add subtask.")
            }
        }
    }

    fun updateSubtask(
        subtaskId: String,
        taskId: String,
        newTitle: String,
        newDescription: String,
        newTime: String
    ) {
        viewModelScope.launch {
            val current = _taskDetailState.value.subtasks.find { it.id == subtaskId }
            if (current == null) {
                _snackbarMessage.emit("Subtask not found.")
                return@launch
            }
            val updated = current.copy(
                title         = newTitle,
                description   = newDescription,
                timeScheduled = newTime
            )
            val success = repo.updateSubtask(updated)
            if (success) {
                loadTaskDetail(taskId)
                _snackbarMessage.emit("Subtask updated")
            } else {
                _snackbarMessage.emit("Failed to update subtask.")
            }
        }
    }

    fun markSubtaskDone(subtaskId: String, taskId: String) {
        viewModelScope.launch {
            repo.updateSubtaskProgress(subtaskId, 100)
            repo.updateSubtaskStatus(subtaskId, TaskStatus.DONE)
            loadTaskDetail(taskId)
            recalculateTaskProgress(taskId)
        }
    }

    fun markSubtaskNotDone(subtaskId: String, taskId: String) {
        viewModelScope.launch {
            repo.updateSubtaskProgress(subtaskId, 0)
            repo.updateSubtaskStatus(subtaskId, TaskStatus.NOT_DONE)
            loadTaskDetail(taskId)
            recalculateTaskProgress(taskId)
        }
    }

    fun updateSubtaskProgress(subtaskId: String, taskId: String, progress: Int) {
        viewModelScope.launch {
            repo.updateSubtaskProgress(subtaskId, progress)
            val status = when {
                progress >= 100 -> TaskStatus.DONE
                progress > 0    -> TaskStatus.IN_PROGRESS
                else            -> TaskStatus.PENDING
            }
            repo.updateSubtaskStatus(subtaskId, status)
            recalculateTaskProgress(taskId)
            loadTaskDetail(taskId)
        }
    }

    fun deleteSubtask(subtaskId: String, taskId: String) {
        viewModelScope.launch {
            repo.deleteSubtask(subtaskId)
            loadTaskDetail(taskId)
            recalculateTaskProgress(taskId)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun recalculateTaskProgress(taskId: String) {
        val subtasks = repo.getSubtasksForTask(taskId)
        val progress = if (subtasks.isEmpty()) 0
        else subtasks.sumOf { it.progressPercentage } / subtasks.size

        val status = when {
            progress >= 100 -> TaskStatus.DONE
            progress > 0    -> TaskStatus.IN_PROGRESS
            else            -> TaskStatus.PENDING
        }

        repo.updateTaskProgress(taskId, progress)
        repo.updateTaskStatus(taskId, status)
        loadHomeData()
        loadTaskDetail(taskId)
    }

    private fun generateInsights(
        stats: DailyStats,
        todayTasks: List<MainTask>,
        allTasks: List<MainTask>
    ): List<AiInsight> {
        val insights = mutableListOf<AiInsight>()

        when {
            allTasks.isEmpty() -> insights.add(
                AiInsight(
                    message        = "Your task list is empty. Add your first task to get started.",
                    type           = InsightType.MOTIVATION,
                    actionQuestion = "What is one task you can add today?"
                )
            )
            todayTasks.isEmpty() && allTasks.isNotEmpty() -> insights.add(
                AiInsight(
                    message        = "You have tasks in your app, but none are scheduled for today.",
                    type           = InsightType.SUGGESTION,
                    actionQuestion = "Would you like to move one upcoming task to today?"
                )
            )
            stats.overallProgress == 100 && todayTasks.isNotEmpty() -> insights.add(
                AiInsight(
                    message        = "Excellent work. All of today's tasks are completed.",
                    type           = InsightType.ACHIEVEMENT,
                    actionQuestion = "Would you like to plan tomorrow now?"
                )
            )
            stats.overallProgress >= 70 -> insights.add(
                AiInsight(
                    message        = "You are making strong progress. A little more effort will finish the day well.",
                    type           = InsightType.MOTIVATION,
                    actionQuestion = "Which remaining task is the fastest win?"
                )
            )
            else -> insights.add(
                AiInsight(
                    message        = "Start with the smallest important task and build momentum.",
                    type           = InsightType.SUGGESTION,
                    actionQuestion = "What task can you complete in the next 10 minutes?"
                )
            )
        }

        if (todayTasks.any { it.status == TaskStatus.NOT_DONE.name }) {
            insights.add(
                AiInsight(
                    message        = "Some tasks were marked not done. Review them and reschedule if needed.",
                    type           = InsightType.WARNING,
                    actionQuestion = "Do you want to break one unfinished task into smaller subtasks?"
                )
            )
        }

        if (todayTasks.count { it.status == TaskStatus.PENDING.name } >= 3) {
            insights.add(
                AiInsight(
                    message        = "You have several pending tasks for today.",
                    type           = InsightType.SUGGESTION,
                    actionQuestion = "Focus on one priority task before switching to the next."
                )
            )
        }

        return insights.take(3)
    }
}