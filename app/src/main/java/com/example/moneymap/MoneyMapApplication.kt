package com.example.moneymap

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import com.example.moneymap.notification.NotificationChannelManager
import com.example.moneymap.worker.DailyReminderWorker

@HiltAndroidApp
class MoneyMapApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    @Inject
    lateinit var syncManager: com.example.moneymap.data.sync.SyncManager

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager.createNotificationChannels()
        syncManager.initialize()
        scheduleDailyReminder()
    }

    private fun scheduleDailyReminder() {
        val workManager = androidx.work.WorkManager.getInstance(this)
        val request = androidx.work.PeriodicWorkRequestBuilder<com.example.moneymap.worker.DailyReminderWorker>(
            24, java.util.concurrent.TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "DailyReminder",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

