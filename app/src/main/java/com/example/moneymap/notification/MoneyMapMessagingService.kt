package com.example.moneymap.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MoneyMapMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var localNotificationService: LocalNotificationService

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to server if needed
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            localNotificationService.showGeneralNotification(
                it.title ?: "MoneyMap",
                it.body ?: "New Notification"
            )
        }
        
        // Also handle data payload if necessary
    }
}
