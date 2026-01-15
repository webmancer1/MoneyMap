package com.example.moneymap.di

import android.content.Context
import com.example.moneymap.notification.LocalNotificationService
import com.example.moneymap.notification.NotificationChannelManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationChannelManager(
        @ApplicationContext context: Context
    ): NotificationChannelManager {
        return NotificationChannelManager(context)
    }

    @Provides
    @Singleton
    fun provideLocalNotificationService(
        @ApplicationContext context: Context,
        notificationRepository: com.example.moneymap.data.repository.NotificationRepository
    ): LocalNotificationService {
        return LocalNotificationService(context, notificationRepository)
    }
}
