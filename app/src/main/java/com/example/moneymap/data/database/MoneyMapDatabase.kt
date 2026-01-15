package com.example.moneymap.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.moneymap.data.dao.NotificationDao
import com.example.moneymap.data.database.dao.*
import com.example.moneymap.data.model.Budget
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.Goal
import com.example.moneymap.data.model.NotificationEntity
import com.example.moneymap.data.model.Transaction

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class,
        Debt::class,
        Goal::class,
        NotificationEntity::class
    ],
    version = 4, // Increment version if needed
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MoneyMapDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun goalDao(): GoalDao
    abstract fun notificationDao(): NotificationDao
}
