package com.example.apzolife.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apzolife.data.ai.AiChatService
import com.example.apzolife.data.ai.AiChatTurn
import com.example.apzolife.data.ai.AiDailyPlanResult
import com.example.apzolife.data.ai.AiDailyPlannerService
import com.example.apzolife.data.ai.AiInsightsCoachService
import com.example.apzolife.data.ai.AiInsightsResult
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.data.repository.ApzoRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

data class HomeUiState(
    val todayTasks: List<MainTask> = emptyList(),
    val allTasks: List<MainTask> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TaskDetailUiState(
    val task: MainTask? = null,
    val subtasks: List<SubTask> = emptyList(),
    val isLoading: Boolean = false
)

/** Input used by AddTaskScreen to create a subtask tree before the task exists. */
data class CreateSubtaskInput(
    val title: String,
    val description: String = "",
    val children: List<CreateSubtaskInput> = emptyList()
)

/** UI state for the "Plan My Day" AI feature on HomeScreen. */
data class DailyPlanUiState(
    val isLoading: Boolean = false,
    val plan: AiDailyPlanResult? = null,
    val error: String? = null
)

/** UI state for the "AI Coach" insights feature on the Analytics screen. */
data class AiInsightsUiState(
    val isLoading: Boolean = false,
    val insights: AiInsightsResult? = null,
    val error: String? = null
)

/** UI state for the "Ask Apzo AI" chat assistant. */
data class AiChatUiState(
    val messages: List<AiChatTurn> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
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

    private val _dailyPlanState = MutableStateFlow(DailyPlanUiState())
    val dailyPlanState: StateFlow<DailyPlanUiState> = _dailyPlanState.asStateFlow()

    private val _insightsState = MutableStateFlow(AiInsightsUiState())
    val insightsState: StateFlow<AiInsightsUiState> = _insightsState.asStateFlow()

    private val _chatState = MutableStateFlow(AiChatUiState())
    val chatState: StateFlow<AiChatUiState> = _chatState.asStateFlow()

    // The active AI request must be cancelled when the user resets the chat.
    private var chatJob: Job? = null

    // Guards the UI against late responses from an older/cancelled request.
    private var chatRequestVersion: Long = 0L

    init { loadHomeData() }

    fun loadHomeData() {
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true) }
            val today = repo.getTodayTasks()
            val all   = repo.getAllTasks()
            _homeState.update { it.copy(todayTasks = today, allTasks = all, isLoading = false) }
            launch {
                repo.refreshFromRemote()
                val freshToday = repo.getTodayTasks()
                val freshAll   = repo.getAllTasks()
                _homeState.update { it.copy(todayTasks = freshToday, allTasks = freshAll) }
            }
        }
    }

    fun createTask(
        title: String, description: String,
        startDate: String, startTime: String,
        endDate: String, endTime: String
    ) {
        createTaskWithSubtasks(
            title = title,
            description = description,
            startDate = startDate,
            startTime = startTime,
            endDate = endDate,
            endTime = endTime,
            subtasks = emptyList()
        )
    }

    fun createTaskWithSubtasks(
        title: String, description: String,
        startDate: String, startTime: String,
        endDate: String, endTime: String,
        subtasks: List<CreateSubtaskInput>
    ) {
        viewModelScope.launch {
            val uid = repo.currentUserId() ?: run {
                _snackbarMessage.emit("Not logged in."); return@launch
            }

            val taskId = UUID.randomUUID().toString()
            val task = MainTask(
                id = taskId,
                userId = uid, title = title, description = description,
                startDate = startDate, startTime = startTime,
                endDate = endDate, endTime = endTime,
                status = TaskStatus.PENDING.name
            )

            if (repo.insertTask(task)) {
                val failedSubtasks = insertSubtaskTree(
                    taskId = taskId,
                    userId = uid,
                    parentSubtaskId = null,
                    inputs = subtasks
                )

                loadHomeData()
                if (failedSubtasks == 0) {
                    _snackbarMessage.emit("Task created!")
                } else {
                    _snackbarMessage.emit("Task created, but $failedSubtasks sub task(s) failed.")
                }
                _taskCreated.emit(Unit)
            } else {
                _snackbarMessage.emit("Failed to create task.")
            }
        }
    }

    private suspend fun insertSubtaskTree(
        taskId: String,
        userId: String,
        parentSubtaskId: String?,
        inputs: List<CreateSubtaskInput>
    ): Int {
        var failedCount = 0
        inputs
            .mapNotNull { input ->
                val cleanTitle = input.title.trim()
                if (cleanTitle.isBlank()) null
                else input.copy(title = cleanTitle, description = input.description.trim())
            }
            .forEachIndexed { index, input ->
                val subtaskId = UUID.randomUUID().toString()
                val subtask = SubTask(
                    id = subtaskId,
                    taskId = taskId,
                    userId = userId,
                    parentSubtaskId = parentSubtaskId,
                    title = input.title,
                    description = input.description,
                    status = TaskStatus.PENDING.name,
                    orderIndex = index
                )
                if (repo.insertSubtask(subtask)) {
                    failedCount += insertSubtaskTree(
                        taskId = taskId,
                        userId = userId,
                        parentSubtaskId = subtaskId,
                        inputs = input.children
                    )
                } else {
                    failedCount++
                }
            }
        return failedCount
    }

    fun updateTask(
        taskId: String, title: String, description: String,
        startDate: String, startTime: String,
        endDate: String, endTime: String, status: String
    ) {
        viewModelScope.launch {
            val current = repo.getTaskById(taskId) ?: run {
                _snackbarMessage.emit("Task not found."); return@launch
            }
            val updated = current.copy(
                title = title, description = description,
                startDate = startDate, startTime = startTime,
                endDate = endDate, endTime = endTime, status = status
            )
            if (repo.updateTask(updated)) {
                loadHomeData(); loadTaskDetail(taskId)
                _snackbarMessage.emit("Task updated!")
            } else {
                _snackbarMessage.emit("Failed to update.")
            }
        }
    }

    fun markTaskDone(taskId: String) {
        viewModelScope.launch {
            repo.updateTaskStatus(taskId, TaskStatus.DONE)
            loadHomeData(); loadTaskDetail(taskId)
        }
    }

    fun markTaskNotDone(taskId: String) {
        viewModelScope.launch {
            repo.updateTaskStatus(taskId, TaskStatus.NOT_DONE)
            loadHomeData(); loadTaskDetail(taskId)
        }
    }

    fun markTaskPending(taskId: String) {
        viewModelScope.launch {
            repo.updateTaskStatus(taskId, TaskStatus.PENDING)
            loadHomeData(); loadTaskDetail(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repo.deleteTask(taskId)
            loadHomeData()
            _snackbarMessage.emit("Task deleted.")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            val resetSuccess = repo.resetAllUserData()
            _taskDetailState.value = TaskDetailUiState()
            loadHomeData()
            if (resetSuccess) {
                _snackbarMessage.emit("All task data has been reset.")
            } else {
                _snackbarMessage.emit("Failed to reset task data.")
            }
        }
    }

    fun loadTaskDetail(taskId: String) {
        viewModelScope.launch {
            _taskDetailState.update { it.copy(isLoading = true) }
            val task     = repo.getTaskById(taskId)
            val subtasks = repo.getSubtasksForTask(taskId)
            _taskDetailState.update { it.copy(task = task, subtasks = subtasks, isLoading = false) }
        }
    }

    fun addSubtask(taskId: String, title: String, description: String) {
        addSubtask(taskId = taskId, parentSubtaskId = null, title = title, description = description)
    }

    fun addSubtask(taskId: String, parentSubtaskId: String?, title: String, description: String) {
        viewModelScope.launch {
            val uid = repo.currentUserId() ?: run {
                _snackbarMessage.emit("Not logged in."); return@launch
            }
            val siblingCount = _taskDetailState.value.subtasks.count { it.parentSubtaskId == parentSubtaskId }
            val subtask = SubTask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                userId = uid,
                parentSubtaskId = parentSubtaskId,
                title = title,
                description = description,
                status = TaskStatus.PENDING.name,
                orderIndex = siblingCount
            )
            if (repo.insertSubtask(subtask)) loadTaskDetail(taskId)
            else _snackbarMessage.emit("Failed to add subtask.")
        }
    }

    fun updateSubtask(subtaskId: String, taskId: String, newTitle: String, newDescription: String) {
        viewModelScope.launch {
            val current = _taskDetailState.value.subtasks
                .find { it.id == subtaskId } ?: return@launch
            repo.updateSubtask(current.copy(title = newTitle, description = newDescription))
            loadTaskDetail(taskId)
        }
    }

    fun markSubtaskDone(subtaskId: String, taskId: String) {
        viewModelScope.launch {
            repo.updateSubtaskStatus(subtaskId, TaskStatus.DONE)
            loadTaskDetail(taskId)
        }
    }

    fun markSubtaskNotDone(subtaskId: String, taskId: String) {
        viewModelScope.launch {
            repo.updateSubtaskStatus(subtaskId, TaskStatus.NOT_DONE)
            loadTaskDetail(taskId)
        }
    }

    fun markSubtaskPending(subtaskId: String, taskId: String) {
        viewModelScope.launch {
            repo.updateSubtaskStatus(subtaskId, TaskStatus.PENDING)
            loadTaskDetail(taskId)
        }
    }

    fun deleteSubtask(subtaskId: String, taskId: String) {
        viewModelScope.launch {
            repo.deleteSubtask(subtaskId)
            loadTaskDetail(taskId)
        }
    }

    // â”€â”€ AI Daily Planner â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Asks Gemini to order today's PENDING tasks into a schedule.
     * Only today's pending tasks are sent â€” not full history â€” for
     * speed, cost, and privacy.
     */
    fun planMyDay() {
        viewModelScope.launch {
            _dailyPlanState.update { it.copy(isLoading = true, error = null) }

            val pendingToday = _homeState.value.todayTasks
                .filter { it.status == TaskStatus.PENDING.name }

            if (pendingToday.isEmpty()) {
                _dailyPlanState.update {
                    it.copy(isLoading = false, error = "No pending tasks today to plan.")
                }
                return@launch
            }

            val result = AiDailyPlannerService.planDay(pendingToday)
            result
                .onSuccess { plan ->
                    _dailyPlanState.update { it.copy(isLoading = false, plan = plan) }
                }
                .onFailure { e ->
                    _dailyPlanState.update {
                        it.copy(
                            isLoading = false,
                            error = AiDailyPlannerService.friendlyErrorMessage(e)
                        )
                    }
                }
        }
    }

    /** Clears the current AI plan result/error so the sheet can be dismissed cleanly. */
    fun clearDailyPlan() {
        _dailyPlanState.value = DailyPlanUiState()
    }

    // â”€â”€ AI Insights Coach â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Asks the AI to turn the last 7 days of task activity into a short
     * coaching summary: what happened, why tasks were missed, a tip, and
     * a concrete next action. Uses allTasks already loaded in homeState
     * rather than a fresh repo call, since Analytics already has that data.
     */
    fun generateInsights() {
        viewModelScope.launch {
            _insightsState.update { it.copy(isLoading = true, error = null) }

            val allTasks = _homeState.value.allTasks
            val result = AiInsightsCoachService.generateInsights(allTasks)

            result
                .onSuccess { insights ->
                    _insightsState.update { it.copy(isLoading = false, insights = insights) }
                }
                .onFailure { e ->
                    _insightsState.update {
                        it.copy(
                            isLoading = false,
                            error = AiInsightsCoachService.friendlyErrorMessage(e)
                        )
                    }
                }
        }
    }

    /** Clears the current AI Coach result/error, e.g. before regenerating. */
    fun clearInsights() {
        _insightsState.value = AiInsightsUiState()
    }

    // â”€â”€ Ask Apzo AI (chat assistant with function calling) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Sends a user message to Apzo AI. History is passed as it was BEFORE
     * this message (priorHistory) so the service can build the correct
     * chat context, while the UI immediately shows the user's new message
     * with an optimistic update.
     */
    fun sendChatMessage(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || _chatState.value.isLoading) return

        // Cancel any request left from a previous attempt.
        chatJob?.cancel()

        val requestVersion = ++chatRequestVersion
        val priorHistory = _chatState.value.messages
        val withUser = priorHistory + AiChatTurn("user", clean)

        _chatState.value = AiChatUiState(
            messages = withUser,
            isLoading = true,
            error = null
        )

        chatJob = viewModelScope.launch {
            try {
                // Never allow a stalled provider/network request to spin forever.
                val result = withTimeout(45_000L) {
                    AiChatService.sendMessage(clean, priorHistory)
                }

                // Reset may have been pressed while the provider was finishing.
                if (requestVersion != chatRequestVersion) return@launch

                val failure = result.exceptionOrNull()
                if (failure != null) {
                    _chatState.update {
                        it.copy(
                            isLoading = false,
                            error = AiChatService.friendlyErrorMessage(failure)
                        )
                    }
                    return@launch
                }

                val reply = result.getOrThrow()
                _chatState.value = AiChatUiState(
                    messages = withUser + AiChatTurn("assistant", reply),
                    isLoading = false,
                    error = null
                )

                // Tool calls may have created tasks or subtasks.
                loadHomeData()
            } catch (e: TimeoutCancellationException) {
                if (requestVersion == chatRequestVersion) {
                    _chatState.update {
                        it.copy(
                            isLoading = false,
                            error = "Apzo AI took too long. Please try again."
                        )
                    }
                }
            } catch (e: CancellationException) {
                // Expected when reset/cancel is pressed. Do not show an error.
                throw e
            } catch (e: Throwable) {
                if (requestVersion == chatRequestVersion) {
                    _chatState.update {
                        it.copy(
                            isLoading = false,
                            error = AiChatService.friendlyErrorMessage(e)
                        )
                    }
                }
            }
        }
    }

    /** Cancels the running request and resets the whole conversation. */
    fun clearChat() {
        chatRequestVersion++
        chatJob?.cancel()
        chatJob = null
        _chatState.value = AiChatUiState()
    }

    override fun onCleared() {
        chatRequestVersion++
        chatJob?.cancel()
        chatJob = null
        super.onCleared()
    }
}
