package com.example.moneymap.data.database.dao

import androidx.room.*
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId AND isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(userId: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id AND userId = :userId")
    suspend fun getCategoryById(userId: String, id: String): Category?

    @Query("SELECT * FROM categories WHERE userId = :userId AND type = :type AND isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getCategoriesByType(userId: String, type: CategoryType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategories(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("UPDATE categories SET isActive = 0 WHERE id = :id AND userId = :userId")
    suspend fun deactivateCategory(userId: String, id: String)
}

