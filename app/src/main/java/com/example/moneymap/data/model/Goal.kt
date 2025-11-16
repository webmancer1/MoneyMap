package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

