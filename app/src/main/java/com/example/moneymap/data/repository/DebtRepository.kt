package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.DebtDao
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.DebtType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepository @Inject constructor(
    private val debtDao: DebtDao
) {
    fun getAllDebts(): Flow<List<Debt>> {
        return debtDao.getAllDebts()
    }

    suspend fun getDebtById(id: String): Debt? {
        return debtDao.getDebtById(id)
    }

    fun getDebtsByType(type: DebtType): Flow<List<Debt>> {
        return debtDao.getDebtsByType(type)
    }

    suspend fun insertDebt(debt: Debt) {
        debtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt)
    }
}

