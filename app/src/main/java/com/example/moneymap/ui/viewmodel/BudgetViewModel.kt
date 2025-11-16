package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.Budget
import com.example.moneymap.data.model.BudgetPeriod
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.repository.BudgetRepository
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class BudgetUiModel(
    val budget: Budget,
    val category: Category?,
    val spentAmount: Double,
    val progress: Float,
    val daysRemaining: Int,
    val isOverBudget: Boolean
)

data class BudgetScreenState(
    val budgets: List<BudgetUiModel> = emptyList(),
    val isLoading: Boolean = true
)

data class BudgetFormState(
    val id: String? = null,
    val categoryId: String? = null,
    val amount: String = "",
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val startDate: Long = startOfCurrentMonth(),
    val endDate: Long = endOfCurrentMonth(),
    val alertThreshold: Float = 0.8f,
    val isActive: Boolean = true,
    val isVisible: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

private fun startOfCurrentMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun endOfCurrentMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val categoriesFlow = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val budgetsFlow = budgetRepository.getAllBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val transactionsFlow = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _screenState = MutableStateFlow(BudgetScreenState())
    val screenState: StateFlow<BudgetScreenState> = _screenState.asStateFlow()

    private val _formState = MutableStateFlow(BudgetFormState())
    val formState: StateFlow<BudgetFormState> = _formState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        observeBudgets()
    }

    private fun observeBudgets() {
        viewModelScope.launch {
            combine(budgetsFlow, categoriesFlow, transactionsFlow) { budgets, categories, transactions ->
                val models = budgets.map { budget ->
                    createBudgetUiModel(budget, categories, transactions)
                }.sortedBy { it.budget.startDate }
                BudgetScreenState(budgets = models, isLoading = false)
            }.collect { state ->
                _screenState.value = state
            }
        }
    }

    private fun createBudgetUiModel(
        budget: Budget,
        categories: List<Category>,
        transactions: List<Transaction>
    ): BudgetUiModel {
        val category = categories.firstOrNull { it.id == budget.categoryId }
        val spentAmount = transactions
            .filter { it.categoryId == budget.categoryId && it.date in budget.startDate..budget.endDate }
            .sumOf { it.amount }
        val progress = if (budget.amount <= 0.0) 0f else (spentAmount / budget.amount).toFloat()
        val isOverBudget = spentAmount >= budget.amount
        val daysRemaining = calculateDaysRemaining(budget.endDate)
        return BudgetUiModel(
            budget = budget,
            category = category,
            spentAmount = spentAmount,
            progress = progress.coerceAtMost(2f),
            daysRemaining = daysRemaining,
            isOverBudget = isOverBudget
        )
    }

    private fun calculateDaysRemaining(endDate: Long): Int {
        val today = startOfCurrentDay()
        return if (endDate < today) {
            0
        } else {
            ((endDate - today) / (1000 * 60 * 60 * 24)).toInt() + 1
        }
    }

    private fun startOfCurrentDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun openCreateBudget() {
        _formState.value = BudgetFormState(isVisible = true)
    }

    fun openEditBudget(budgetId: String) {
        val budget = budgetsFlow.value.firstOrNull { it.id == budgetId } ?: return
        _formState.value = BudgetFormState(
            id = budget.id,
            categoryId = budget.categoryId,
            amount = budget.amount.toString(),
            period = budget.period,
            startDate = budget.startDate,
            endDate = budget.endDate,
            alertThreshold = budget.alertThreshold,
            isActive = budget.isActive,
            isVisible = true
        )
    }

    fun dismissForm() {
        _formState.value = _formState.value.copy(isVisible = false, errorMessage = null, isSaving = false)
    }

    fun updateCategory(categoryId: String) {
        _formState.update { it.copy(categoryId = categoryId) }
    }

    fun updateAmount(amount: String) {
        val filtered = amount.filter { char -> char.isDigit() || char == '.' }
        _formState.update { it.copy(amount = filtered) }
    }

    fun updatePeriod(period: BudgetPeriod) {
        val start = if (period == BudgetPeriod.MONTHLY) startOfCurrentMonth() else _formState.value.startDate
        val end = if (period == BudgetPeriod.MONTHLY) endOfCurrentMonth() else _formState.value.endDate
        _formState.update { it.copy(period = period, startDate = start, endDate = end) }
    }

    fun updateStartDate(timestamp: Long) {
        _formState.update { state ->
            state.copy(startDate = timestamp)
        }
    }

    fun updateEndDate(timestamp: Long) {
        _formState.update { state ->
            state.copy(endDate = timestamp)
        }
    }

    fun updateAlertThreshold(threshold: Float) {
        _formState.update { it.copy(alertThreshold = threshold.coerceIn(0.1f, 1f)) }
    }

    fun updateActive(isActive: Boolean) {
        _formState.update { it.copy(isActive = isActive) }
    }

    fun saveBudget() {
        val state = _formState.value
        val amountValue = state.amount.toDoubleOrNull()
        when {
            state.categoryId.isNullOrBlank() -> showFormError("Please select a category")
            amountValue == null || amountValue <= 0.0 -> showFormError("Enter a valid amount")
            state.startDate > state.endDate -> showFormError("Start date must be before end date")
            else -> {
                viewModelScope.launch {
                    try {
                        _formState.update { it.copy(isSaving = true, errorMessage = null) }
                        val budget = Budget(
                            id = state.id ?: UUID.randomUUID().toString(),
                            categoryId = state.categoryId,
                            amount = amountValue,
                            period = state.period,
                            startDate = state.startDate,
                            endDate = state.endDate,
                            alertThreshold = state.alertThreshold,
                            isActive = state.isActive
                        )
                        if (state.id == null) {
                            budgetRepository.insertBudget(budget)
                            _snackbarMessages.emit("Budget created")
                        } else {
                            budgetRepository.updateBudget(budget)
                            _snackbarMessages.emit("Budget updated")
                        }
                        _formState.value = BudgetFormState(isVisible = false)
                    } catch (e: Exception) {
                        showFormError(e.message ?: "Failed to save budget")
                    }
                }
            }
        }
    }

    private fun showFormError(message: String) {
        _formState.update { it.copy(errorMessage = message, isSaving = false) }
    }

    fun deleteBudget(budgetId: String) {
        val budget = budgetsFlow.value.firstOrNull { it.id == budgetId } ?: return
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
            _snackbarMessages.emit("Budget deleted")
        }
    }

    fun deactivateBudget(budgetId: String) {
        viewModelScope.launch {
            budgetRepository.deactivateBudget(budgetId)
            _snackbarMessages.emit("Budget deactivated")
        }
    }

    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    val categories: StateFlow<List<Category>> get() = categoriesFlow
}
