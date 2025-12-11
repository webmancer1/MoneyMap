package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.preferences.SettingsRepository
import com.example.moneymap.data.repository.AuthRepository
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.CurrencyRepository
import com.example.moneymap.data.repository.TransactionRepository
import com.example.moneymap.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val currencyCode: String = "KES",
    val isLoading: Boolean = false,
    val userName: String = "",
    val greeting: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val syncManager: SyncManager,
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentTransactions = transactionRepository.getAllTransactions()
        .map { list -> list.sortedByDescending { it.date } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        initializeCategories()
        loadUserProfile()
        observeMonthlySummary()
        schedulePeriodicSync()
        // Warm up FX rates in the background so summaries can use real-time values.
        viewModelScope.launch {
            currencyRepository.refreshRatesIfStale()
        }
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

    private fun loadUserProfile() {
        val user = authRepository.currentUser
        val displayName = user?.displayName ?: "User"
        val firstName = displayName.split(" ").firstOrNull() ?: displayName
        
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }

        _uiState.update { it.copy(userName = firstName, greeting = greeting) }
    }

    private fun observeMonthlySummary() {
        val (startDate, endDate) = currentMonthRange()
        transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .combine(settingsRepository.settingsFlow.map { it.currency }.distinctUntilChanged()) { transactions, currency ->
                Pair(transactions, currency)
            }
            .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
            .onEach { (transactions, displayCurrency) ->
                val income = transactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { tx ->
                        currencyRepository.convert(tx.amount, tx.currency, displayCurrency)
                    }
                val expenses = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { tx ->
                        currencyRepository.convert(tx.amount, tx.currency, displayCurrency)
                    }

                _uiState.value = _uiState.value.copy(
                    totalIncome = income,
                    totalExpenses = expenses,
                    balance = income - expenses,
                    currencyCode = displayCurrency,
                    isLoading = false
                )
            }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)
    }

    private fun currentMonthRange(): Pair<Long, Long> {
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
        return startDate to endDate
    }

    fun convertAmount(amount: Double, fromCurrency: String, toCurrency: String): Double {
        return currencyRepository.convert(amount, fromCurrency, toCurrency)
    }
}

