package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.TransactionRepository
import com.example.moneymap.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentTransactions = transactionRepository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        initializeCategories()
        loadMonthlySummary()
        schedulePeriodicSync()
    }

    private fun schedulePeriodicSync() {
        viewModelScope.launch {
            syncManager.schedulePeriodicSync()
        }
    }

    private fun initializeCategories() {
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()
        }
    }

    private fun loadMonthlySummary() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startDate = calendar.timeInMillis

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endDate = calendar.timeInMillis

            val income = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.INCOME,
                startDate,
                endDate
            )
            val expenses = transactionRepository.getTotalAmountByTypeAndDateRange(
                TransactionType.EXPENSE,
                startDate,
                endDate
            )

            _uiState.value = _uiState.value.copy(
                totalIncome = income,
                totalExpenses = expenses,
                balance = income - expenses
            )
        }
    }
}

