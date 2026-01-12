package com.example.moneymap.data.database.dao

import androidx.room.*
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.DebtType
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllDebts(userId: String): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id AND userId = :userId")
    suspend fun getDebtById(userId: String, id: String): Debt?

    @Query("SELECT * FROM debts WHERE userId = :userId AND type = :type ORDER BY createdAt DESC")
    fun getDebtsByType(userId: String, type: DebtType): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<Debt>)

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)
}

