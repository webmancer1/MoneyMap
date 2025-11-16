package com.example.moneymap.data.database

import androidx.room.TypeConverter
import com.example.moneymap.data.model.*

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String {
        return value.name
    }

    @TypeConverter
    fun toCategoryType(value: String): CategoryType {
        return CategoryType.valueOf(value)
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? {
        return value?.name
    }

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? {
        return value?.let { PaymentMethod.valueOf(it) }
    }

    @TypeConverter
    fun fromRecurringPattern(value: RecurringPattern?): String? {
        return value?.name
    }

    @TypeConverter
    fun toRecurringPattern(value: String?): RecurringPattern? {
        return value?.let { RecurringPattern.valueOf(it) }
    }

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String {
        return value.name
    }

    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod {
        return BudgetPeriod.valueOf(value)
    }

    @TypeConverter
    fun fromDebtType(value: DebtType): String {
        return value.name
    }

    @TypeConverter
    fun toDebtType(value: String): DebtType {
        return DebtType.valueOf(value)
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String {
        return value.name
    }

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}

