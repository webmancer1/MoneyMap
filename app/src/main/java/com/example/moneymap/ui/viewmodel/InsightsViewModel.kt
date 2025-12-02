package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class InsightSpendingInsight(
    val title: String,
    val message: String,
    val type: InsightType,
    val category: String? = null,
    val amount: Double? = null,
    val percentage: Double? = null
)

enum class InsightType {
    WARNING,
    INFO,
    SUCCESS,
    SUGGESTION
}

data class InsightCategorySpending(
    val categoryId: String,
    val categoryName: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentage: Double
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val transactions = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getInsights(): List<InsightSpendingInsight> {
        val allTransactions = transactions.value
        val allCategories = categories.value
        val insights = mutableListOf<InsightSpendingInsight>()

        if (allTransactions.isEmpty()) {
            insights.add(
                InsightSpendingInsight(
                    title = "No Data Yet",
                    message = "Start adding transactions to get personalized insights!",
                    type = InsightType.INFO
                )
            )
            return insights
        }

        val expenseTransactions = allTransactions.filter { it.type == TransactionType.EXPENSE }
        val incomeTransactions = allTransactions.filter { it.type == TransactionType.INCOME }

        if (expenseTransactions.isEmpty()) {
            insights.add(
                InsightSpendingInsight(
                    title = "Great Start!",
                    message = "You haven't recorded any expenses yet. Keep tracking to see insights!",
                    type = InsightType.SUCCESS
                )
            )
            return insights
        }

        // Monthly Analysis
        val monthlyInsights = getMonthlyInsights(expenseTransactions, incomeTransactions)
        insights.addAll(monthlyInsights)

        // Category Analysis
        val categoryInsights = getCategoryInsights(expenseTransactions, allCategories)
        insights.addAll(categoryInsights)

        // Spending Trends
        val trendInsights = getSpendingTrendInsights(expenseTransactions)
        insights.addAll(trendInsights)

        // Budget Warnings
        val budgetWarnings = getBudgetWarnings(expenseTransactions, incomeTransactions)
        insights.addAll(budgetWarnings)

        return insights
    }

    fun getTopSpendingCategories(limit: Int = 5): List<InsightCategorySpending> {
        val expenseTransactions = transactions.value.filter { it.type == TransactionType.EXPENSE }
        val allCategories = categories.value
        val totalExpenses = expenseTransactions.sumOf { it.amount }

        if (totalExpenses == 0.0) return emptyList()

        val categoryMap = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) ->
                transactions.sumOf { it.amount }
            }

        return categoryMap.mapNotNull { (categoryId, totalAmount) ->
            val category = allCategories.firstOrNull { it.id == categoryId }
            val transactionCount = expenseTransactions.count { it.categoryId == categoryId }
            val percentage = (totalAmount / totalExpenses) * 100

            InsightCategorySpending(
                categoryId = categoryId,
                categoryName = category?.name ?: "Unknown",
                totalAmount = totalAmount,
                transactionCount = transactionCount,
                percentage = percentage
            )
        }.sortedByDescending { it.totalAmount }.take(limit)
    }

    private fun getMonthlyInsights(
        expenseTransactions: List<Transaction>,
        incomeTransactions: List<Transaction>
    ): List<InsightSpendingInsight> {
        val insights = mutableListOf<InsightSpendingInsight>()
        val calendar = Calendar.getInstance()

        // Current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val currentMonthStart = calendar.timeInMillis

        val currentMonthExpenses = expenseTransactions
            .filter { it.date >= currentMonthStart }
            .sumOf { it.amount }

        val currentMonthIncome = incomeTransactions
            .filter { it.date >= currentMonthStart }
            .sumOf { it.amount }

        // Previous month
        calendar.add(Calendar.MONTH, -1)
        val previousMonthStart = calendar.timeInMillis
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val previousMonthEnd = calendar.timeInMillis

        val previousMonthExpenses = expenseTransactions
            .filter { it.date >= previousMonthStart && it.date <= previousMonthEnd }
            .sumOf { it.amount }

        if (previousMonthExpenses > 0) {
            val change = ((currentMonthExpenses - previousMonthExpenses) / previousMonthExpenses) * 100
            if (change > 20) {
                insights.add(
                    InsightSpendingInsight(
                        title = "Spending Increase",
                        message = "Your spending increased by ${String.format("%.1f", change)}% compared to last month.",
                        type = InsightType.WARNING,
                        percentage = change
                    )
                )
            } else if (change < -20) {
                insights.add(
                    InsightSpendingInsight(
                        title = "Spending Decrease",
                        message = "Great! Your spending decreased by ${String.format("%.1f", -change)}% compared to last month.",
                        type = InsightType.SUCCESS,
                        percentage = -change
                    )
                )
            }
        }

        // Income vs Expenses
        if (currentMonthIncome > 0) {
            val expenseRatio = (currentMonthExpenses / currentMonthIncome) * 100
            if (expenseRatio > 90) {
                insights.add(
                    InsightSpendingInsight(
                        title = "High Spending Ratio",
                        message = "You're spending ${String.format("%.1f", expenseRatio)}% of your income. Consider saving more.",
                        type = InsightType.WARNING,
                        percentage = expenseRatio
                    )
                )
            } else if (expenseRatio < 50) {
                insights.add(
                    InsightSpendingInsight(
                        title = "Great Savings Rate",
                        message = "You're spending only ${String.format("%.1f", expenseRatio)}% of your income. Keep it up!",
                        type = InsightType.SUCCESS,
                        percentage = expenseRatio
                    )
                )
            }
        }

        return insights
    }

    private fun getCategoryInsights(
        expenseTransactions: List<Transaction>,
        allCategories: List<com.example.moneymap.data.model.Category>
    ): List<InsightSpendingInsight> {
        val insights = mutableListOf<InsightSpendingInsight>()
        val totalExpenses = expenseTransactions.sumOf { it.amount }

        if (totalExpenses == 0.0) return insights

        val categorySpending = expenseTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) ->
                transactions.sumOf { it.amount }
            }

        categorySpending.forEach { (categoryId, amount) ->
            val category = allCategories.firstOrNull { it.id == categoryId }
            val percentage = (amount / totalExpenses) * 100

            if (percentage > 40) {
                insights.add(
                    InsightSpendingInsight(
                        title = "High Spending Category",
                        message = "${category?.name ?: "This category"} accounts for ${String.format("%.1f", percentage)}% of your expenses.",
                        type = InsightType.WARNING,
                        category = category?.name,
                        amount = amount,
                        percentage = percentage
                    )
                )
            }
        }

        return insights
    }

    private fun getSpendingTrendInsights(expenseTransactions: List<Transaction>): List<InsightSpendingInsight> {
        val insights = mutableListOf<InsightSpendingInsight>()

        // Daily average spending
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val monthStart = calendar.timeInMillis

        val currentMonthExpenses = expenseTransactions
            .filter { it.date >= monthStart }
            .sumOf { it.amount }

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val dailyAverage = if (currentDay > 0) currentMonthExpenses / currentDay else 0.0

        val projectedMonthly = dailyAverage * daysInMonth

        if (projectedMonthly > currentMonthExpenses * 1.2) {
            insights.add(
                InsightSpendingInsight(
                    title = "Spending Pace Alert",
                    message = "At your current pace, you'll spend ${String.format("%.0f", projectedMonthly)} this month. Consider reducing expenses.",
                    type = InsightType.WARNING,
                    amount = projectedMonthly
                )
            )
        }

        return insights
    }

    private fun getBudgetWarnings(
        expenseTransactions: List<Transaction>,
        incomeTransactions: List<Transaction>
    ): List<InsightSpendingInsight> {
        val insights = mutableListOf<InsightSpendingInsight>()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val monthStart = calendar.timeInMillis

        val monthlyExpenses = expenseTransactions
            .filter { it.date >= monthStart }
            .sumOf { it.amount }

        val monthlyIncome = incomeTransactions
            .filter { it.date >= monthStart }
            .sumOf { it.amount }

        if (monthlyExpenses > monthlyIncome && monthlyIncome > 0) {
            insights.add(
                InsightSpendingInsight(
                    title = "Overspending Alert",
                    message = "You're spending more than you earn this month. Review your expenses.",
                    type = InsightType.WARNING,
                    amount = monthlyExpenses - monthlyIncome
                )
            )
        }

        return insights
    }

    fun getMonthlyExpenseTotal(): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val monthStart = calendar.timeInMillis

        return transactions.value
            .filter { it.type == TransactionType.EXPENSE && it.date >= monthStart }
            .sumOf { it.amount }
    }

    fun getMonthlyIncomeTotal(): Double {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val monthStart = calendar.timeInMillis

        return transactions.value
            .filter { it.type == TransactionType.INCOME && it.date >= monthStart }
            .sumOf { it.amount }
    }
}

