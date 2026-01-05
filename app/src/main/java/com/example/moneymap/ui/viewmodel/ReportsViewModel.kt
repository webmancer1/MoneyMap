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
    val currentMonthData: List<ChartDataPoint> = emptyList(),
    val yearlyData: List<ChartDataPoint> = emptyList(),
    val paymentMethodSpending: List<PaymentMethodSpending> = emptyList(),
    val spendingInsights: List<SpendingInsight> = emptyList(),
    val averageDailySpending: Double = 0.0,
    val averageTransactionAmount: Double = 0.0,
    val totalTransactions: Int = 0,
    val currentMonthLabel: String = "",
    val currentYearLabel: String = "",
    val currency: String = "KES"
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
    private val selectedMonthOffset = MutableStateFlow(0)
    private val selectedYearOffset = MutableStateFlow(0)

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
            val filtersFlow = combine(
                selectedPeriod,
                selectedMonthOffset,
                selectedYearOffset
            ) { period, monthOffset, yearOffset ->
                Triple(period, monthOffset, yearOffset)
            }

            combine(
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories(),
                settingsRepository.settingsFlow,
                filtersFlow
            ) { transactions, categories, settings, filters ->
                val (period, monthOffset, yearOffset) = filters
                buildUiState(
                    transactions = transactions,
                    categories = categories,
                    period = period,
                    displayCurrency = settings.currency,
                    monthOffset = monthOffset,
                    yearOffset = yearOffset
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
        displayCurrency: String,
        monthOffset: Int,
        yearOffset: Int
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
                    categories.firstOrNull { it.id == transaction.categoryId }?.name
                        ?: "Uncategorized"
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
                        color = categories.firstOrNull { it.name == categoryName }?.color
                            ?: "#808080",
                        transactionCount = count,
                        averageAmount = if (avg.isFinite()) avg else 0.0
                    )
                }
                .sortedByDescending { it.amount }

            // Fixed Charts Data
            val (monthStart, monthEnd) = getMonthRange(monthOffset)
            val monthTransactions = transactions.filter { it.date in monthStart..monthEnd }
            val currentMonthData =
                computeMonthTotalData(monthTransactions, monthStart, displayCurrency)
            val currentMonthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(monthStart))

            val (yearStart, yearEnd) = getYearRange(yearOffset)
            val yearTransactions = transactions.filter { it.date in yearStart..yearEnd }
            val yearlyData =
                computeYearTotalData(yearTransactions, yearStart, displayCurrency)
            val currentYearLabel = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(yearStart))

            val paymentMethodSpending = expenseTransactions
                .filter { it.paymentMethod != null }
                .groupBy { it.paymentMethod }
                .mapNotNull { (method, transactions) ->
                    method?.let { m ->
                        PaymentMethodSpending(
                            paymentMethod = m.name.replace("_", " "),
                            amount = let {
                                val sum = transactions.sumOf { tx ->
                                    currencyRepository.convert(
                                        tx.amount,
                                        tx.currency,
                                        displayCurrency
                                    )
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
                period = period,
                displayCurrency = displayCurrency
            )

            ReportsUiState(
                selectedPeriod = period,
                totalIncome = if (totalIncome.isFinite()) totalIncome else 0.0,
                totalExpense = if (totalExpense.isFinite()) totalExpense else 0.0,
                spendingByCategory = spendingByCategory,
                currentMonthData = currentMonthData,
                yearlyData = yearlyData,
                paymentMethodSpending = paymentMethodSpending,
                spendingInsights = spendingInsights,
                averageDailySpending = averageDailySpending,
                averageTransactionAmount = averageTransactionAmount,
                totalTransactions = totalTransactions,
                currentMonthLabel = currentMonthLabel,
                currentYearLabel = currentYearLabel,
                currency = displayCurrency
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty state if there's an error
            ReportsUiState(
                selectedPeriod = period,
                totalIncome = 0.0,
                totalExpense = 0.0,
                spendingByCategory = emptyList(),
                currentMonthData = emptyList(),
                yearlyData = emptyList(),
                paymentMethodSpending = emptyList(),
                spendingInsights = emptyList(),
                averageDailySpending = 0.0,
                averageTransactionAmount = 0.0,
                totalTransactions = 0,
                currentMonthLabel = "",
                currentYearLabel = "",
                currency = "KES"
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







    private fun computeMonthTotalData(
        transactions: List<Transaction>,
        monthStart: Long,
        displayCurrency: String
    ): List<ChartDataPoint> {
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
            
        val expense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
            
        return listOf(
            ChartDataPoint(
                label = monthFormat.format(Date(monthStart)),
                income = if (income.isFinite()) income else 0.0,
                expense = if (expense.isFinite()) expense else 0.0,
                timestamp = monthStart
            )
        )
    }

    private fun computeMonthlyChartData(
        transactions: List<Transaction>,
        startMs: Long,
        endMs: Long,
        displayCurrency: String
    ): List<ChartDataPoint> {
        val result = mutableListOf<ChartDataPoint>()
        val iterateCal = Calendar.getInstance()
        iterateCal.timeInMillis = startMs

        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endMs

        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        if (startMs > endMs) return emptyList()

        while (iterateCal.timeInMillis <= endCal.timeInMillis) {
            // Start of month is iterateCal
            val monthStart = iterateCal.timeInMillis

            val tempCal = iterateCal.clone() as Calendar
            tempCal.add(Calendar.MONTH, 1)
            tempCal.add(Calendar.MILLISECOND, -1)
            val monthEnd = tempCal.timeInMillis

            val (mIncome, mExpense) = transactions
                .filter { it.date in monthStart..monthEnd }
                .let { monthTxs ->
                    val inc = monthTxs.filter { it.type == TransactionType.INCOME }
                        .sumOf {
                            currencyRepository.convert(
                                it.amount,
                                it.currency,
                                displayCurrency
                            )
                        }
                    val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }
                        .sumOf {
                            currencyRepository.convert(
                                it.amount,
                                it.currency,
                                displayCurrency
                            )
                        }
                    inc to exp
                }

            result.add(
                ChartDataPoint(
                    label = monthFormat.format(Date(monthStart)),
                    income = if (mIncome.isFinite()) mIncome else 0.0,
                    expense = if (mExpense.isFinite()) mExpense else 0.0,
                    timestamp = monthStart
                )
            )

            iterateCal.add(Calendar.MONTH, 1)
            if (result.size > 24) break
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
        period: ReportPeriod,
        displayCurrency: String
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        val currencyInstance = java.util.Currency.getInstance(displayCurrency)
        val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale.getDefault())
        currencyFormat.currency = currencyInstance
        
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
    private fun computeYearTotalData(
        transactions: List<Transaction>,
        yearStart: Long,
        displayCurrency: String
    ): List<ChartDataPoint> {
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        
        val income = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
            
        val expense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { tx -> currencyRepository.convert(tx.amount, tx.currency, displayCurrency) }
            
        return listOf(
            ChartDataPoint(
                label = yearFormat.format(Date(yearStart)),
                income = if (income.isFinite()) income else 0.0,
                expense = if (expense.isFinite()) expense else 0.0,
                timestamp = yearStart
            )
        )
    }
    
    fun navigateMonth(direction: Int) {
        selectedMonthOffset.value += direction
    }

    fun navigateYear(direction: Int) {
        selectedYearOffset.value += direction
    }

    private fun getMonthRange(offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, offset)
        
        // End of month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        // Start of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        return startDate to endDate
    }

    private fun getYearRange(offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, offset)
        
        calendar.set(Calendar.MONTH, Calendar.DECEMBER)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        return startDate to endDate
    }
}
