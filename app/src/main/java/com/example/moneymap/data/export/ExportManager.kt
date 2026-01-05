package com.example.moneymap.data.export

import android.content.Context
import com.example.moneymap.data.repository.*
import com.example.moneymap.data.sync.SyncResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val debtRepository: DebtRepository
) {
    suspend fun exportData(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch all data
                val transactions = transactionRepository.getAllTransactions().first()
                val categories = categoryRepository.getAllCategories().first()
                val budgets = budgetRepository.getAllBudgets().first()
                val goals = goalRepository.getAllGoals().first()
                val debts = debtRepository.getAllDebts().first()

                // Create a temporary directory for CSVs
                val exportDir = File(context.cacheDir, "export_temp")
                if (exportDir.exists()) exportDir.deleteRecursively()
                exportDir.mkdirs()

                // 1. Transactions CSV
                val transFile = File(exportDir, "transactions.csv")
                transFile.printWriter().use { out ->
                    out.println("Date,Type,Category,Amount,Currency,Notes,Payment Method")
                    transactions.forEach { t ->
                        val categoryName = categories.find { it.id == t.categoryId }?.name ?: "Unknown"
                        out.println(
                            "${formatDate(t.date)},${t.type.name},${escapeCsv(categoryName)},${t.amount},${t.currency},${escapeCsv(t.notes ?: "")},${escapeCsv(t.paymentMethod?.name ?: "")}"
                        )
                    }
                }

                // 2. Categories CSV
                val catFile = File(exportDir, "categories.csv")
                catFile.printWriter().use { out ->
                    out.println("Name,Type")
                    categories.forEach { c ->
                        out.println("${escapeCsv(c.name)},${c.type.name}")
                    }
                }

                // 3. Budgets CSV
                val budgetFile = File(exportDir, "budgets.csv")
                budgetFile.printWriter().use { out ->
                    out.println("Category,Amount,Period,Start Date,End Date")
                    budgets.forEach { b ->
                        val categoryName = categories.find { it.id == b.categoryId }?.name ?: "Unknown"
                        out.println(
                            "${escapeCsv(categoryName)},${b.amount},${b.period.name},${formatDate(b.startDate)},${formatDate(b.endDate)}"
                        )
                    }
                }

                // 4. Goals CSV
                val goalFile = File(exportDir, "goals.csv")
                goalFile.printWriter().use { out ->
                    out.println("Name,Target Amount,Saved Amount,Target Date")
                    goals.forEach { g ->
                        out.println(
                            "${escapeCsv(g.name)},${g.targetAmount},${g.savedAmount},${g.targetDate?.let { formatDate(it) } ?: ""}"
                        )
                    }
                }

                // 5. Debts CSV
                val debtFile = File(exportDir, "debts.csv")
                debtFile.printWriter().use { out ->
                    out.println("Type,Person,Total Amount,Paid Amount,Due Date,Notes")
                    debts.forEach { d ->
                        out.println(
                            "${d.type.name},${escapeCsv(d.personName)},${d.totalAmount},${d.paidAmount},${d.dueDate?.let { formatDate(it) } ?: ""},${escapeCsv(d.notes ?: "")}"
                        )
                    }
                }

                // Zip files
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val zipFileName = "MoneyMap_Export_$timeStamp.zip"
                val zipFile = File(context.cacheDir, zipFileName)

                ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                    exportDir.listFiles()?.forEach { file ->
                        val entry = ZipEntry(file.name)
                        zos.putNextEntry(entry)
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }

                // Cleanup temp dir
                exportDir.deleteRecursively()

                SyncResult.Success(zipFile.absolutePath)

            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Export failed")
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    private fun escapeCsv(value: String): String {
        var escaped = value.replace("\"", "\"\"")
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }
}
