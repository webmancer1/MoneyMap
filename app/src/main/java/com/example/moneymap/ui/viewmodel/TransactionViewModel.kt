package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.CategoryType
import com.example.moneymap.data.model.PaymentMethod
import com.example.moneymap.data.model.RecurringPattern
import com.example.moneymap.data.model.SyncStatus
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.data.preferences.SettingsRepository
import com.example.moneymap.data.repository.CategoryRepository
import com.example.moneymap.data.repository.CurrencyRepository
import com.example.moneymap.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import com.example.moneymap.notification.LocalNotificationService

private val initialFormState = TransactionFormState()

data class TransactionFormState(
    val id: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val currency: String = "KES",
    val categoryId: String? = null,
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val paymentMethod: PaymentMethod? = null,
    val tags: String = "",
    val receiptUrl: String? = null,
    val isRecurring: Boolean = false,
    val recurringPattern: RecurringPattern? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

data class TransactionListUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val currencyRepository: CurrencyRepository,
    private val localNotificationService: LocalNotificationService
) : ViewModel() {

    data class TransactionFilterState(
        val dateRange: Pair<Long, Long>? = null,
        val categoryId: String? = null,
        val minAmount: Double? = null,
        val maxAmount: Double? = null,
        val paymentMethod: PaymentMethod? = null,
        val transactionType: TransactionType? = null
    )

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactions = combine(
        _filterState,
        _searchQuery
    ) { filter, query ->
        Pair(filter, query)
    }.flatMapLatest { (filter, query) ->
        transactionRepository.getFilteredTransactions(
            query = query.takeIf { it.isNotBlank() },
            type = filter.transactionType,
            categoryId = filter.categoryId,
            paymentMethod = filter.paymentMethod,
            startDate = filter.dateRange?.first,
            endDate = filter.dateRange?.second,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )



    val hasAnyTransactions = transactionRepository.getAllTransactions()
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val categories = categoryRepository.getAllCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _formState = MutableStateFlow(initialFormState)
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    private val _listUiState = MutableStateFlow(TransactionListUiState())
    val listUiState: StateFlow<TransactionListUiState> = _listUiState.asStateFlow()

    private val _deleteEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteEvent = _deleteEvent.asSharedFlow()

    private val _undoDeleteEvent = MutableSharedFlow<Transaction>(extraBufferCapacity = 1)
    val undoDeleteEvent = _undoDeleteEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private var recentlyDeletedTransaction: Transaction? = null

    val displayCurrency: StateFlow<String> = settingsRepository.settingsFlow
        .map { it.currency }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "KES"
        )

    init {
        // Warm up FX rates in the background
        viewModelScope.launch {
            currencyRepository.refreshRatesIfStale()
        }
    }

    fun convertAmount(amount: Double, fromCurrency: String): Double {
        val targetCurrency: String = displayCurrency.value
        return currencyRepository.convert(amount, fromCurrency, targetCurrency)
    }

    fun updateAmount(amount: String) {
        _formState.update { it.copy(amount = amount.filter { char -> char.isDigit() || char == '.' }) }
    }

    fun updateTransactionType(type: TransactionType) {
        _formState.update { it.copy(type = type, categoryId = null) }
    }

    fun updateCategory(categoryId: String) {
        _formState.update { it.copy(categoryId = categoryId) }
    }

    fun updateDate(timestamp: Long) {
        _formState.update { it.copy(date = timestamp) }
    }

    fun updateNotes(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethod?) {
        _formState.update { it.copy(paymentMethod = paymentMethod) }
    }

    fun updateTags(tags: String) {
        _formState.update { it.copy(tags = tags) }
    }

    fun updateRecurring(isRecurring: Boolean) {
        _formState.update {
            it.copy(
                isRecurring = isRecurring,
                recurringPattern = if (isRecurring) it.recurringPattern else null
            )
        }
    }

    fun updateRecurringPattern(pattern: RecurringPattern?) {
        _formState.update { it.copy(recurringPattern = pattern) }
    }

    fun clearFormEvent() {
        _formState.update { initialFormState }
    }

    fun clearSuccessFlag() {
        _formState.update { it.copy(isSuccess = false, errorMessage = null, isSaving = false) }
    }

    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(transactionId) ?: return@launch
            _formState.update {
                it.copy(
                    id = transaction.id,
                    type = transaction.type,
                    amount = transaction.amount.toString(),
                    currency = transaction.currency,
                    categoryId = transaction.categoryId,
                    date = transaction.date,
                    notes = transaction.notes.orEmpty(),
                    paymentMethod = transaction.paymentMethod,
                    tags = transaction.tags.joinToString(", "),
                    receiptUrl = transaction.receiptUrl,
                    isRecurring = transaction.isRecurring,
                    recurringPattern = transaction.recurringPattern,
                    isSaving = false,
                    errorMessage = null,
                    isSuccess = false
                )
            }
        }
    }

    fun saveTransaction() {
        val currentState = _formState.value
        val amountValue = currentState.amount.toDoubleOrNull()
        when {
            currentState.categoryId.isNullOrBlank() -> {
                showFormError("Please select a category")
            }
            amountValue == null || amountValue <= 0.0 -> {
                showFormError("Enter a valid amount")
            }
            else -> {
                viewModelScope.launch {
                    try {
                        _formState.update { it.copy(isSaving = true, errorMessage = null) }
                        val existing = currentState.id?.let { transactionRepository.getTransactionById(it) }
                        val transaction = buildTransactionFromFormState(currentState, amountValue, existing)
                        if (currentState.id == null) {
                            transactionRepository.insertTransaction(transaction)
                        } else {
                            transactionRepository.updateTransaction(transaction)
                        }
                        _formState.update { it.copy(isSaving = false, isSuccess = true) }
                        _snackbarMessage.emit("Transaction saved successfully")
                        localNotificationService.showTransactionNotification(
                            "Transaction Added",
                            "Your ${currentState.type.name.lowercase()} of ${transaction.currency} ${transaction.amount} has been recorded."
                        )
                    } catch (e: Exception) {
                        showFormError(e.message ?: "Failed to save transaction")
                    }
                }
            }
        }
    }

    private fun buildTransactionFromFormState(
        state: TransactionFormState,
        amountValue: Double,
        existingTransaction: Transaction?
    ): Transaction {
        val tagsList = state.tags.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val now = System.currentTimeMillis()

        return if (state.id == null || existingTransaction == null) {
            Transaction(
                id = state.id ?: UUID.randomUUID().toString(),
                type = state.type,
                amount = amountValue,
                currency = state.currency,
                categoryId = state.categoryId!!,
                date = state.date,
                notes = state.notes.ifBlank { null },
                paymentMethod = state.paymentMethod,
                tags = tagsList,
                receiptUrl = state.receiptUrl,
                isRecurring = state.isRecurring,
                recurringPattern = state.recurringPattern,
                createdAt = existingTransaction?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        } else {
            existingTransaction.copy(
                type = state.type,
                amount = amountValue,
                currency = state.currency,
                categoryId = state.categoryId!!,
                date = state.date,
                notes = state.notes.ifBlank { null },
                paymentMethod = state.paymentMethod,
                tags = tagsList,
                receiptUrl = state.receiptUrl,
                isRecurring = state.isRecurring,
                recurringPattern = state.recurringPattern,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            recentlyDeletedTransaction = transaction
            transactionRepository.deleteTransaction(transaction)
            _deleteEvent.emit(Unit)
        }
    }

    fun restoreDeletedTransaction() {
        viewModelScope.launch {
            val transaction = recentlyDeletedTransaction ?: return@launch
            transactionRepository.insertTransaction(transaction)
            _undoDeleteEvent.emit(transaction)
            recentlyDeletedTransaction = null
        }
    }

    fun clearDeleteState() {
        recentlyDeletedTransaction = null
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }

    private fun showFormError(message: String) {
        _formState.update { it.copy(isSaving = false, errorMessage = message, isSuccess = false) }
    }

    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(timestamp)
    }

    fun getCategoriesForType(type: TransactionType, allCategories: List<Category>): List<Category> {
        val categoryType = when (type) {
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.EXPENSE -> CategoryType.EXPENSE
        }
        return allCategories.filter { it.type == categoryType }
    }

    fun updateFilter(
        dateRange: Pair<Long, Long>? = _filterState.value.dateRange,
        categoryId: String? = _filterState.value.categoryId,
        minAmount: Double? = _filterState.value.minAmount,
        maxAmount: Double? = _filterState.value.maxAmount,
        paymentMethod: PaymentMethod? = _filterState.value.paymentMethod,
        transactionType: TransactionType? = _filterState.value.transactionType
    ) {
        _filterState.update {
            it.copy(
                dateRange = dateRange,
                categoryId = categoryId,
                minAmount = minAmount,
                maxAmount = maxAmount,
                paymentMethod = paymentMethod,
                transactionType = transactionType
            )
        }
    }

    fun clearFilters() {
        _filterState.update { TransactionFilterState() }
        _searchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNewCategory(name: String, type: CategoryType, color: String) {
        viewModelScope.launch {
            try {
                val newCategory = Category(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    color = color,
                    icon = "Category",
                    isDefault = false,
                    isActive = true
                )
                categoryRepository.insertCategory(newCategory)
                _formState.update { it.copy(categoryId = newCategory.id) } // Auto-select new category
                _snackbarMessage.emit("Category added")
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to add category: ${e.message}")
            }
        }
    }
}
