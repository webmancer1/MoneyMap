package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.preferences.SettingsRepository
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.CurrencyRepository
import com.example.moneymap.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ReportPeriod(val months: Int, val label: String) {
    MONTH(1, "1M"),
    THREE_MONTHS(3, "3M"),
    SIX_MONTHS(6, "6M"),
    YEAR(12, "1Y")
}

data class CategorySpending(
    val categoryName: String,
    val amount: Double,
    val color: String,
    val transactionCount: Int = 0,
    val averageAmount: Double = 0.0
)

data class ChartDataPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val timestamp: Long
)

data class PaymentMethodSpending(
    val paymentMethod: String,
    val amount: Double,
    val transactionCount: Int = 0
)

data class SpendingInsight(
    val title: String,
    val value: String,
    val description: String
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val spendingByCategory: List<CategorySpending> = emptyList(),
    val chartData: List<ChartDataPoint> = emptyList(),
    val paymentMethodSpending: List<PaymentMethodSpending> = emptyList(),
    val spendingInsights: List<SpendingInsight> = emptyList(),
    val averageDailySpending: Double = 0.0,
    val averageTransactionAmount: Double = 0.0,
    val totalTransactions: Int = 0
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val selectedPeriod = MutableStateFlow(ReportPeriod.MONTH)

    init {
        observeReports()
    }

    fun onPeriodSelected(period: ReportPeriod) {
        if (selectedPeriod.value != period) {
            selectedPeriod.value = period
        }
    }

    private fun observeReports() {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories(),
                selectedPeriod,
                settingsRepository.settingsFlow
            ) { transactions, categories, period, settings ->
                buildUiState(
                    transactions = transactions,
                    categories = categories,
                    period = period,
                    displayCurrency = settings.currency
                )
            }
            .flowOn(Dispatchers.Default)
            .collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildUiState(
        transactions: List<Transaction>,
        categories: List<Category>,
        period: ReportPeriod,
        displayCurrency: String
    ): ReportsUiState {
        return try {
            val (startDate, endDate) = calculatePeriodRange(period)
            val filtered = transactions.filter { 
                it.date >= startDate && it.date <= endDate 
            }

            val totalIncome = filtered
                .filter { it.type == TransactionType.INCOME }
                .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
            val totalExpense = filtered
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }

            val expenseTransactions = filtered.filter { it.type == TransactionType.EXPENSE }
            
            val spendingByCategory = expenseTransactions
                .groupBy { transaction ->
                    categories.firstOrNull { it.id == transaction.categoryId }?.name ?: "Uncategorized"
                }
                .map { (categoryName, categoryTransactions) ->
                    val total = categoryTransactions.sumOf { tx ->
                        currencyRepository.convert(tx.amount, tx.currency, displayCurrency)
                    }
                    val count = categoryTransactions.size
                    val avg = if (count > 0) total / count else 0.0
                    CategorySpending(
                        categoryName = categoryName,
                        amount = if (total.isFinite()) total else 0.0,
                        color = categories.firstOrNull { it.name == categoryName }?.color ?: "#808080",
                        transactionCount = count,
                        averageAmount = if (avg.isFinite()) avg else 0.0
                    )
                }
                .sortedByDescending { it.amount }

            val chartData = computeChartData(filtered, period, displayCurrency)
            
            val paymentMethodSpending = expenseTransactions
                .filter { it.paymentMethod != null }
                .groupBy { it.paymentMethod }
                .mapNotNull { (method, transactions) ->
                    method?.let { m ->
                        PaymentMethodSpending(
                            paymentMethod = m.name.replace("_", " "),
                            amount = let {
                                val sum = transactions.sumOf { tx ->
                                    currencyRepository.convert(tx.amount, tx.currency, displayCurrency)
                                }
                                if (sum.isFinite()) sum else 0.0
                            },
                            transactionCount = transactions.size
                        )
                    }
                }
                .sortedByDescending { it.amount }

            val totalTransactions = filtered.size
            val expenseCount = expenseTransactions.size
            val averageTransactionAmount = if (expenseCount > 0) {
                val avg = totalExpense / expenseCount
                if (avg.isFinite()) avg else 0.0
            } else 0.0
            
            val daysInPeriod = calculateDaysInPeriod(period)
            val averageDailySpending = if (daysInPeriod > 0) {
                val avg = totalExpense / daysInPeriod
                if (avg.isFinite()) avg else 0.0
            } else 0.0

            val spendingInsights = buildSpendingInsights(
                totalExpense = if (totalExpense.isFinite()) totalExpense else 0.0,
                totalIncome = if (totalIncome.isFinite()) totalIncome else 0.0,
                averageDailySpending = averageDailySpending,
                topCategory = spendingByCategory.firstOrNull(),
                expenseCount = expenseCount,
                period = period
            )

            ReportsUiState(
                selectedPeriod = period,
                totalIncome = if (totalIncome.isFinite()) totalIncome else 0.0,
                totalExpense = if (totalExpense.isFinite()) totalExpense else 0.0,
                spendingByCategory = spendingByCategory,
                chartData = chartData,
                paymentMethodSpending = paymentMethodSpending,
                spendingInsights = spendingInsights,
                averageDailySpending = averageDailySpending,
                averageTransactionAmount = averageTransactionAmount,
                totalTransactions = totalTransactions
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty state if there's an error
            ReportsUiState(
                selectedPeriod = period,
                totalIncome = 0.0,
                totalExpense = 0.0,
                spendingByCategory = emptyList(),
                chartData = emptyList(),
                paymentMethodSpending = emptyList(),
                spendingInsights = emptyList(),
                averageDailySpending = 0.0,
                averageTransactionAmount = 0.0,
                totalTransactions = 0
            )
        }
    }

    private fun calculatePeriodRange(period: ReportPeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, -(period.months - 1))

        val startDate = calendar.timeInMillis
        return startDate to endDate
    }

    private fun computeChartData(
        transactions: List<Transaction>,
        period: ReportPeriod,
        displayCurrency: String
    ): List<ChartDataPoint> {
        val result = mutableListOf<ChartDataPoint>()
        val calendar = Calendar.getInstance()
        
        // Reset time part
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (period == ReportPeriod.MONTH) {
            // Daily breakdown for the current month
            // We need to iterate from day 1 to end of month/today?
            // "1 Month" usually implies "This Month" or "Last 30 Days"?
            // Based on calculatePeriodRange, it subtracts (months - 1). 
            // If MONTH(1), it subtracts 0, so it is just this current month range?
            // Actually calculatePeriodRange does:
            // endDate = now
            // startDate = 1st of (now - 0 months) = 1st of this month
            
            // So we iterate from startDate to endDate (which is today)
            // But usually charts show valid days.
            
            val (startMs, endMs) = calculatePeriodRange(period)
            
            // Iterate day by day from start to end
            val iterateCal = Calendar.getInstance()
            iterateCal.timeInMillis = startMs
            
            val endCal = Calendar.getInstance()
            endCal.timeInMillis = endMs
            
            val dayFormat = SimpleDateFormat("d", Locale.getDefault())
            
            while (iterateCal.timeInMillis <= endCal.timeInMillis) {
                val dayStart = iterateCal.timeInMillis
                
                // End of this day
                val tempCal = iterateCal.clone() as Calendar
                tempCal.add(Calendar.DAY_OF_MONTH, 1)
                tempCal.add(Calendar.MILLISECOND, -1)
                val dayEnd = tempCal.timeInMillis
                
                val dailyTransactions = transactions.filter { it.date in dayStart..dayEnd }
                
                val income = dailyTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
                val expense = dailyTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
                
                // Only add if we want to show all days or just days with data?
                // Usually showing all days 1..Current looks better
                result.add(
                    ChartDataPoint(
                        label = dayFormat.format(Date(dayStart)),
                        income = income,
                        expense = expense,
                        timestamp = dayStart
                    )
                )
                
                iterateCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        } else {
            // Monthly breakdown for > 1 month
            // Existing logic adapted
            // We iterate back from current month to (period.months - 1) ago
            
            // Normalize calendar to start of this month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            
            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            
            // We want chronological order? The loop downTo 0 suggests we build latest first? 
            // Ah no, loop says 0 is current month, period-1 is oldest.
            // If we want result to be chronological, we should iterate 0..period-1 but 
            // calculate months ago.
            // Or iterate period-1 downTo 0 and add to list.
            
            for (i in period.months - 1 downTo 0) {
                val monthCalendar = (calendar.clone() as Calendar)
                monthCalendar.add(Calendar.MONTH, -i)
                val start = monthCalendar.timeInMillis

                monthCalendar.add(Calendar.MONTH, 1)
                monthCalendar.add(Calendar.MILLISECOND, -1)
                val end = monthCalendar.timeInMillis

                val monthlyTransactions = transactions.filter { it.date in start..end }
                val income = monthlyTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
                val expense = monthlyTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }

                val label = monthFormat.format(Date(start))
                result.add(ChartDataPoint(label, income, expense, start))
            }
        }

        return result
    }

    private fun calculateDaysInPeriod(period: ReportPeriod): Int {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.MONTH, -(period.months - 1))
        val startDate = calendar.timeInMillis
        
        return ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
    }

    private fun buildSpendingInsights(
        totalExpense: Double,
        totalIncome: Double,
        averageDailySpending: Double,
        topCategory: CategorySpending?,
        expenseCount: Int,
        period: ReportPeriod
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.getDefault())
        
        // Savings rate
        if (totalIncome > 0) {
            val savingsRate = ((totalIncome - totalExpense) / totalIncome * 100).coerceAtLeast(0.0)
            insights.add(
                SpendingInsight(
                    title = "Savings Rate",
                    value = "${String.format("%.1f", savingsRate)}%",
                    description = if (savingsRate > 0) "You're saving well!" else "Try to reduce expenses"
                )
            )
        }
        
        // Top spending category
        topCategory?.let {
            insights.add(
                SpendingInsight(
                    title = "Top Category",
                    value = it.categoryName,
                    description = "${currencyFormat.format(it.amount)} across ${it.transactionCount} transactions"
                )
            )
        }
        
        // Average daily spending
        insights.add(
            SpendingInsight(
                title = "Daily Average",
                value = currencyFormat.format(averageDailySpending),
                description = "Average spending per day in this period"
            )
        )
        
        // Transaction frequency
        val avgTransactionsPerDay = if (expenseCount > 0) {
            val days = calculateDaysInPeriod(period)
            expenseCount.toDouble() / days
        } else 0.0
        insights.add(
            SpendingInsight(
                title = "Transaction Frequency",
                value = String.format("%.1f", avgTransactionsPerDay),
                description = "Transactions per day on average"
            )
        )
        
        return insights
    }
}
