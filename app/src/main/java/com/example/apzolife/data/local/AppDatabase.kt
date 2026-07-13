package com.example.apzolife.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.apzolife.data.local.dao.SubTaskDao
import com.example.apzolife.data.local.dao.SyncQueueDao
import com.example.apzolife.data.local.dao.TaskDao
import com.example.apzolife.data.local.entity.SubTaskEntity
import com.example.apzolife.data.local.entity.SyncQueueEntity
import com.example.apzolife.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, SubTaskEntity::class, SyncQueueEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun subtaskDao(): SubTaskDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun create(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apzo_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

