package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.TransactionDao
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    suspend fun getTransactionById(id: String): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }

    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(categoryId)
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }

    suspend fun getTotalAmountByTypeAndDateRange(
        type: TransactionType,
        startDate: Long,
        endDate: Long
    ): Double {
        return transactionDao.getTotalAmountByTypeAndDateRange(type, startDate, endDate) ?: 0.0
    }

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: String) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun getPendingSyncTransactions(): List<Transaction> {
        return transactionDao.getTransactionsBySyncStatus(SyncStatus.PENDING)
    }
}

