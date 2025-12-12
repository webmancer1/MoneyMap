package com.example.moneymap.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    private val context: Context
) {
    companion object {
        const val CHANNEL_TRANSACTIONS = "transactions_channel"
        const val CHANNEL_BUDGET = "budget_channel"
        const val CHANNEL_SECURITY = "security_channel"
        const val CHANNEL_GENERAL = "general_channel"
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val transactionsChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Transactions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new transactions"
            }

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when exceeding budget"
            }

            val securityChannel = NotificationChannel(
                CHANNEL_SECURITY,
                "Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Security related notifications"
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app reminders"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannels(
                listOf(
                    transactionsChannel,
                    budgetChannel,
                    securityChannel,
                    generalChannel
                )
            )
        }
    }
}
