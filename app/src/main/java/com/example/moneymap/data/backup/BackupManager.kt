package com.example.moneymap.data.backup

import com.example.moneymap.data.model.Budget
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.Goal
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.repository.BudgetRepository
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.DebtRepository
import com.example.moneymap.data.repository.GoalRepository
import com.example.moneymap.data.repository.TransactionRepository
import com.example.moneymap.data.sync.SyncResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class BackupData(
    val timestamp: Long,
    val translations: List<Transaction>,
    val categories: List<Category>,
    val budgets: List<Budget>,
    val goals: List<Goal>,
    val debts: List<Debt>
)

@Singleton
class BackupManager @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val debtRepository: DebtRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) {

    private val gson = Gson()

    suspend fun createBackup(): SyncResult {
        val userId = firebaseAuth.currentUser?.uid ?: return SyncResult.Error("User not authenticated")

        return try {
            val backupData = BackupData(
                timestamp = System.currentTimeMillis(),
                translations = transactionRepository.getAllTransactions().first(),
                categories = categoryRepository.getAllCategories().first(),
                budgets = budgetRepository.getAllBudgets().first(),
                goals = goalRepository.getAllGoals().first(),
                debts = debtRepository.getAllDebts().first()
            )

            val jsonString = gson.toJson(backupData)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val path = "backups/$userId/backup_$timestamp.json"
            
            val storageRef = firebaseStorage.reference.child(path)
            storageRef.putBytes(jsonString.toByteArray()).await()

            SyncResult.Success("Backup uploaded to ${path}")
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Backup failed")
        }
    }
}
