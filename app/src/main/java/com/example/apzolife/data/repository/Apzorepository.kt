package com.example.apzolife.data.repository

import android.util.Log
import com.example.apzolife.data.NetworkMonitor
import com.example.apzolife.data.SupabaseClient
import com.example.apzolife.data.local.AppDatabase
import com.example.apzolife.data.local.entity.SyncQueueEntity
import com.example.apzolife.data.local.entity.toEntity
import com.example.apzolife.data.local.entity.toMainTask
import com.example.apzolife.data.local.entity.toSubTask
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import com.example.apzolife.data.model.TaskStatus
import com.example.apzolife.data.sync.SyncManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ApzoRepository {
    private const val TAG = "ApzoRepository"
    private lateinit var taskDao: com.example.apzolife.data.local.dao.TaskDao
    private lateinit var subtaskDao: com.example.apzolife.data.local.dao.SubTaskDao
    private lateinit var syncQueueDao: com.example.apzolife.data.local.dao.SyncQueueDao
    private lateinit var networkMonitor: NetworkMonitor

    private val supabase get() = SupabaseClient.client.postgrest
    private val auth     get() = SupabaseClient.client.auth
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun init(db: AppDatabase, networkMonitor: NetworkMonitor) {
        taskDao      = db.taskDao()
        subtaskDao   = db.subtaskDao()
        syncQueueDao = db.syncQueueDao()
        this.networkMonitor = networkMonitor
    }

    fun currentUserId(): String? = auth.currentUserOrNull()?.id

    suspend fun refreshFromRemote() {
        if (!networkMonitor.isOnline) return
        val uid = currentUserId() ?: return
        SyncManager.trySync()
        try {
            val remoteTasks = supabase.from("tasks").select {
                filter { eq("user_id", uid) }
            }.decodeList<MainTask>()
            taskDao.insertAll(remoteTasks.map { it.toEntity() })

            val remoteSubs = supabase.from("subtasks").select {
                filter { eq("user_id", uid) }
            }.decodeList<SubTask>()
            subtaskDao.insertAll(remoteSubs.map { it.toEntity() })
        } catch (e: Exception) {
            Log.e(TAG, "refreshFromRemote failed", e)
        }
    }

    suspend fun getTodayTasks(): List<MainTask> {
        val uid   = currentUserId() ?: return emptyList()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        return taskDao.getTasksForDate(uid, today).map { it.toMainTask() }
    }

    suspend fun getAllTasks(): List<MainTask> {
        val uid = currentUserId() ?: return emptyList()
        return taskDao.getAllTasks(uid).map { it.toMainTask() }
    }

    suspend fun getTaskById(taskId: String): MainTask? {
        val uid = currentUserId() ?: return null
        return taskDao.getTaskById(taskId, uid)?.toMainTask()
    }

    suspend fun insertTask(task: MainTask): Boolean = runCatching {
        taskDao.insert(task.toEntity())
        enqueue("UPSERT_TASK", task.id, "TASK", json.encodeToString(task))
    }.onFailure { Log.e(TAG, "insertTask failed", it) }.isSuccess

    suspend fun updateTask(task: MainTask): Boolean = runCatching {
        taskDao.insert(task.toEntity())
        enqueue("UPSERT_TASK", task.id, "TASK", json.encodeToString(task))
    }.onFailure { Log.e(TAG, "updateTask failed", it) }.isSuccess

    suspend fun updateTaskStatus(taskId: String, status: TaskStatus): Boolean = runCatching {
        taskDao.updateStatus(taskId, status.name)
        val uid  = currentUserId() ?: return false
        val task = taskDao.getTaskById(taskId, uid)?.toMainTask() ?: return false
        enqueue("UPSERT_TASK", taskId, "TASK", json.encodeToString(task))
    }.onFailure { Log.e(TAG, "updateTaskStatus failed", it) }.isSuccess

    suspend fun deleteTask(taskId: String): Boolean = runCatching {
        taskDao.deleteById(taskId)
        subtaskDao.deleteByTaskId(taskId)
        syncQueueDao.deleteAllForEntity(taskId)
        enqueue("DELETE_TASK", taskId, "TASK", "")
    }.onFailure { Log.e(TAG, "deleteTask failed", it) }.isSuccess

    suspend fun getSubtasksForTask(taskId: String): List<SubTask> = runCatching {
        subtaskDao.getSubtasksForTask(taskId).map { it.toSubTask() }
    }.getOrElse { emptyList() }

    suspend fun resetAllUserData(): Boolean = runCatching {
        val uid = currentUserId() ?: return false
        val existingTasks = taskDao.getAllTasks(uid)
        val existingSubtasks = subtaskDao.getSubtasksForUser(uid)

        // Remove old queued UPSERT/DELETE operations so deleted data cannot come back later.
        syncQueueDao.deleteAll()

        if (networkMonitor.isOnline) {
            // Delete remote child records first, then main tasks.
            supabase.from("subtasks").delete { filter { eq("user_id", uid) } }
            supabase.from("tasks").delete { filter { eq("user_id", uid) } }
        } else {
            // Queue deletes for the next sync when the user is offline.
            // Use normal for-loops here because DAO insert() is suspend.
            // Calling suspend functions inside Iterable.forEach {} causes a Kotlin compile error.
            for (subtask in existingSubtasks) {
                syncQueueDao.insert(
                    SyncQueueEntity(
                        operation = "DELETE_SUBTASK",
                        entityId = subtask.id,
                        entityType = "SUBTASK",
                        payload = ""
                    )
                )
            }
            for (task in existingTasks) {
                syncQueueDao.insert(
                    SyncQueueEntity(
                        operation = "DELETE_TASK",
                        entityId = task.id,
                        entityType = "TASK",
                        payload = ""
                    )
                )
            }
        }

        subtaskDao.deleteAllForUser(uid)
        taskDao.deleteAllForUser(uid)
        SyncManager.trySync()
    }.onFailure { Log.e(TAG, "resetAllUserData failed", it) }.isSuccess

    suspend fun insertSubtask(subtask: SubTask): Boolean = runCatching {
        subtaskDao.insert(subtask.toEntity())
        enqueue("UPSERT_SUBTASK", subtask.id, "SUBTASK", json.encodeToString(subtask))
    }.onFailure { Log.e(TAG, "insertSubtask failed", it) }.isSuccess

    suspend fun updateSubtask(subtask: SubTask): Boolean = runCatching {
        subtaskDao.insert(subtask.toEntity())
        enqueue("UPSERT_SUBTASK", subtask.id, "SUBTASK", json.encodeToString(subtask))
    }.onFailure { Log.e(TAG, "updateSubtask failed", it) }.isSuccess

    suspend fun updateSubtaskStatus(subtaskId: String, status: TaskStatus): Boolean = runCatching {
        subtaskDao.updateStatus(subtaskId, status.name)
        val sub = subtaskDao.getById(subtaskId)?.toSubTask() ?: return false
        enqueue("UPSERT_SUBTASK", subtaskId, "SUBTASK", json.encodeToString(sub))
    }.onFailure { Log.e(TAG, "updateSubtaskStatus failed", it) }.isSuccess

    suspend fun deleteSubtask(subtaskId: String): Boolean = runCatching {
        deleteSubtaskAndChildren(subtaskId)
    }.onFailure { Log.e(TAG, "deleteSubtask failed", it) }.isSuccess

    private suspend fun deleteSubtaskAndChildren(subtaskId: String) {
        val children = subtaskDao.getSubtasksByParent(subtaskId)
        children.forEach { deleteSubtaskAndChildren(it.id) }
        subtaskDao.deleteById(subtaskId)
        syncQueueDao.deleteAllForEntity(subtaskId)
        enqueue("DELETE_SUBTASK", subtaskId, "SUBTASK", "")
    }

    private suspend fun enqueue(operation: String, entityId: String, entityType: String, payload: String) {
        try {
            syncQueueDao.insert(SyncQueueEntity(operation = operation, entityId = entityId, entityType = entityType, payload = payload))
            SyncManager.trySync()
        } catch (e: Exception) {
            Log.e(TAG, "Enqueue failed", e)
        }
    }
}
