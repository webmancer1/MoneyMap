package com.example.moneymap.ui.screen.transaction

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.DateRange as DateRangeOutlined
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.data.model.PaymentMethod
import com.example.moneymap.data.model.RecurringPattern
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.ui.viewmodel.TransactionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionId: String? = null,
    onNavigateBack: () -> Unit,
    onTransactionSaved: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransaction(transactionId)
        } else {
            viewModel.clearFormEvent()
        }
    }

    LaunchedEffect(formState.isSuccess) {
        if (formState.isSuccess) {
            onTransactionSaved()
            viewModel.clearFormEvent()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(formState.errorMessage) {
        formState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearSuccessFlag()
        }
    }

    var isCategoryMenuExpanded by remember { mutableStateOf(false) }
    var isPaymentMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.date)

    val categoriesForType = remember(formState.type, allCategories) {
        viewModel.getCategoriesForType(formState.type, allCategories)
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis ?: formState.date
                        viewModel.updateDate(selectedDate)
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId != null) "Edit Transaction" else "Add Transaction") },
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
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formState.amount,
                        onValueChange = viewModel::updateAmount,
                        placeholder = { Text("0.00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Text(
                text = "Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = formState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.updateTransactionType(TransactionType.EXPENSE) },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = formState.type == TransactionType.INCOME,
                    onClick = { viewModel.updateTransactionType(TransactionType.INCOME) },
                    label = { Text("Income") }
                )
            }

            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ExposedDropdownMenuBox(
                expanded = isCategoryMenuExpanded,
                onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded }
            ) {
                OutlinedTextField(
                    value = categoriesForType.firstOrNull { it.id == formState.categoryId }?.name ?: "Select category",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded)
                    },
                    placeholder = { Text("Select category") }
                )
                ExposedDropdownMenu(
                    expanded = isCategoryMenuExpanded,
                    onDismissRequest = { isCategoryMenuExpanded = false }
                ) {
                    if (categoriesForType.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No categories available") },
                            onClick = { isCategoryMenuExpanded = false }
                        )
                    } else {
                        categoriesForType.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    viewModel.updateCategory(category.id)
                                    isCategoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.formatDate(formState.date))
                }
            }

            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                singleLine = false,
                minLines = 2,
                maxLines = 4
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = isPaymentMenuExpanded,
                    onExpandedChange = { isPaymentMenuExpanded = !isPaymentMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = formState.paymentMethod?.name?.replace("_", " ") ?: "Select payment method",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentMenuExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = isPaymentMenuExpanded,
                        onDismissRequest = { isPaymentMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                viewModel.updatePaymentMethod(null)
                                isPaymentMenuExpanded = false
                            }
                        )
                        PaymentMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.name.replace("_", " ")) },
                                onClick = {
                                    viewModel.updatePaymentMethod(method)
                                    isPaymentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = formState.tags,
                onValueChange = viewModel::updateTags,
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recurring",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilterChip(
                        selected = formState.isRecurring,
                        onClick = { viewModel.updateRecurring(!formState.isRecurring) },
                        label = { Text(if (formState.isRecurring) "Yes" else "No") },
                        leadingIcon = {
                            Icon(Icons.Default.Done, contentDescription = null)
                        }
                    )
                }

                if (formState.isRecurring) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RecurringPattern.values().forEach { pattern ->
                            AssistChip(
                                onClick = { viewModel.updateRecurringPattern(pattern) },
                                label = {
                                    Text(
                                        pattern.name.lowercase(Locale.getDefault())
                                            .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                                    )
                                },
                                leadingIcon = if (formState.recurringPattern == pattern) {
                                    { Icon(Icons.Default.Done, contentDescription = null) }
                                } else null
                            )
                        }
                    }
                }
            }

            Divider()

            OutlinedButton(onClick = { /* TODO: integrate receipt upload */ }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Receipt (Coming Soon)")
            }

            Button(
                onClick = { viewModel.saveTransaction() },
                enabled = !formState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (formState.isSaving) "Saving..." else "Save Transaction")
            }
        }
    }
}

