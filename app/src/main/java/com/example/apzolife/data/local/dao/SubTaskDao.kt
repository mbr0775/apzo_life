package com.example.apzolife.data.local.dao

import androidx.room.*
import com.example.apzolife.data.local.entity.SubTaskEntity

@Dao
interface SubTaskDao {
    @Query("SELECT * FROM subtasks WHERE task_id = :taskId ORDER BY parent_subtask_id ASC, order_index ASC")
    suspend fun getSubtasksForTask(taskId: String): List<SubTaskEntity>

    @Query("SELECT * FROM subtasks WHERE task_id = :taskId AND parent_subtask_id IS NULL ORDER BY order_index ASC")
    suspend fun getRootSubtasksForTask(taskId: String): List<SubTaskEntity>

    @Query("SELECT * FROM subtasks WHERE parent_subtask_id = :parentSubtaskId ORDER BY order_index ASC")
    suspend fun getSubtasksByParent(parentSubtaskId: String): List<SubTaskEntity>

    @Query("SELECT * FROM subtasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): SubTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subtask: SubTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subtasks: List<SubTaskEntity>)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM subtasks WHERE user_id = :userId")
    suspend fun getSubtasksForUser(userId: String): List<SubTaskEntity>

    @Query("DELETE FROM subtasks WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM subtasks WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM subtasks WHERE parent_subtask_id = :parentSubtaskId")
    suspend fun deleteByParentId(parentSubtaskId: String)

    @Query("UPDATE subtasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
