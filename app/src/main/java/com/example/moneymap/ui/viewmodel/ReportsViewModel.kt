package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.database.dao.CategorySpendingSum
import com.example.moneymap.data.database.dao.IncomeExpenseSum
import com.example.moneymap.data.database.dao.PaymentMethodSpendingSum
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.preferences.SettingsRepository
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.CurrencyRepository
import com.example.moneymap.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
    val currency: String = "KES",
    val isLoading: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
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
        selectedPeriod.value = period
    }

    fun navigateMonth(direction: Int) {
        selectedMonthOffset.value += direction
    }

    fun navigateYear(direction: Int) {
        selectedYearOffset.value += direction
    }

    private fun observeReports() {
        val currencyFlow = settingsRepository.settingsFlow.map { it.currency }.distinctUntilChanged()

        // 1. Summary Data Flow (Totals, Categories, Payment Methods, Insights)
        val summaryFlow = combine(selectedPeriod, currencyFlow) { period, currency ->
            Pair(period, currency)
        }.flatMapLatest { (period, currency) ->
            val (startDate, endDate) = calculatePeriodRange(period)
            
            combine(
                transactionRepository.getIncomeExpenseSum(startDate, endDate),
                transactionRepository.getCategorySpendingSum(startDate, endDate),
                transactionRepository.getPaymentMethodSpendingSum(startDate, endDate),
                categoryRepository.getAllCategories()
            ) { incomeExpenseSums, categorySums, paymentSums, categories ->
                computeSummaryData(
                    period = period,
                    displayCurrency = currency,
                    incomeExpenseSums = incomeExpenseSums,
                    categorySums = categorySums,
                    paymentSums = paymentSums,
                    categories = categories
                )
            }
        }

        // 2. Monthly Chart Flow (Daily breakdown for specific month)
        val monthlyChartFlow = combine(selectedMonthOffset, currencyFlow) { offset, currency ->
            Pair(offset, currency)
        }.flatMapLatest { (offset, currency) ->
            val (startDate, endDate) = getMonthRange(offset)
            val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startDate))
            
            // We fetch transactions for the month because we need daily granularity which is tricky to aggregate cleanly in room across timezones
            // But since it's just one month (~100-300 txs usually), it's fast enough.
            transactionRepository.getTransactionsByDateRange(startDate, endDate)
                .map { transactions ->
                     val data = computeMonthTotalData(transactions, startDate, currency)
                     Pair(monthLabel, data)
                }
        }

        // 3. Yearly Chart Flow (Monthly breakdown for specific year)
        val yearlyChartFlow = combine(selectedYearOffset, currencyFlow) { offset, currency ->
            Pair(offset, currency)
        }.flatMapLatest { (offset, currency) ->
            val (startDate, endDate) = getYearRange(offset)
            val yearLabel = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(startDate))

            transactionRepository.getTransactionsByDateRange(startDate, endDate)
                .map { transactions ->
                    val data = computeYearTotalData(transactions, startDate, currency)
                    Pair(yearLabel, data)
                }
        }

        // Combine everything into UI State
        combine(
            summaryFlow,
            monthlyChartFlow,
            yearlyChartFlow,
            currencyFlow
        ) { summary, (monthLabel, monthData), (yearLabel, yearData), currency ->
            summary.copy(
                currentMonthData = monthData,
                yearlyData = yearData,
                currentMonthLabel = monthLabel,
                currentYearLabel = yearLabel,
                currency = currency
            )
        }
        .flowOn(Dispatchers.Default)
        .onEach { _uiState.value = it }
        .launchIn(viewModelScope)
    }

    private fun computeSummaryData(
        period: ReportPeriod,
        displayCurrency: String,
        incomeExpenseSums: List<IncomeExpenseSum>,
        categorySums: List<CategorySpendingSum>,
        paymentSums: List<PaymentMethodSpendingSum>,
        categories: List<Category>
    ): ReportsUiState {
        // Totals
        var totalIncome = 0.0
        var totalExpense = 0.0

        incomeExpenseSums.forEach { sum ->
            val amount = currencyRepository.convert(sum.total, sum.currency, displayCurrency)
            if (sum.type == TransactionType.INCOME) {
                totalIncome += amount
            } else {
                totalExpense += amount
            }
        }

        // Category Spending
        val spendingByCategory = categorySums.map { sum ->
            val category = categories.find { it.id == sum.categoryId }
            CategorySpending(
                categoryName = category?.name ?: "Uncategorized",
                amount = currencyRepository.convert(sum.totalAmount, sum.currency, displayCurrency),
                color = category?.color ?: "#808080",
                transactionCount = sum.transactionCount,
                averageAmount = 0.0 // Placeholder, handled in grouping below
            )
        }
        // Group by category name (because different currencies might result in multiple entries for same category in the DTO list if we didn't group by currency in the map first... wait, DTO groups by category AND currency)
        // We need to merge them.
        val mergedCategorySpending = spendingByCategory
            .groupBy { it.categoryName }
            .map { (name, list) ->
                val total = list.sumOf { it.amount }
                val count = list.sumOf { it.transactionCount }
                val color = list.first().color
                CategorySpending(
                    categoryName = name,
                    amount = total,
                    color = color,
                    transactionCount = count,
                    averageAmount = if (count > 0) total / count else 0.0
                )
            }
            .sortedByDescending { it.amount }

        // Payment Method Spending
        val paymentMethodSpending = paymentSums.map { sum ->
             PaymentMethodSpending(
                 paymentMethod = sum.paymentMethod.name.replace("_", " "),
                 amount = currencyRepository.convert(sum.totalAmount, sum.currency, displayCurrency),
                 transactionCount = sum.transactionCount
             )
        }
        val mergedPaymentSpending = paymentMethodSpending
            .groupBy { it.paymentMethod }
            .map { (method, list) ->
                PaymentMethodSpending(
                    paymentMethod = method,
                    amount = list.sumOf { it.amount },
                    transactionCount = list.sumOf { it.transactionCount }
                )
            }
            .sortedByDescending { it.amount }

        // Insights / Stats
        val daysInPeriod = calculateDaysInPeriod(period)
        val averageDailySpending = if (daysInPeriod > 0) totalExpense / daysInPeriod else 0.0
        
        val totalTxCount = categorySums.sumOf { it.transactionCount } // Expense only count approx? Or use income+expense sums count? DTO only has sums for expenses.
        // Actually totalTransactions should include income too, but typically reports focus on expense count for frequency.
        // Let's use the sums we have. `incomeExpenseSums` doesn't have count.
        // We can approximate or just use expense count.
        
        val expenseCount = mergedCategorySpending.sumOf { it.transactionCount }
        val averageTransactionAmount = if (expenseCount > 0) totalExpense / expenseCount else 0.0

        val insights = buildSpendingInsights(
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            averageDailySpending = averageDailySpending,
            topCategory = mergedCategorySpending.firstOrNull(),
            expenseCount = expenseCount,
            period = period,
            displayCurrency = displayCurrency
        )

        return ReportsUiState(
            selectedPeriod = period,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            spendingByCategory = mergedCategorySpending,
            paymentMethodSpending = mergedPaymentSpending,
            spendingInsights = insights,
            averageDailySpending = averageDailySpending,
            averageTransactionAmount = averageTransactionAmount,
            totalTransactions = expenseCount // Using expense count for now as per available data
        )
    }

    private fun computeMonthTotalData(
        transactions: List<Transaction>,
        monthStart: Long,
        displayCurrency: String
    ): List<ChartDataPoint> {
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        // This calculates a single bar for the whole month? 
        // Wait, "IncomeVsExpenseChart" usually expects multiple points?
        // UI says: "Monthly Activity" -> This usually means Daily breakdown?
        // Let's check previous implementation.
        // `computeMonthTotalData` previously returned listOf(ChartDataPoint(...)). ONE point.
        // BUT `ReportsScreen` calls `IncomeVsExpenseChart`.
        // If it's "Monthly Activity" and shows only 1 bar, that's weird.
        // Ah, looking at the previous code: `computeMonthlyChartData` (unused? NO, I saw `computeMonthTotalData` being used in buildUiState).
        
        // Wait, in previous code: 
        // val currentMonthData = computeMonthTotalData(monthTransactions, ...)
        // fun computeMonthTotalData(...) returns listOf(ChartDataPoint(...)) -> List of size 1.
        
        // The previous code had a function `computeMonthlyChartData` which did a while loop. BUT IT WAS NOT USED in `buildUiState`!
        // `buildUiState` used `computeMonthTotalData` (singular).
        // This means the chart likely only showed one bar in the old code too? Or maybe I misread.
        // "Monthly Activity" likely implies daily bars.
        // Let's try to give daily bars (Daily Breakdown) for "Monthly Activity".
        // And Monthly bars for "Yearly Overview".
        
        // Let's implement Daily Breakdown for Current Month.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = monthStart
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val dailyMap = transactions.groupBy { 
            val c = Calendar.getInstance()
            c.timeInMillis = it.date
            c.get(Calendar.DAY_OF_MONTH)
        }
        
        val points = mutableListOf<ChartDataPoint>()
        for (day in 1..maxDay) {
            val dayTxs = dailyMap[day] ?: emptyList()
            val income = dayTxs.filter { it.type == TransactionType.INCOME }
                .sumOf { currencyRepository.convert(it.amount, it.currency, displayCurrency) }
            val expense = dayTxs.filter { it.type == TransactionType.EXPENSE }
                .sumOf { currencyRepository.convert(it.amount, it.currency, displayCurrency) }
            
            // Only add points if there is data? or add all days?
            // "IncomeVsExpenseChart" uses BarChart.
            // If we have 30 bars, it might be crowded but workable.
            // Let's stick to what we think is best: Daily breakdown.
            if (income > 0 || expense > 0) {
                 points.add(ChartDataPoint(day.toString(), income, expense, 0L))
            }
        }
        // If empty, return a zero point or empty list
        if (points.isEmpty()) return emptyList()
        return points.sortedBy { it.label.toIntOrNull() ?: 0 }
    }

    private fun computeYearTotalData(
        transactions: List<Transaction>,
        yearStart: Long,
        displayCurrency: String
    ): List<ChartDataPoint> {
        // Yearly Overview -> Monthly breakdown
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        val monthlyMap = transactions.groupBy { 
            val c = Calendar.getInstance()
            c.timeInMillis = it.date
            c.get(Calendar.MONTH) // 0-11
        }
        
        val points = mutableListOf<ChartDataPoint>()
        for (month in 0..11) {
            val monthTxs = monthlyMap[month] ?: emptyList()
             val income = monthTxs.filter { it.type == TransactionType.INCOME }
                .sumOf { currencyRepository.convert(it.amount, it.currency, displayCurrency) }
            val expense = monthTxs.filter { it.type == TransactionType.EXPENSE }
                .sumOf { currencyRepository.convert(it.amount, it.currency, displayCurrency) }
            
            if (income > 0 || expense > 0) {
                // Get label
                val c = Calendar.getInstance()
                c.set(Calendar.MONTH, month)
                val label = monthFormat.format(c.time)
                points.add(ChartDataPoint(label, income, expense, c.timeInMillis))
            }
        }
        return points.sortedBy { it.timestamp }
    }
    
    // ... Helper functions ...
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

    private fun getMonthRange(offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, offset)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
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
        
        topCategory?.let {
            insights.add(
                SpendingInsight(
                    title = "Top Category",
                    value = it.categoryName,
                    description = "${currencyFormat.format(it.amount)} across ${it.transactionCount} transactions"
                )
            )
        }
        
        insights.add(
            SpendingInsight(
                title = "Daily Average",
                value = currencyFormat.format(averageDailySpending),
                description = "Average spending per day in this period"
            )
        )
        
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
