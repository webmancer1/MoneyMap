package com.example.moneymap.data.sync

import com.example.moneymap.data.model.*
import com.example.moneymap.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val debtRepository: DebtRepository
) {
    private val userId: String?
        get() = firebaseAuth.currentUser?.uid

    suspend fun syncAll(): SyncResult {
        val userId = userId ?: return SyncResult.Error("User not authenticated")
        
        return try {
            var stats = "Synced: "
            val catCount = syncCategories(userId)
            val transCount = syncTransactions(userId)
            val budgetCount = syncBudgets(userId)
            val goalCount = syncGoals(userId)
            val debtCount = syncDebts(userId)
            
            stats += "$transCount Trans, $catCount Cats, $budgetCount Budgets"
            SyncResult.Success(stats)
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    suspend fun syncTransactions(userId: String): Int {
        var count = 0
        return try {
            // Push pending transactions to Firestore
            val pendingTransactions = transactionRepository.getPendingSyncTransactions()
            pendingTransactions.forEach { transaction ->
                uploadTransaction(userId, transaction)
                count++
            }

            // Pull transactions from Firestore
            val firestoreTransactions = firestore
                .collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .await()

            firestoreTransactions.documents.forEach { doc ->
                val firestoreTransaction = doc.toObject(Transaction::class.java)
                if (firestoreTransaction != null) {
                    val localTransaction = transactionRepository.getTransactionById(firestoreTransaction.id)
                    if (localTransaction == null || firestoreTransaction.updatedAt > localTransaction.updatedAt) {
                        // Firestore version is newer, update local
                        transactionRepository.insertTransaction(
                            firestoreTransaction.copy(syncStatus = SyncStatus.SYNCED)
                        )
                    }
                }
            }

            count
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun syncCategories(userId: String): Int {
        var count = 0
        return try {
            // Push categories to Firestore
            val categories = categoryRepository.getAllCategories().first()
            categories.forEach { category ->
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("categories")
                    .document(category.id)
                    .set(category, SetOptions.merge())
                    .await()
                count++
            }

            // Pull categories from Firestore
            val firestoreCategories = firestore
                .collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .await()

            firestoreCategories.documents.forEach { doc ->
                val category = doc.toObject(Category::class.java)
                if (category != null) {
                    val localCategory = categoryRepository.getCategoryById(category.id)
                    if (localCategory == null) {
                        // New category from cloud, add locally
                        categoryRepository.insertCategory(category)
                    }
                }
            }

            count
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun syncBudgets(userId: String): Int {
        var count = 0
        return try {
            // Push budgets to Firestore
            val budgets = budgetRepository.getAllBudgets().first()
            budgets.forEach { budget ->
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("budgets")
                    .document(budget.id)
                    .set(budget, SetOptions.merge())
                    .await()
                count++
            }

            // Pull budgets from Firestore
            val firestoreBudgets = firestore
                .collection("users")
                .document(userId)
                .collection("budgets")
                .get()
                .await()

            firestoreBudgets.documents.forEach { doc ->
                val budget = doc.toObject(Budget::class.java)
                if (budget != null) {
                    val localBudget = budgetRepository.getBudgetById(budget.id)
                    if (localBudget == null || budget.startDate > localBudget.startDate) {
                        budgetRepository.insertBudget(budget)
                    }
                }
            }

            count
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun syncGoals(userId: String): Int {
        var count = 0
        return try {
            // Push goals to Firestore
            val goals = goalRepository.getAllGoals().first()
            goals.forEach { goal ->
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("goals")
                    .document(goal.id)
                    .set(goal, SetOptions.merge())
                    .await()
                count++
            }

            // Pull goals from Firestore
            val firestoreGoals = firestore
                .collection("users")
                .document(userId)
                .collection("goals")
                .get()
                .await()

            firestoreGoals.documents.forEach { doc ->
                val goal = doc.toObject(Goal::class.java)
                if (goal != null) {
                    val localGoal = goalRepository.getGoalById(goal.id)
                    if (localGoal == null || goal.createdAt > localGoal.createdAt) {
                        goalRepository.insertGoal(goal)
                    }
                }
            }

            count
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun syncDebts(userId: String): Int {
        var count = 0
        return try {
            // Push debts to Firestore
            val debts = debtRepository.getAllDebts().first()
            debts.forEach { debt ->
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("debts")
                    .document(debt.id)
                    .set(debt, SetOptions.merge())
                    .await()
                count++
            }

            // Pull debts from Firestore
            val firestoreDebts = firestore
                .collection("users")
                .document(userId)
                .collection("debts")
                .get()
                .await()

            firestoreDebts.documents.forEach { doc ->
                val debt = doc.toObject(Debt::class.java)
                if (debt != null) {
                    val localDebt = debtRepository.getDebtById(debt.id)
                    if (localDebt == null || debt.createdAt > localDebt.createdAt) {
                        debtRepository.insertDebt(debt)
                    }
                }
            }

            count
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun uploadTransaction(userId: String, transaction: Transaction) {
        try {
            firestore
                .collection("users")
                .document(userId)
                .collection("transactions")
                .document(transaction.id)
                .set(transaction, SetOptions.merge())
                .await()

            // Mark as synced
            transactionRepository.updateTransaction(
                transaction.copy(syncStatus = SyncStatus.SYNCED)
            )
        } catch (e: Exception) {
            // Keep as pending on error
            transactionRepository.updateTransaction(
                transaction.copy(syncStatus = SyncStatus.PENDING)
            )
            throw e
        }
    }

    suspend fun deleteTransactionFromCloud(transactionId: String): SyncResult {
        val userId = userId ?: return SyncResult.Error("User not authenticated")
        return try {
            firestore
                .collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)
                .delete()
                .await()
            SyncResult.Success()
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Failed to delete transaction from cloud")
        }
    }
}

sealed class SyncResult {
    data class Success(val stats: String = "") : SyncResult()
    data class Error(val message: String) : SyncResult()
}

