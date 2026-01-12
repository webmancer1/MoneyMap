package com.example.moneymap.data.database.dao

import androidx.room.*
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun getTransactionById(userId: String, id: String): Transaction?

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY date DESC")
    fun getTransactionsByType(userId: String, type: TransactionType): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND categoryId = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(userId: String, categoryId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE syncStatus = :status")
    suspend fun getTransactionsBySyncStatus(status: SyncStatus): List<Transaction>

    @Query("""
        SELECT * FROM transactions 
        WHERE userId = :userId
        AND (:query IS NULL OR notes LIKE '%' || :query || '%' OR CAST(amount AS TEXT) LIKE '%' || :query || '%')
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:paymentMethod IS NULL OR paymentMethod = :paymentMethod)
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY date DESC
    """)
    fun getFilteredTransactions(
        userId: String,
        query: String?,
        type: TransactionType?,
        categoryId: String?,
        paymentMethod: com.example.moneymap.data.model.PaymentMethod?,
        startDate: Long?,
        endDate: Long?,
        minAmount: Double?,
        maxAmount: Double?
    ): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type AND date >= :startDate AND date <= :endDate")
    suspend fun getTotalAmountByTypeAndDateRange(
        userId: String,
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id AND userId = :userId")
    suspend fun deleteTransactionById(userId: String, id: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun deleteAllTransactions(userId: String)
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(userId: String, limit: Int): Flow<List<Transaction>>

    @Query("SELECT type, currency, SUM(amount) as total FROM transactions WHERE userId = :userId AND date >= :startDate AND date <= :endDate GROUP BY type, currency")
    fun getIncomeExpenseSum(userId: String, startDate: Long, endDate: Long): Flow<List<IncomeExpenseSum>>

    // Helper class for the aggregation result. 
    // Since this is an interface, we can't define the data class inside easily without it being a valid return type.
    // However, Room can return POJOs. 
    // I will add the POJO classes in a separate file or at the bottom of this file if appropriate, 
    // but for now I'll use a Map or a specific DTO.
    // Let's stick to returning specific DTOs. I need to define them.
    @Query("SELECT categoryId, currency, SUM(amount) as totalAmount, COUNT(*) as transactionCount FROM transactions WHERE userId = :userId AND type = 'EXPENSE' AND date >= :startDate AND date <= :endDate GROUP BY categoryId, currency ORDER BY totalAmount DESC")
    fun getCategorySpendingSum(userId: String, startDate: Long, endDate: Long): Flow<List<CategorySpendingSum>>

    @Query("SELECT paymentMethod, currency, SUM(amount) as totalAmount, COUNT(*) as transactionCount FROM transactions WHERE userId = :userId AND type = 'EXPENSE' AND paymentMethod IS NOT NULL AND date >= :startDate AND date <= :endDate GROUP BY paymentMethod, currency ORDER BY totalAmount DESC")
    fun getPaymentMethodSpendingSum(userId: String, startDate: Long, endDate: Long): Flow<List<PaymentMethodSpendingSum>>
}

data class IncomeExpenseSum(
    val type: TransactionType,
    val currency: String,
    val total: Double
)

data class CategorySpendingSum(
    val categoryId: String,
    val currency: String,
    val totalAmount: Double,
    val transactionCount: Int
)

data class PaymentMethodSpendingSum(
    val paymentMethod: com.example.moneymap.data.model.PaymentMethod,
    val currency: String,
    val totalAmount: Double,
    val transactionCount: Int
)

