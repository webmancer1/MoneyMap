package com.example.moneymap.data.database.dao

import androidx.room.*
import com.example.moneymap.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId AND isActive = 1 ORDER BY startDate DESC")
    fun getAllBudgets(userId: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id AND userId = :userId")
    suspend fun getBudgetById(userId: String, id: String): Budget?

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND isActive = 1")
    fun getBudgetsByCategory(userId: String, categoryId: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND isActive = 1 AND startDate <= :date AND endDate >= :date")
    fun getActiveBudgetsForDate(userId: String, date: Long): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<Budget>)

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id AND userId = :userId")
    suspend fun deactivateBudget(userId: String, id: String)
}

