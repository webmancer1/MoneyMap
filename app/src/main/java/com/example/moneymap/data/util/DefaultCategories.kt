package com.example.moneymap.data.util

import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.CategoryType
import java.util.UUID

object DefaultCategories {
    fun getDefaultCategories(): List<Category> {
        return listOf(
            // Expense Categories
            Category(
                id = UUID.randomUUID().toString(),
                name = "Food & Dining",
                icon = "restaurant",
                color = "#FF6B6B",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Transportation",
                icon = "directions_car",
                color = "#4ECDC4",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Shopping",
                icon = "shopping_bag",
                color = "#95E1D3",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Bills & Utilities",
                icon = "receipt",
                color = "#F38181",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Entertainment",
                icon = "movie",
                color = "#AA96DA",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Healthcare",
                icon = "local_hospital",
                color = "#FCBAD3",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Education",
                icon = "school",
                color = "#A8E6CF",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Personal Care",
                icon = "spa",
                color = "#FFD93D",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Travel",
                icon = "flight",
                color = "#6BCB77",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Other",
                icon = "more_horiz",
                color = "#95A5A6",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            // Income Categories
            Category(
                id = UUID.randomUUID().toString(),
                name = "Salary",
                icon = "account_balance_wallet",
                color = "#2ECC71",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Freelance",
                icon = "work",
                color = "#3498DB",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Business",
                icon = "business",
                color = "#9B59B6",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Investment",
                icon = "trending_up",
                color = "#1ABC9C",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Gift",
                icon = "card_giftcard",
                color = "#E74C3C",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Other",
                icon = "more_horiz",
                color = "#95A5A6",
                type = CategoryType.INCOME,
                isDefault = true
            )
        )
    }
}

