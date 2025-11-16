package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val amount: Double,
    val period: BudgetPeriod,
    val startDate: Long,
    val endDate: Long,
    val alertThreshold: Float = 0.8f, // 80% threshold
    val isActive: Boolean = true
)

