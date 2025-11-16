package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.repository.CategoryRepository
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
    val amount: Double
)

data class MonthlyIncomeExpense(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val spendingByCategory: List<CategorySpending> = emptyList(),
    val monthlyIncomeExpense: List<MonthlyIncomeExpense> = emptyList()
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
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
                selectedPeriod
            ) { transactions, categories, period ->
                buildUiState(transactions, categories, period)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun buildUiState(
        transactions: List<Transaction>,
        categories: List<Category>,
        period: ReportPeriod
    ): ReportsUiState {
        val (startDate, endDate) = calculatePeriodRange(period)
        val filtered = transactions.filter { it.date in startDate..endDate }

        val totalIncome = filtered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = filtered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val spendingByCategory = filtered
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { transaction ->
                categories.firstOrNull { it.id == transaction.categoryId }?.name ?: "Uncategorized"
            }
            .map { (categoryName, categoryTransactions) ->
                CategorySpending(
                    categoryName = categoryName,
                    amount = categoryTransactions.sumOf { it.amount }
                )
            }
            .sortedByDescending { it.amount }

        val monthlyIncomeExpense = computeMonthlyIncomeExpense(filtered, period)

        return ReportsUiState(
            selectedPeriod = period,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            spendingByCategory = spendingByCategory,
            monthlyIncomeExpense = monthlyIncomeExpense
        )
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
        period: ReportPeriod
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
                .sumOf { it.amount }
            val expense = monthlyTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val label = monthFormat.format(Date(start))
            result.add(MonthlyIncomeExpense(label, income, expense))
        }

        return result
    }
}
