package com.example.moneymap.di

import android.content.Context
import androidx.room.Room
import com.example.moneymap.data.dao.NotificationDao
import com.example.moneymap.data.database.MoneyMapDatabase
import com.example.moneymap.data.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MoneyMapDatabase {
        return Room.databaseBuilder(
            context,
            MoneyMapDatabase::class.java,
            "money_map_database"
        ).fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideTransactionDao(database: MoneyMapDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideCategoryDao(database: MoneyMapDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideBudgetDao(database: MoneyMapDatabase): BudgetDao {
        return database.budgetDao()
    }

    @Provides
    fun provideDebtDao(database: MoneyMapDatabase): DebtDao {
        return database.debtDao()
    }

    @Provides
    fun provideGoalDao(database: MoneyMapDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideNotificationDao(database: MoneyMapDatabase): NotificationDao {
        return database.notificationDao()
    }
}

