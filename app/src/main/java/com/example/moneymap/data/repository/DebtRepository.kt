package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.DebtDao
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.DebtType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.google.firebase.auth.FirebaseAuth

@Singleton
class DebtRepository @Inject constructor(
    private val debtDao: DebtDao,
    private val firebaseAuth: FirebaseAuth
) {
    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""
    fun getAllDebts(): Flow<List<Debt>> {
        return debtDao.getAllDebts(currentUserId)
    }

    suspend fun getDebtById(id: String): Debt? {
        return debtDao.getDebtById(currentUserId, id)
    }

    fun getDebtsByType(type: DebtType): Flow<List<Debt>> {
        return debtDao.getDebtsByType(currentUserId, type)
    }

    suspend fun insertDebt(debt: Debt) {
        debtDao.insertDebt(debt.copy(userId = currentUserId))
    }

    suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) {
        debtDao.deleteDebt(debt)
    }
}

