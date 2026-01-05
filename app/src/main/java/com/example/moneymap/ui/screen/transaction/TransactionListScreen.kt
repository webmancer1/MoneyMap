package com.example.moneymap.ui.screen.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.widget.DatePicker
import com.example.moneymap.data.model.PaymentMethod
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.data.model.Category
import com.example.moneymap.data.model.Transaction
import com.example.moneymap.data.model.TransactionType
import com.example.moneymap.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val displayCurrency by viewModel.displayCurrency.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val filterState by viewModel.filterState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDeleteDialogFor by remember { mutableStateOf<Transaction?>(null) }
    
    val currencyFormat = remember(displayCurrency) { 
        val locale = Locale.getDefault()
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = java.util.Currency.getInstance(displayCurrency)
        }
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect {
            val result = snackbarHostState.showSnackbar(
                message = "Transaction deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreDeletedTransaction()
            } else {
                viewModel.clearDeleteState()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.undoDeleteEvent.collect {
            snackbarHostState.showSnackbar("Transaction restored")
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
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.List, contentDescription = "Filter")
                    }
                    IconButton(onClick = onNavigateToAddTransaction) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterState.transactionType == null,
                    onClick = { viewModel.updateFilter(transactionType = null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filterState.transactionType == TransactionType.INCOME,
                    onClick = {
                        val newType = if (filterState.transactionType == TransactionType.INCOME) null else TransactionType.INCOME
                        viewModel.updateFilter(transactionType = newType)
                    },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = filterState.transactionType == TransactionType.EXPENSE,
                    onClick = {
                        val newType = if (filterState.transactionType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                        viewModel.updateFilter(transactionType = newType)
                    },
                    label = { Text("Expense") }
                )
            }
            
            val hasAnyTransactions by viewModel.hasAnyTransactions.collectAsState()

            if (transactions.isEmpty()) {
                EmptyTransactionsState(
                    hasTransactions = hasAnyTransactions,
                    onAddTransaction = onNavigateToAddTransaction
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactions, key = { it.id }) { transaction ->
                        val category = categories.firstOrNull { it.id == transaction.categoryId }
                        TransactionListItem(
                            transaction = transaction,
                            category = category,
                            displayCurrency = displayCurrency,
                            convertAmount = { amount, fromCurrency ->
                                viewModel.convertAmount(amount, fromCurrency)
                            },
                            currencyFormat = currencyFormat,
                            dateFormat = dateFormat,
                            onEdit = { onNavigateToEditTransaction(transaction.id) },
                            onDelete = { showDeleteDialogFor = transaction }
                        )
                    }
                }
            }
        }
    }

    showDeleteDialogFor?.let { transaction ->
        DeleteTransactionDialog(
            onDismiss = { showDeleteDialogFor = null },
            onConfirm = {
                showDeleteDialogFor = null
                coroutineScope.launch { viewModel.deleteTransaction(transaction) }
            }
        )
    }

    if (showFilterSheet) {
        FilterTransactionSheet(
            onDismiss = { showFilterSheet = false },
            viewModel = viewModel,
            categories = categories
        )
    }
}

@Composable
private fun EmptyTransactionsState(
    hasTransactions: Boolean,
    onAddTransaction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hasTransactions) {
                    "No transactions match your filter"
                } else {
                    "No transactions yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!hasTransactions) {
                Button(onClick = onAddTransaction) {
                    Text("Add Transaction")
                }
            }
        }
    }
}

@Composable
private fun TransactionListItem(
    transaction: Transaction,
    category: Category?,
    displayCurrency: String,
    convertAmount: (Double, String) -> Double,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val convertedAmount = remember(transaction.amount, transaction.currency, displayCurrency) {
        convertAmount(transaction.amount, transaction.currency)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category?.name ?: "Unknown Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateFormat.format(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = buildString {
                            append(if (transaction.type == TransactionType.INCOME) "+" else "-")
                            append(currencyFormat.format(convertedAmount))
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == TransactionType.INCOME) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    if (transaction.currency != currencyFormat.currency.currencyCode) {
                        Text(
                            text = "${transaction.currency} ${String.format("%.2f", transaction.amount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            transaction.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            transaction.paymentMethod?.let { method ->
                Text(
                    text = "Payment: ${method.name.replace("_", " ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (transaction.tags.isNotEmpty()) {
                Text(
                    text = "Tags: ${transaction.tags.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Transaction")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Transaction") },
        text = {
            Text("Are you sure you want to delete this transaction? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTransactionSheet(
    onDismiss: () -> Unit,
    viewModel: TransactionViewModel,
    categories: List<Category>
) {
    val filterState by viewModel.filterState.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current
    
    var minAmount by remember { mutableStateOf(filterState.minAmount?.toString() ?: "") }
    var maxAmount by remember { mutableStateOf(filterState.maxAmount?.toString() ?: "") }
    
    val startDateCalendar = Calendar.getInstance()
    val endDateCalendar = Calendar.getInstance()
    
    val startDatePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val start = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.timeInMillis
            val end = filterState.dateRange?.second ?: start
            viewModel.updateFilter(dateRange = start to end)
        },
        startDateCalendar.get(Calendar.YEAR),
        startDateCalendar.get(Calendar.MONTH),
        startDateCalendar.get(Calendar.DAY_OF_MONTH)
    )

    val endDatePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val end = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.timeInMillis
            val start = filterState.dateRange?.first ?: end
            viewModel.updateFilter(dateRange = start to end)
        },
        endDateCalendar.get(Calendar.YEAR),
        endDateCalendar.get(Calendar.MONTH),
        endDateCalendar.get(Calendar.DAY_OF_MONTH)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Filter Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Date Range
            Text("Date Range", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { startDatePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filterState.dateRange?.first?.let { 
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) 
                        } ?: "Start Date"
                    )
                }
                OutlinedButton(
                    onClick = { endDatePickerDialog.show() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filterState.dateRange?.second?.let { 
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) 
                        } ?: "End Date"
                    )
                }
            }

            // Category
            Text("Category", style = MaterialTheme.typography.titleMedium)
            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = categories.find { it.id == filterState.categoryId }?.name ?: "All Categories",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Categories") },
                        onClick = {
                            viewModel.updateFilter(categoryId = null)
                            expandedCategory = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.updateFilter(categoryId = category.id)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            // Amount Range
            Text("Amount Range", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minAmount,
                    onValueChange = { 
                        minAmount = it
                        viewModel.updateFilter(minAmount = it.toDoubleOrNull())
                    },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxAmount,
                    onValueChange = { 
                        maxAmount = it
                        viewModel.updateFilter(maxAmount = it.toDoubleOrNull())
                    },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Payment Method
            Text("Payment Method", style = MaterialTheme.typography.titleMedium)
            var expandedPayment by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedPayment,
                onExpandedChange = { expandedPayment = !expandedPayment }
            ) {
                OutlinedTextField(
                    value = filterState.paymentMethod?.name?.replace("_", " ") ?: "All Methods",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPayment) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedPayment,
                    onDismissRequest = { expandedPayment = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Methods") },
                        onClick = {
                            viewModel.updateFilter(paymentMethod = null)
                            expandedPayment = false
                        }
                    )
                    PaymentMethod.values().forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.name.replace("_", " ")) },
                            onClick = {
                                viewModel.updateFilter(paymentMethod = method)
                                expandedPayment = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.clearFilters()
                        minAmount = ""
                        maxAmount = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear All")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

