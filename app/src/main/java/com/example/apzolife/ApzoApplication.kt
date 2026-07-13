package com.example.apzolife

import android.app.Application
import com.example.apzolife.data.NetworkMonitor
import com.example.apzolife.data.SessionManager
import com.example.apzolife.data.local.AppDatabase
import com.example.apzolife.data.repository.ApzoRepository
import com.example.apzolife.data.sync.SyncManager

class ApzoApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Session (must be first)
        SessionManager.init(applicationContext)

        // 2. Local Room database
        database = AppDatabase.create(this)

        // 3. Network monitor (starts listening immediately)
        networkMonitor = NetworkMonitor(this)
        networkMonitor.register()

        // 4. Repository + SyncManager wired together
        ApzoRepository.init(database, networkMonitor)
        SyncManager.init(database.syncQueueDao(), networkMonitor)
    }
}