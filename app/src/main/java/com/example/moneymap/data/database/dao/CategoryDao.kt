package com.example.moneymap.data.database.dao

import androidx.room.*
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): Category?

    @Query("SELECT * FROM categories WHERE type = :type AND isActive = 1 ORDER BY isDefault DESC, name ASC")
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>

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

    @Query("UPDATE categories SET isActive = 0 WHERE id = :id")
    suspend fun deactivateCategory(id: String)
}

