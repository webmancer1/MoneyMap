package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.GoalDao
import com.example.moneymap.data.model.Goal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals()
    }

    suspend fun getGoalById(id: String): Goal? {
        return goalDao.getGoalById(id)
    }

    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }
}

