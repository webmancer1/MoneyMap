package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val categoryId: String = "",
    val amount: Double = 0.0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val alertThreshold: Float = 0.8f, // 80% threshold
    val isActive: Boolean = true
)

