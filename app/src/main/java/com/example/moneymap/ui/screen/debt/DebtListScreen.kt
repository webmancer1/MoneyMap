package com.example.moneymap.ui.screen.debt

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.data.model.Debt
import com.example.moneymap.data.model.DebtType
import com.example.moneymap.ui.viewmodel.DebtViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddDebt: () -> Unit,
    onNavigateToEditDebt: (String) -> Unit,
    viewModel: DebtViewModel = hiltViewModel()
) {
    val debts by viewModel.debts.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTypeFilter by remember { mutableStateOf<DebtType?>(null) }
    var showDeleteDialogFor by remember { mutableStateOf<Debt?>(null) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val filteredDebts = remember(debts, selectedTypeFilter) {
        debts.filter { debt ->
            selectedTypeFilter == null || debt.type == selectedTypeFilter
        }
    }

    val totalPayable = remember(debts) {
        debts.filter { it.type == DebtType.PAYABLE }
            .sumOf { it.totalAmount - it.paidAmount }
    }

    val totalReceivable = remember(debts) {
        debts.filter { it.type == DebtType.RECEIVABLE }
            .sumOf { it.totalAmount - it.paidAmount }
    }

    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect {
            val result = snackbarHostState.showSnackbar(
                message = "Debt deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreDeletedDebt()
            } else {
                viewModel.clearDeleteState()
            }
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
                title = { Text("Debt Tracker") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddDebt) {
                        Icon(Icons.Default.Add, contentDescription = "Add Debt")
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
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "You Owe",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = currencyFormat.format(totalPayable),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "You're Owed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = currencyFormat.format(totalReceivable),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Filter Chips
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
                    selected = selectedTypeFilter == DebtType.PAYABLE,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == DebtType.PAYABLE) null else DebtType.PAYABLE
                    },
                    label = { Text("Payable") }
                )
                FilterChip(
                    selected = selectedTypeFilter == DebtType.RECEIVABLE,
                    onClick = {
                        selectedTypeFilter = if (selectedTypeFilter == DebtType.RECEIVABLE) null else DebtType.RECEIVABLE
                    },
                    label = { Text("Receivable") }
                )
            }

            if (filteredDebts.isEmpty()) {
                EmptyDebtsState(
                    hasDebts = debts.isNotEmpty(),
                    onAddDebt = onNavigateToAddDebt
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDebts, key = { it.id }) { debt ->
                        DebtListItem(
                            debt = debt,
                            currencyFormat = currencyFormat,
                            dateFormat = dateFormat,
                            onEdit = { onNavigateToEditDebt(debt.id) },
                            onDelete = { showDeleteDialogFor = debt }
                        )
                    }
                }
            }
        }
    }

    showDeleteDialogFor?.let { debt ->
        DeleteDebtDialog(
            onDismiss = { showDeleteDialogFor = null },
            onConfirm = {
                showDeleteDialogFor = null
                coroutineScope.launch { viewModel.deleteDebt(debt) }
            }
        )
    }
}

@Composable
private fun EmptyDebtsState(
    hasDebts: Boolean,
    onAddDebt: () -> Unit
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
                text = if (hasDebts) {
                    "No debts match your filter"
                } else {
                    "No debts yet"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DebtListItem(
    debt: Debt,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val remainingAmount = debt.totalAmount - debt.paidAmount
    val progress = if (debt.totalAmount > 0) (debt.paidAmount / debt.totalAmount).toFloat() else 0f

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
                        text = debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = debt.type.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = currencyFormat.format(remainingAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (debt.type == DebtType.PAYABLE) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            debt.dueDate?.let { dueDate ->
                Text(
                    text = "Due: ${dateFormat.format(dueDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Total: ${currencyFormat.format(debt.totalAmount)} | Paid: ${currencyFormat.format(debt.paidAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            debt.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
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
                    Icon(Icons.Default.Edit, contentDescription = "Edit Debt")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Debt",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Debt") },
        text = {
            Text("Are you sure you want to delete this debt? This action cannot be undone.")
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

