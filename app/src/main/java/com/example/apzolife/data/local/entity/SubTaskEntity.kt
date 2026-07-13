package com.example.apzolife.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.apzolife.data.model.SubTask

@Entity(tableName = "subtasks")
data class SubTaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "task_id") val taskId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "parent_subtask_id") val parentSubtaskId: String? = null,
    val title: String,
    val description: String,
    val status: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int
)

fun SubTaskEntity.toSubTask() = SubTask(
    id = id,
    taskId = taskId,
    userId = userId,
    parentSubtaskId = parentSubtaskId,
    title = title,
    description = description,
    status = status,
    orderIndex = orderIndex
)

fun SubTask.toEntity() = SubTaskEntity(
    id = id,
    taskId = taskId,
    userId = userId,
    parentSubtaskId = parentSubtaskId,
    title = title,
    description = description,
    status = status,
    orderIndex = orderIndex
)
