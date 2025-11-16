package com.example.moneymap.ui.screen.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialogFor by remember { mutableStateOf<Transaction?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val filteredTransactions = remember(transactions, selectedTypeFilter, searchQuery) {
        transactions.filter { transaction ->
            val matchesType = selectedTypeFilter == null || transaction.type == selectedTypeFilter
            val matchesSearch = searchQuery.isBlank() ||
                transaction.notes?.contains(searchQuery, ignoreCase = true) == true ||
                currencyFormat.format(transaction.amount).contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }

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
                onValueChange = { searchQuery = it },
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
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.INCOME,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                    },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                    },
                    label = { Text("Expense") }
                )
            }

            if (filteredTransactions.isEmpty()) {
                EmptyTransactionsState(
                    hasTransactions = transactions.isNotEmpty(),
                    onAddTransaction = onNavigateToAddTransaction
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        val category = categories.firstOrNull { it.id == transaction.categoryId }
                        TransactionListItem(
                            transaction = transaction,
                            category = category,
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
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                Text(
                    text = buildString {
                        append(if (transaction.type == TransactionType.INCOME) "+" else "-")
                        append(currencyFormat.format(transaction.amount))
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.INCOME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
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

