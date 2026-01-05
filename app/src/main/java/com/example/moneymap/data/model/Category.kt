package com.example.moneymap.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val icon: String = "", // Material Icon name
    val color: String = "", // Hex color string
    val type: CategoryType = CategoryType.EXPENSE,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)

