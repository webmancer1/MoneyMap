package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.BudgetDao
import com.example.moneymap.data.model.Budget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.google.firebase.auth.FirebaseAuth

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao,
    private val firebaseAuth: FirebaseAuth
) {
    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""
    fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets(currentUserId)
    }

    suspend fun getBudgetById(id: String): Budget? {
        return budgetDao.getBudgetById(currentUserId, id)
    }

    fun getBudgetsByCategory(categoryId: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsByCategory(currentUserId, categoryId)
    }

    fun getActiveBudgetsForDate(date: Long): Flow<List<Budget>> {
        return budgetDao.getActiveBudgetsForDate(currentUserId, date)
    }

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget.copy(userId = currentUserId))
    }

    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun deactivateBudget(id: String) {
        budgetDao.deactivateBudget(currentUserId, id)
    }
}

