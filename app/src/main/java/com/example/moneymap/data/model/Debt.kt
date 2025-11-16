package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: DebtType,
    val personName: String,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

