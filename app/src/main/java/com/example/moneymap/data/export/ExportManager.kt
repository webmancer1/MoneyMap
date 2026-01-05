package com.example.moneymap.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.moneymap.data.repository.*
import com.example.moneymap.data.sync.SyncResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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
    enum class ExportType { CSV, PDF }

    suspend fun exportData(type: ExportType): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                if (type == ExportType.CSV) {
                    exportCsvZip()
                } else {
                    exportPdf()
                }
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Export failed")
            }
        }
    }

    private suspend fun exportCsvZip(): SyncResult {
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

        return SyncResult.Success(zipFile.absolutePath)
    }

    private suspend fun exportPdf(): SyncResult {
        // Fetch all data
        val transactions = transactionRepository.getAllTransactions().first()
        val categories = categoryRepository.getAllCategories().first()
        val budgets = budgetRepository.getAllBudgets().first()
        val goals = goalRepository.getAllGoals().first()
        val debts = debtRepository.getAllDebts().first()

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = Color.BLACK
        }
        val headerPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = Color.DKGRAY
        }
        val textPaint = Paint().apply {
            textSize = 10f
            color = Color.BLACK
        }

        var y = 40f
        val margin = 20f

        // Title
        canvas.drawText("MoneyMap Data Export", margin, y, titlePaint)
        y += 30f

        // Helper to check page break
        fun checkPageBreak(heightNeeded: Float) {
            if (y + heightNeeded > pageInfo.pageHeight - margin) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
        }

        // --- Transactions Section ---
        checkPageBreak(40f) // Header space
        canvas.drawText("Transactions", margin, y, titlePaint)
        y += 20f
        
        val colWidths = floatArrayOf(70f, 50f, 80f, 60f, 40f, 100f) // Date, Type, Category, Amount, Cur, Notes
        val headers = listOf("Date", "Type", "Category", "Amount", "Cur", "Notes")
        
        var x = margin
        headers.forEachIndexed { i, h -> 
            canvas.drawText(h, x, y, headerPaint)
            x += colWidths[i]
        }
        y += 15f
        canvas.drawLine(margin, y, 595f - margin, y, Paint().apply { color = Color.LTGRAY })
        y += 15f

        transactions.forEach { t ->
            checkPageBreak(20f)
            x = margin
            val catName = categories.find { it.id == t.categoryId }?.name ?: "-"
            val data = listOf(
                formatDate(t.date),
                t.type.name.take(3),
                catName.take(15),
                t.amount.toString(),
                t.currency,
                (t.notes ?: "").take(20)
            )
            data.forEachIndexed { i, text ->
                canvas.drawText(text, x, y, textPaint)
                x += colWidths[i]
            }
            y += 15f
        }
        y += 20f


        // --- Budgets Section ---
        checkPageBreak(40f)
        canvas.drawText("Budgets", margin, y, titlePaint)
        y += 20f
        
        x = margin
        listOf("Category", "Amount", "Period", "Start", "End").forEachIndexed { i, h ->
           // Reusing manual spacing approx
           val w = if (i == 0) 100f else 70f
           canvas.drawText(h, x, y, headerPaint)
           x += w
        }
        y += 15f
        canvas.drawLine(margin, y, 595f - margin, y, Paint().apply { color = Color.LTGRAY })
        y += 15f

        budgets.forEach { b ->
            checkPageBreak(20f)
            x = margin
            val catName = categories.find { it.id == b.categoryId }?.name ?: "-"
            val data = listOf(catName, b.amount.toString(), b.period.name, formatDate(b.startDate), formatDate(b.endDate))
            data.forEachIndexed { i, text ->
                val w = if (i == 0) 100f else 70f
                canvas.drawText(text, x, y, textPaint)
                x += w
            }
            y += 15f
        }
        y += 20f
        
        // --- Goals Section ---
        checkPageBreak(40f)
        canvas.drawText("Goals", margin, y, titlePaint)
        y += 20f
        
        x = margin
        listOf("Name", "Target", "Saved", "Date").forEachIndexed { i, h ->
             val w = if (i == 0) 120f else 80f
             canvas.drawText(h, x, y, headerPaint)
             x += w
        }
        y += 15f
        canvas.drawLine(margin, y, 595f - margin, y, Paint().apply { color = Color.LTGRAY })
        y += 15f

        goals.forEach { g ->
            checkPageBreak(20f)
            x = margin
            val data = listOf(g.name, g.targetAmount.toString(), g.savedAmount.toString(), g.targetDate?.let { formatDate(it) } ?: "-")
            data.forEachIndexed { i, text ->
                 val w = if (i == 0) 120f else 80f
                 canvas.drawText(text, x, y, textPaint)
                 x += w
            }
            y += 15f
        }
        y += 20f

        // --- Debts Section ---
        checkPageBreak(40f)
        canvas.drawText("Debts", margin, y, titlePaint)
        y += 20f

        x = margin
        listOf("Type", "Person", "Total", "Paid", "Due").forEachIndexed { i, h ->
             val w = if (i == 1) 120f else 70f
             canvas.drawText(h, x, y, headerPaint)
             x += w
        }
        y += 15f
        canvas.drawLine(margin, y, 595f - margin, y, Paint().apply { color = Color.LTGRAY })
        y += 15f

        debts.forEach { d ->
            checkPageBreak(20f)
            x = margin
            val data = listOf(d.type.name, d.personName, d.totalAmount.toString(), d.paidAmount.toString(), d.dueDate?.let { formatDate(it) } ?: "-")
            data.forEachIndexed { i, text ->
                 val w = if (i == 1) 120f else 70f
                 canvas.drawText(text, x, y, textPaint)
                 x += w
            }
            y += 15f
        }


        document.finishPage(page)

        // Save File
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "MoneyMap_Report_$timeStamp.pdf"
        val file = File(context.cacheDir, fileName)
        
        try {
            document.writeTo(FileOutputStream(file))
        } catch (e: IOException) {
            e.printStackTrace()
            document.close()
            throw e
        }
        document.close()

        return SyncResult.Success(file.absolutePath)
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
