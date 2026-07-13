package com.example.apzolife.data.sync

import android.util.Log
import com.example.apzolife.data.NetworkMonitor
import com.example.apzolife.data.SupabaseClient
import com.example.apzolife.data.local.dao.SyncQueueDao
import com.example.apzolife.data.model.MainTask
import com.example.apzolife.data.model.SubTask
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object SyncManager {
    private const val TAG = "SyncManager"
    private lateinit var syncQueueDao: SyncQueueDao
    private lateinit var networkMonitor: NetworkMonitor
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun init(syncQueueDao: SyncQueueDao, networkMonitor: NetworkMonitor) {
        this.syncQueueDao = syncQueueDao
        this.networkMonitor = networkMonitor
        scope.launch {
            networkMonitor.isOnlineFlow.collect { online ->
                if (online) flushQueue()
            }
        }
    }

    fun trySync() {
        if (::networkMonitor.isInitialized && networkMonitor.isOnline)
            scope.launch { flushQueue() }
    }

    private suspend fun flushQueue() {
        val items = syncQueueDao.getAll()
        if (items.isEmpty()) return
        val db = SupabaseClient.client.postgrest
        for (item in items) {
            try {
                when (item.operation) {
                    "UPSERT_TASK"    -> db.from("tasks").upsert(json.decodeFromString<MainTask>(item.payload))
                    "DELETE_TASK"    -> db.from("tasks").delete { filter { eq("id", item.entityId) } }
                    "UPSERT_SUBTASK" -> db.from("subtasks").upsert(json.decodeFromString<SubTask>(item.payload))
                    "DELETE_SUBTASK" -> db.from("subtasks").delete { filter { eq("id", item.entityId) } }
                }
                syncQueueDao.delete(item)
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed: ${e.message}")
                break
            }
        }
    }
}