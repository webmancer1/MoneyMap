package com.example.moneymap.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.moneymap.data.database.dao.*
import com.example.moneymap.data.model.*

@Database(
    entities = [
        Transaction::class,
        Category::class,
        Budget::class,
        Goal::class,
        Debt::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MoneyMapDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao
}

