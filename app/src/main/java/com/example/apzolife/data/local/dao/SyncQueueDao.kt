package com.example.apzolife.data.local.dao

import androidx.room.*
import com.example.apzolife.data.local.entity.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Delete
    suspend fun delete(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE entity_id = :entityId")
    suspend fun deleteAllForEntity(entityId: String)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun count(): Int
}
