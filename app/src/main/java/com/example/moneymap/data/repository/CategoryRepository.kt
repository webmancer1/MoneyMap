package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.CategoryDao
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.CategoryType
import com.example.moneymap.data.util.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getCategoryById(id)
    }

    fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deactivateCategory(id: String) {
        categoryDao.deactivateCategory(id)
    }

    suspend fun initializeDefaultCategories() {
        val existingCategories = categoryDao.getAllCategories().first()
        if (existingCategories.isEmpty()) {
            val defaultCategories = DefaultCategories.getDefaultCategories()
            categoryDao.insertCategories(defaultCategories)
        }
    }
}

