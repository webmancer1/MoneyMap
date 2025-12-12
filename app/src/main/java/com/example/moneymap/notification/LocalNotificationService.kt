package com.example.moneymap.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.moneymap.MainActivity
import com.example.moneymap.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNotificationService @Inject constructor(
    private val context: Context
) {

    private fun showNotification(
        channelId: String,
        title: String,
        message: String,
        notificationId: Int
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, cannot show notification
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using a default icon for now, ideally use app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }

    fun showTransactionNotification(title: String, message: String) {
        showNotification(
            NotificationChannelManager.CHANNEL_TRANSACTIONS,
            title,
            message,
            System.currentTimeMillis().toInt()
        )
    }

    fun showBudgetAlert(title: String, message: String) {
        showNotification(
            NotificationChannelManager.CHANNEL_BUDGET,
            title,
            message,
            System.currentTimeMillis().toInt()
        )
    }

    fun showSecurityAlert(title: String, message: String) {
        showNotification(
            NotificationChannelManager.CHANNEL_SECURITY,
            title,
            message,
            System.currentTimeMillis().toInt()
        )
    }

    fun showGeneralNotification(title: String, message: String) {
        showNotification(
            NotificationChannelManager.CHANNEL_GENERAL,
            title,
            message,
            System.currentTimeMillis().toInt()
        )
    }
}
