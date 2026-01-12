package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.GoalDao
import com.example.moneymap.data.model.Goal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

import com.google.firebase.auth.FirebaseAuth

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val firebaseAuth: FirebaseAuth
) {
    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""
    fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals(currentUserId)
    }

    suspend fun getGoalById(id: String): Goal? {
        return goalDao.getGoalById(currentUserId, id)
    }

    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal.copy(userId = currentUserId))
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }
}

