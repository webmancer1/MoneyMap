package com.example.moneymap.data.sync

import androidx.work.*
import com.example.moneymap.data.sync.SyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val workManager: WorkManager,
    private val syncService: FirestoreSyncService
) {
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("periodic_sync")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    suspend fun triggerManualSync(): SyncResult {
        return syncService.syncAll()
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork("periodic_sync")
    }
}

