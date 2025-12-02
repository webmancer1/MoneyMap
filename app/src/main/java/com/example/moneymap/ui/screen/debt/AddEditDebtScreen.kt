package com.example.moneymap.ui.screen.debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.data.model.DebtType
import com.example.moneymap.ui.viewmodel.DebtViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtScreen(
    debtId: String? = null,
    onNavigateBack: () -> Unit,
    onDebtSaved: () -> Unit,
    viewModel: DebtViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = formState.dueDate
    )

    LaunchedEffect(debtId) {
        if (debtId != null) {
            viewModel.loadDebt(debtId)
        } else {
            viewModel.clearFormEvent()
        }
    }

    LaunchedEffect(formState.isSuccess) {
        if (formState.isSuccess) {
            onDebtSaved()
            viewModel.clearFormEvent()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (debtId == null) "Add Debt" else "Edit Debt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Debt Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Debt Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = formState.type == DebtType.PAYABLE,
                            onClick = { viewModel.updateDebtType(DebtType.PAYABLE) },
                            label = { Text("You Owe") }
                        )
                        FilterChip(
                            selected = formState.type == DebtType.RECEIVABLE,
                            onClick = { viewModel.updateDebtType(DebtType.RECEIVABLE) },
                            label = { Text("You're Owed") }
                        )
                    }
                }
            }

            // Person Name
            OutlinedTextField(
                value = formState.personName,
                onValueChange = { viewModel.updatePersonName(it) },
                label = { Text("Person/Entity Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Total Amount
            OutlinedTextField(
                value = formState.totalAmount,
                onValueChange = { viewModel.updateTotalAmount(it) },
                label = { Text("Total Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Paid Amount
            OutlinedTextField(
                value = formState.paidAmount,
                onValueChange = { viewModel.updatePaidAmount(it) },
                label = { Text("Paid Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.dueDate?.let {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                    } ?: "",
                    onValueChange = {},
                    label = { Text("Due Date (Optional)") },
                    modifier = Modifier.weight(1f),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    }
                )
                if (formState.dueDate != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { viewModel.updateDueDate(null) }) {
                        Text("Clear")
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = formState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("Notes (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 4
            )

            // Error Message
            formState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveDebt() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isSaving
            ) {
                Text(if (formState.isSaving) "Saving..." else "Save Debt")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.updateDueDate(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

