package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amount: Double,
    val currency: String = "KES",
    val categoryId: String,
    val date: Long, // Unix timestamp
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

