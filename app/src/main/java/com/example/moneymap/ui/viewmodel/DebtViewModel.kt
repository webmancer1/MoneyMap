package com.example.moneymap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.DebtType
import com.example.moneymap.data.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private val initialFormState = DebtFormState()

data class DebtFormState(
    val id: String? = null,
    val type: DebtType = DebtType.PAYABLE,
    val personName: String = "",
    val totalAmount: String = "",
    val paidAmount: String = "0",
    val dueDate: Long? = null,
    val notes: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val debtRepository: DebtRepository
) : ViewModel() {

    val debts = debtRepository.getAllDebts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _formState = MutableStateFlow(initialFormState)
    val formState: StateFlow<DebtFormState> = _formState.asStateFlow()

    private val _deleteEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleteEvent = _deleteEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private var recentlyDeletedDebt: Debt? = null

    fun updateDebtType(type: DebtType) {
        _formState.update { it.copy(type = type) }
    }

    fun updatePersonName(name: String) {
        _formState.update { it.copy(personName = name) }
    }

    fun updateTotalAmount(amount: String) {
        _formState.update { it.copy(totalAmount = amount.filter { char -> char.isDigit() || char == '.' }) }
    }

    fun updatePaidAmount(amount: String) {
        _formState.update { it.copy(paidAmount = amount.filter { char -> char.isDigit() || char == '.' }) }
    }

    fun updateDueDate(timestamp: Long?) {
        _formState.update { it.copy(dueDate = timestamp) }
    }

    fun updateNotes(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    fun clearFormEvent() {
        _formState.update { initialFormState }
    }

    fun clearSuccessFlag() {
        _formState.update { it.copy(isSuccess = false, errorMessage = null, isSaving = false) }
    }

    fun loadDebt(debtId: String) {
        viewModelScope.launch {
            val debt = debtRepository.getDebtById(debtId) ?: return@launch
            _formState.update {
                it.copy(
                    id = debt.id,
                    type = debt.type,
                    personName = debt.personName,
                    totalAmount = debt.totalAmount.toString(),
                    paidAmount = debt.paidAmount.toString(),
                    dueDate = debt.dueDate,
                    notes = debt.notes.orEmpty(),
                    isSaving = false,
                    errorMessage = null,
                    isSuccess = false
                )
            }
        }
    }

    fun saveDebt() {
        val currentState = _formState.value
        val totalAmountValue = currentState.totalAmount.toDoubleOrNull()
        val paidAmountValue = currentState.paidAmount.toDoubleOrNull() ?: 0.0

        when {
            currentState.personName.isBlank() -> {
                showFormError("Please enter a person name")
            }
            totalAmountValue == null || totalAmountValue <= 0.0 -> {
                showFormError("Enter a valid total amount")
            }
            paidAmountValue < 0.0 || paidAmountValue > totalAmountValue -> {
                showFormError("Paid amount cannot be negative or greater than total amount")
            }
            else -> {
                viewModelScope.launch {
                    try {
                        _formState.update { it.copy(isSaving = true, errorMessage = null) }
                        val existing = currentState.id?.let { debtRepository.getDebtById(it) }
                        val debt = buildDebtFromFormState(currentState, totalAmountValue, paidAmountValue, existing)
                        if (currentState.id == null) {
                            debtRepository.insertDebt(debt)
                        } else {
                            debtRepository.updateDebt(debt)
                        }
                        _formState.update { it.copy(isSaving = false, isSuccess = true) }
                        _snackbarMessage.emit("Debt saved successfully")
                    } catch (e: Exception) {
                        showFormError(e.message ?: "Failed to save debt")
                    }
                }
            }
        }
    }

    private fun buildDebtFromFormState(
        state: DebtFormState,
        totalAmountValue: Double,
        paidAmountValue: Double,
        existingDebt: Debt?
    ): Debt {
        return if (state.id == null || existingDebt == null) {
            Debt(
                id = state.id ?: UUID.randomUUID().toString(),
                type = state.type,
                personName = state.personName.trim(),
                totalAmount = totalAmountValue,
                paidAmount = paidAmountValue,
                dueDate = state.dueDate,
                notes = state.notes.ifBlank { null },
                createdAt = System.currentTimeMillis()
            )
        } else {
            existingDebt.copy(
                type = state.type,
                personName = state.personName.trim(),
                totalAmount = totalAmountValue,
                paidAmount = paidAmountValue,
                dueDate = state.dueDate,
                notes = state.notes.ifBlank { null }
            )
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            recentlyDeletedDebt = debt
            debtRepository.deleteDebt(debt)
            _deleteEvent.emit(Unit)
        }
    }

    fun restoreDeletedDebt() {
        viewModelScope.launch {
            val debt = recentlyDeletedDebt ?: return@launch
            debtRepository.insertDebt(debt)
            _snackbarMessage.emit("Debt restored")
            recentlyDeletedDebt = null
        }
    }

    fun clearDeleteState() {
        recentlyDeletedDebt = null
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }

    private fun showFormError(message: String) {
        _formState.update { it.copy(isSaving = false, errorMessage = message, isSuccess = false) }
    }

    fun getTotalPayable(): Double {
        return debts.value
            .filter { it.type == DebtType.PAYABLE }
            .sumOf { it.totalAmount - it.paidAmount }
    }

    fun getTotalReceivable(): Double {
        return debts.value
            .filter { it.type == DebtType.RECEIVABLE }
            .sumOf { it.totalAmount - it.paidAmount }
    }
}

