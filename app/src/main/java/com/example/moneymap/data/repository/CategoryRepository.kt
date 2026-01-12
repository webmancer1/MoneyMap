package com.example.moneymap.data.repository

import com.example.moneymap.data.database.dao.CategoryDao
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.CategoryType
import com.example.moneymap.data.util.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val firebaseAuth: FirebaseAuth
) {
    private val currentUserId: String
        get() = firebaseAuth.currentUser?.uid ?: ""

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories(currentUserId)
    }

    suspend fun getCategoryById(id: String): Category? {
        return categoryDao.getCategoryById(currentUserId, id)
    }

    fun getCategoriesByType(type: CategoryType): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(currentUserId, type)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category.copy(userId = currentUserId))
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    suspend fun deactivateCategory(id: String) {
        categoryDao.deactivateCategory(currentUserId, id)
    }

    suspend fun initializeDefaultCategories() {
        val existingCategories = categoryDao.getAllCategories(currentUserId).first()
        if (existingCategories.isEmpty()) {
            val defaultCategories = DefaultCategories.getDefaultCategories().map { 
                it.copy(userId = currentUserId) 
            }
            categoryDao.insertCategories(defaultCategories)
        }
    }
}

