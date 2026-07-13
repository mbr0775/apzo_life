package com.example.apzolife.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TaskStatus { PENDING, DONE, NOT_DONE }

@Serializable
data class MainTask(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_date") val endDate: String = "",
    @SerialName("end_time") val endTime: String = "",
    val status: String = TaskStatus.PENDING.name,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SubTask(
    val id: String = "",
    @SerialName("task_id") val taskId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("parent_subtask_id") val parentSubtaskId: String? = null,
    val title: String = "",
    val description: String = "",
    val status: String = TaskStatus.PENDING.name,
    @SerialName("order_index") val orderIndex: Int = 0
)
