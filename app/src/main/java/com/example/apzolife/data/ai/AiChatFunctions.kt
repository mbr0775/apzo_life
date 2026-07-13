package com.example.apzolife.data.ai

import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.data.repository.ApzoRepository
import kotlinx.serialization.json.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Declares the actions Apzo AI is allowed to take on the user's behalf, and
 * executes them against ApzoRepository. Shared by both the Gemini function
 * calling path (AiChatService) and the OpenAI-style "tools" fallback path
 * (AiChatToolClient), so the schema and the executor never drift apart.
 *
 * Every function returns a JsonObject so both call sites can hand the result
 * straight back to their respective provider without extra conversion.
 */
object AiChatFunctions {

    data class ParamSpec(
        val name: String,
        val type: String, // "string" | "integer"
        val description: String,
        val required: Boolean = false
    )

    data class FunctionSpec(val name: String, val description: String, val params: List<ParamSpec>)

    val specs = listOf(
        FunctionSpec(
            name = "get_today_tasks",
            description = "Get the user's tasks scheduled for today, including status (pending, done, not_done).",
            params = emptyList()
        ),
        FunctionSpec(
            name = "get_pending_tasks",
            description = "Get the user's pending (not yet completed) tasks across all dates, earliest first.",
            params = listOf(
                ParamSpec("limit", "integer", "Max number of tasks to return. Defaults to 15 if omitted.")
            )
        ),
        FunctionSpec(
            name = "get_weekly_stats",
            description = "Get completed vs missed vs pending task counts for the last 7 days. Use this to answer " +
                    "'why am I missing tasks' or 'what should I focus on'.",
            params = emptyList()
        ),
        FunctionSpec(
            name = "create_task",
            description = "Create a new main task for the user.",
            params = listOf(
                ParamSpec("title", "string", "Short task title.", required = true),
                ParamSpec("description", "string", "Optional longer description."),
                ParamSpec("start_date", "string", "Start date, YYYY-MM-DD. Defaults to today if omitted."),
                ParamSpec("start_time", "string", "Start time, 24-hour HH:mm, optional."),
                ParamSpec("end_date", "string", "End date, YYYY-MM-DD, optional."),
                ParamSpec("end_time", "string", "End time, 24-hour HH:mm, optional.")
            )
        ),
        FunctionSpec(
            name = "create_subtask",
            description = "Break an existing task into a smaller subtask/step. task_title must match one of the " +
                    "user's existing task titles (call get_today_tasks / get_pending_tasks first if you aren't sure of it).",
            params = listOf(
                ParamSpec("task_title", "string", "Title (or close match) of the existing main task.", required = true),
                ParamSpec("subtask_title", "string", "Short title for the new subtask/step.", required = true),
                ParamSpec("description", "string", "Optional short description of the subtask.")
            )
        )
    )

    suspend fun execute(name: String, args: JsonObject): JsonObject = try {
        when (name) {
            "get_today_tasks" -> getTodayTasks()
            "get_pending_tasks" -> getPendingTasks(args)
            "get_weekly_stats" -> getWeeklyStats()
            "create_task" -> createTask(args)
            "create_subtask" -> createSubtask(args)
            else -> buildJsonObject { put("error", "Unknown function: $name") }
        }
    } catch (e: Exception) {
        buildJsonObject { put("error", "Function '$name' failed: ${e.message}") }
    }

