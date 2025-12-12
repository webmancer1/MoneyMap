package com.example.moneymap.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.moneymap.notification.LocalNotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val localNotificationService: LocalNotificationService
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            localNotificationService.showGeneralNotification(
                "Daily Check-in",
                "Don't forget to log your transactions for today!"
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
