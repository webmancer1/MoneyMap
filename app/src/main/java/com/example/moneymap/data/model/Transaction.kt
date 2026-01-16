package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

import androidx.annotation.Keep

@Keep
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: Double = 0.0,
    val currency: String = "KES",
    val categoryId: String = "",
    val date: Long = 0L, // Unix timestamp
    val notes: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val tags: List<String> = emptyList(),
    val receiptUrl: String? = null,
    val isRecurring: Boolean = false,
    val recurringPattern: RecurringPattern? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

