package com.example.moneymap.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncService: FirestoreSyncService
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return when (val result = syncService.syncAll()) {
            is SyncResult.Success -> Result.success()
            is SyncResult.Error -> {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }
}

