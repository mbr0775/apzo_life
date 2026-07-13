package com.example.apzolife.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val operation: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "entity_type") val entityType: String,
    val payload: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)