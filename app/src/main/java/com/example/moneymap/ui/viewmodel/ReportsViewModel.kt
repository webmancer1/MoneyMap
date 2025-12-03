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
    val transactionCount: Int = 0,
    val averageAmount: Double = 0.0
)

data class MonthlyIncomeExpense(
    val monthLabel: String,
    val income: Double,
    val expense: Double
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
    val monthlyIncomeExpense: List<MonthlyIncomeExpense> = emptyList(),
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
            }.collect { state ->
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
                CategorySpending(
                    categoryName = categoryName,
                    amount = total,
                    transactionCount = count,
                    averageAmount = if (count > 0) total / count else 0.0
                )
            }
            .sortedByDescending { it.amount }

        val monthlyIncomeExpense = computeMonthlyIncomeExpense(filtered, period, displayCurrency)
        
        val paymentMethodSpending = expenseTransactions
            .filter { it.paymentMethod != null }
            .groupBy { it.paymentMethod }
            .mapNotNull { (method, transactions) ->
                method?.let { m ->
                    PaymentMethodSpending(
                        paymentMethod = m.name.replace("_", " "),
                        amount = transactions.sumOf { tx ->
                            currencyRepository.convert(tx.amount, tx.currency, displayCurrency)
                        },
                        transactionCount = transactions.size
                    )
                }
            }
            .sortedByDescending { it.amount }

        val totalTransactions = filtered.size
        val expenseCount = expenseTransactions.size
        val averageTransactionAmount = if (expenseCount > 0) {
            totalExpense / expenseCount
        } else 0.0
        
        val daysInPeriod = calculateDaysInPeriod(period)
        val averageDailySpending = if (daysInPeriod > 0) {
            totalExpense / daysInPeriod
        } else 0.0

        val spendingInsights = buildSpendingInsights(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            averageDailySpending = averageDailySpending,
            topCategory = spendingByCategory.firstOrNull(),
            expenseCount = expenseCount,
            period = period
        )

            ReportsUiState(
                selectedPeriod = period,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                spendingByCategory = spendingByCategory,
                monthlyIncomeExpense = monthlyIncomeExpense,
                paymentMethodSpending = paymentMethodSpending,
                spendingInsights = spendingInsights,
                averageDailySpending = averageDailySpending,
                averageTransactionAmount = averageTransactionAmount,
                totalTransactions = totalTransactions
            )
        } catch (e: Exception) {
            // Return empty state if there's an error
            ReportsUiState(
                selectedPeriod = period,
                totalIncome = 0.0,
                totalExpense = 0.0,
                spendingByCategory = emptyList(),
                monthlyIncomeExpense = emptyList(),
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

    private fun computeMonthlyIncomeExpense(
        transactions: List<Transaction>,
        period: ReportPeriod,
        displayCurrency: String
    ): List<MonthlyIncomeExpense> {
        if (transactions.isEmpty()) return emptyList()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val result = mutableListOf<MonthlyIncomeExpense>()

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
            result.add(MonthlyIncomeExpense(label, income, expense))
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
