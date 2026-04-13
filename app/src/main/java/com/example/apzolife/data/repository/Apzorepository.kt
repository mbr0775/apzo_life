package com.example.apzolife.data.repository

import android.util.Log
import com.example.apzolife.data.SupabaseClient
import com.example.apzolife.data.model.DailyStats
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ApzoRepository {

    private const val TAG = "ApzoRepository"
    private val db   get() = SupabaseClient.client.postgrest
    private val auth get() = SupabaseClient.client.auth

    fun currentUserId(): String? = auth.currentUserOrNull()?.id

    // ─────────────────────────────────────────────────────────────────────────
    // Tasks
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getTodayTasks(): List<MainTask> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        val uid   = currentUserId() ?: return emptyList()
        return try {
            db.from("tasks").select {
                filter {
                    eq("user_id", uid)
                    eq("date_scheduled", today)
                }
            }.decodeList<MainTask>().sortedWith(
                compareBy<MainTask> { it.dateScheduled }.thenBy { it.timeScheduled }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getTodayTasks failed", e)
            emptyList()
        }
    }

    suspend fun getAllTasks(): List<MainTask> {
        val uid = currentUserId() ?: return emptyList()
        return try {
            db.from("tasks").select {
                filter { eq("user_id", uid) }
            }.decodeList<MainTask>().sortedWith(
                compareByDescending<MainTask> { it.dateScheduled }
                    .thenByDescending { it.timeScheduled }
            )
        } catch (e: Exception) {
            Log.e(TAG, "getAllTasks failed", e)
            emptyList()
        }
    }

    suspend fun getTaskById(taskId: String): MainTask? {
        val uid = currentUserId() ?: return null
        return try {
            db.from("tasks").select {
                filter {
                    eq("id", taskId)
                    eq("user_id", uid)
                }
            }.decodeList<MainTask>().firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "getTaskById failed", e)
            null
        }
    }

    suspend fun insertTask(task: MainTask): Boolean {
        return try {
            db.from("tasks").insert(task)
            true
        } catch (e: Exception) {
            Log.e(TAG, "insertTask failed", e)
            false
        }
    }

    suspend fun updateTask(task: MainTask): Boolean {
        val uid = currentUserId() ?: return false
        return try {
            db.from("tasks").update({
                set("title",               task.title)
                set("category",            task.category)
                set("date_scheduled",      task.dateScheduled)
                set("time_scheduled",      task.timeScheduled)
                set("status",              task.status)
                set("progress_percentage", task.progressPercentage)
                set("notes",               task.notes)
                set("category_data",       task.categoryData)
            }) {
                filter {
                    eq("id",      task.id)
                    eq("user_id", uid)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateTask failed", e)
            false
        }
    }

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Boolean {
        return try {
            db.from("tasks").update({ set("status", status.name) }) {
                filter { eq("id", taskId) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateTaskStatus failed", e)
            false
        }
    }

    suspend fun updateTaskProgress(taskId: String, progress: Int): Boolean {
        return try {
            db.from("tasks").update({ set("progress_percentage", progress) }) {
                filter { eq("id", taskId) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateTaskProgress failed", e)
            false
        }
    }

    suspend fun deleteTask(taskId: String): Boolean {
        return try {
            db.from("tasks").delete { filter { eq("id", taskId) } }
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteTask failed", e)
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Subtasks
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getSubtasksForTask(taskId: String): List<SubTask> {
        return try {
            db.from("subtasks").select {
                filter { eq("task_id", taskId) }
            }.decodeList<SubTask>().sortedBy { it.orderIndex }
        } catch (e: Exception) {
            Log.e(TAG, "getSubtasksForTask failed", e)
            emptyList()
        }
    }

    suspend fun insertSubtask(subtask: SubTask): Boolean {
        return try {
            db.from("subtasks").insert(subtask)
            true
        } catch (e: Exception) {
            Log.e(TAG, "insertSubtask failed", e)
            false
        }
    }

    /**
     * Updates editable text fields of a subtask (title, description, time_scheduled).
     * Status and progress are left as-is; the caller controls those separately.
     */
    suspend fun updateSubtask(subtask: SubTask): Boolean {
        return try {
            db.from("subtasks").update({
                set("title",          subtask.title)
                set("description",    subtask.description)
                set("time_scheduled", subtask.timeScheduled)
            }) {
                filter { eq("id", subtask.id) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateSubtask failed", e)
            false
        }
    }

    suspend fun updateSubtaskStatus(subtaskId: String, status: TaskStatus): Boolean {
        return try {
            db.from("subtasks").update({ set("status", status.name) }) {
                filter { eq("id", subtaskId) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateSubtaskStatus failed", e)
            false
        }
    }

    suspend fun updateSubtaskProgress(subtaskId: String, progress: Int): Boolean {
        return try {
            db.from("subtasks").update({
                set("progress_percentage", progress)
            }) {
                filter { eq("id", subtaskId) }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "updateSubtaskProgress failed", e)
            false
        }
    }

    suspend fun deleteSubtask(subtaskId: String): Boolean {
        return try {
            db.from("subtasks").delete { filter { eq("id", subtaskId) } }
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteSubtask failed", e)
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getDailyStats(): DailyStats {
        val allTasks       = getAllTasks()
        val completedTasks = allTasks.count { it.status == TaskStatus.DONE.name }
        val pendingTasks   = allTasks.count {
            it.status == TaskStatus.PENDING.name || it.status == TaskStatus.IN_PROGRESS.name
        }
        val totalTasks = allTasks.size
        val overall    = if (totalTasks > 0) {
            allTasks.sumOf { it.progressPercentage } / totalTasks
        } else 0

        return DailyStats(
            totalTasks     = totalTasks,
            completedTasks = completedTasks,
            pendingTasks   = pendingTasks,
            overallProgress = overall
        )
    }
}