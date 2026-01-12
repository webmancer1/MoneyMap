package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: DebtType = DebtType.PAYABLE,
    val personName: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