    private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.intOrNull(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private suspend fun getTodayTasks(): JsonObject {
        val tasks = ApzoRepository.getTodayTasks()
        return buildJsonObject {
            put("count", tasks.size)
            putJsonArray("tasks") { tasks.forEach { add(taskJson(it)) } }
        }
    }

    private suspend fun getPendingTasks(args: JsonObject): JsonObject {
        val limit = (args.intOrNull("limit") ?: 15).coerceIn(1, 50)
        val pending = ApzoRepository.getAllTasks()
            .filter { it.status == TaskStatus.PENDING.name }
            .sortedBy { "${it.startDate} ${it.startTime}" }
            .take(limit)
        return buildJsonObject {
            put("count", pending.size)
            putJsonArray("tasks") { pending.forEach { add(taskJson(it)) } }
        }
    }

    private suspend fun getWeeklyStats(): JsonObject {
        val all = ApzoRepository.getAllTasks()
        val today = LocalDate.now()
        val weekAgo = today.minusDays(6)
        val recent = all.filter {
            try {
                val d = LocalDate.parse(it.startDate)
                !d.isBefore(weekAgo) && !d.isAfter(today)
            } catch (_: Exception) { false }
        }
        return buildJsonObject {
            put("total_last_7_days", recent.size)
            put("completed", recent.count { it.status == TaskStatus.DONE.name })
            put("missed", recent.count { it.status == TaskStatus.NOT_DONE.name })
            put("pending", recent.count { it.status == TaskStatus.PENDING.name })
            putJsonArray("missed_task_titles") {
                recent.filter { it.status == TaskStatus.NOT_DONE.name }.take(10).forEach { add(it.title) }
            }
        }
    }

    private suspend fun createTask(args: JsonObject): JsonObject {
        val title = args.str("title")?.trim().orEmpty()
        if (title.isBlank()) return buildJsonObject { put("error", "title is required") }
        val uid = ApzoRepository.currentUserId()
            ?: return buildJsonObject { put("error", "User is not logged in") }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val startDate = args.str("start_date")?.takeIf { it.isNotBlank() } ?: today
        val task = MainTask(
            id = UUID.randomUUID().toString(),
            userId = uid,
            title = title,
            description = args.str("description").orEmpty(),
            startDate = startDate,
            startTime = args.str("start_time").orEmpty(),
            endDate = args.str("end_date")?.takeIf { it.isNotBlank() } ?: startDate,
            endTime = args.str("end_time").orEmpty(),
            status = TaskStatus.PENDING.name
        )
        val ok = ApzoRepository.insertTask(task)
        return buildJsonObject {
            put("success", ok)
            if (ok) { put("task_id", task.id); put("title", task.title); put("start_date", task.startDate) }
        }
    }

    private suspend fun createSubtask(args: JsonObject): JsonObject {
        val taskTitle = args.str("task_title")?.trim().orEmpty()
        val subtaskTitle = args.str("subtask_title")?.trim().orEmpty()
        if (taskTitle.isBlank() || subtaskTitle.isBlank()) {
            return buildJsonObject { put("error", "task_title and subtask_title are required") }
        }
        val uid = ApzoRepository.currentUserId()
            ?: return buildJsonObject { put("error", "User is not logged in") }

        val match = ApzoRepository.getAllTasks().firstOrNull {
            it.title.equals(taskTitle, ignoreCase = true) ||
                    it.title.contains(taskTitle, ignoreCase = true) ||
                    taskTitle.contains(it.title, ignoreCase = true)
        } ?: return buildJsonObject { put("error", "No existing task found matching '$taskTitle'") }

        val siblingCount = ApzoRepository.getSubtasksForTask(match.id).count { it.parentSubtaskId == null }
        val subtask = SubTask(
            id = UUID.randomUUID().toString(),
            taskId = match.id,
            userId = uid,
            parentSubtaskId = null,
            title = subtaskTitle,
            description = args.str("description").orEmpty(),
            status = TaskStatus.PENDING.name,
            orderIndex = siblingCount
        )
        val ok = ApzoRepository.insertSubtask(subtask)
        return buildJsonObject {
            put("success", ok)
            put("task_title", match.title)
            if (ok) put("subtask_id", subtask.id)
        }
    }

    private fun taskJson(t: MainTask): JsonObject = buildJsonObject {
        put("id", t.id)
        put("title", t.title)
        put("status", t.status)
        put("start_date", t.startDate)
        put("start_time", t.startTime)
    }
}