package com.example.moneymap.data.database.dao

import androidx.room.*
import com.example.moneymap.data.model.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllGoals(userId: String): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id AND userId = :userId")
    suspend fun getGoalById(userId: String, id: String): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<Goal>)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}

