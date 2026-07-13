package com.example.apzolife.data.local.dao

import androidx.room.*
import com.example.apzolife.data.local.entity.TaskEntity

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND start_date = :date ORDER BY start_time ASC")
    suspend fun getTasksForDate(userId: String, date: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY start_date DESC, start_time DESC")
    suspend fun getAllTasks(userId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id AND user_id = :userId LIMIT 1")
    suspend fun getTaskById(id: String, userId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("UPDATE tasks SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
