package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.TransactionDao
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.model.SyncStatus
import com.example.moneymap.data.database.dao.IncomeExpenseSum
import com.example.moneymap.data.database.dao.CategorySpendingSum
import com.example.moneymap.data.database.dao.PaymentMethodSpendingSum
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val firebaseAuth: FirebaseAuth
) {
    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions(currentUserId)
    }

    suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getTransactionById(currentUserId, id)
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(currentUserId, type)
    }

    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(currentUserId, categoryId)
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(currentUserId, startDate, endDate)
    }

    fun getFilteredTransactions(
        query: String? = null,
        type: TransactionType? = null,
        categoryId: String? = null,
        paymentMethod: com.example.moneymap.data.model.PaymentMethod? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        minAmount: Double? = null,
        maxAmount: Double? = null
    ): Flow<List<Transaction>> {
        return transactionDao.getFilteredTransactions(
            currentUserId, query, type, categoryId, paymentMethod, startDate, endDate, minAmount, maxAmount
        )
    }

    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double {
        return transactionDao.getTotalAmountByTypeAndDateRange(currentUserId, type, startDate, endDate) ?: 0.0
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.copy(userId = currentUserId))
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(currentUserId, id)
    }

    suspend fun getPendingSyncTransactions(): List<Transaction> {
        return transactionDao.getTransactionsBySyncStatus(SyncStatus.PENDING)
    }

    fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions(currentUserId, limit)
    }

    fun getIncomeExpenseSum(startDate: Long, endDate: Long): Flow<List<IncomeExpenseSum>> {
        return transactionDao.getIncomeExpenseSum(currentUserId, startDate, endDate)
    }

    fun getCategorySpendingSum(startDate: Long, endDate: Long): Flow<List<CategorySpendingSum>> {
        return transactionDao.getCategorySpendingSum(currentUserId, startDate, endDate)
    }

    fun getPaymentMethodSpendingSum(startDate: Long, endDate: Long): Flow<List<PaymentMethodSpendingSum>> {
        return transactionDao.getPaymentMethodSpendingSum(currentUserId, startDate, endDate)
    }
}

