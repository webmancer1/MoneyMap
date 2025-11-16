package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.BudgetDao
import com.example.moneymap.data.model.Budget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    fun getAllBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets()
    }

    suspend fun getBudgetById(id: String): Budget? {
        return budgetDao.getBudgetById(id)
    }

    fun getBudgetsByCategory(categoryId: String): Flow<List<Budget>> {
        return budgetDao.getBudgetsByCategory(categoryId)
    }

    fun getActiveBudgetsForDate(date: Long): Flow<List<Budget>> {
        return budgetDao.getActiveBudgetsForDate(date)
    }

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }

    suspend fun deactivateBudget(id: String) {
        budgetDao.deactivateBudget(id)
    }
}

