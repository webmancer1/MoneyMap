package com.example.moneymap.data.database.dao

import androidx.room.*
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.DebtType
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: String): Debt?

    @Query("SELECT * FROM debts WHERE type = :type ORDER BY createdAt DESC")
    fun getDebtsByType(type: DebtType): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<Debt>)

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)
}

