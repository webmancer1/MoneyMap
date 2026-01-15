package com.example.moneymap.data.repository

import com.example.moneymap.data.dao.NotificationDao
import com.example.moneymap.data.model.NotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val unreadCount: Flow<Int> = notificationDao.getUnreadCount()

    suspend fun insertNotification(title: String, message: String, type: String) {
        val notification = NotificationEntity(
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            type = type
        )
        notificationDao.insertNotification(notification)
    }

    suspend fun markAsRead(id: Int) {
        notificationDao.markAsRead(id)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
    }
}
