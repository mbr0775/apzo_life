package com.example.apzolife.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.apzolife.data.model.MainTask

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "start_time") val startTime: String,
    @ColumnInfo(name = "end_date") val endDate: String,
    @ColumnInfo(name = "end_time") val endTime: String,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: String?
)

fun TaskEntity.toMainTask() = MainTask(
    id = id, userId = userId, title = title, description = description,
    startDate = startDate, startTime = startTime, endDate = endDate,
    endTime = endTime, status = status, createdAt = createdAt
)

fun MainTask.toEntity() = TaskEntity(
    id = id, userId = userId, title = title, description = description,
    startDate = startDate, startTime = startTime, endDate = endDate,
    endTime = endTime, status = status, createdAt = createdAt
)