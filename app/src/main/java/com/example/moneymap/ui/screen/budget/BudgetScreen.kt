package com.example.moneymap.ui.screen.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.example.moneymap.data.model.BudgetPeriod
import com.example.moneymap.data.model.CategoryType
import com.example.moneymap.ui.viewmodel.BudgetFormState
import com.example.moneymap.ui.viewmodel.BudgetUiModel
import com.example.moneymap.ui.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val screenState by viewModel.screenState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openCreateBudget) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (screenState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (screenState.budgets.isEmpty()) {
                EmptyBudgetsState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onAddBudget = viewModel::openCreateBudget
                )
            } else {
                BudgetList(
                    budgets = screenState.budgets,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onEditBudget = viewModel::openEditBudget,
                    onDeleteBudget = { budgetId ->
                        coroutineScope.launch { viewModel.deleteBudget(budgetId) }
                    }
                )
            }
        }
    }

    if (formState.isVisible) {
        BudgetFormDialog(
            formState = formState,
            onDismiss = viewModel::dismissForm,
            categories = categories.filter { it.type == CategoryType.EXPENSE },
            onCategorySelected = viewModel::updateCategory,
            onAmountChange = viewModel::updateAmount,
            onPeriodChange = viewModel::updatePeriod,
            onStartDateChange = viewModel::updateStartDate,
            onEndDateChange = viewModel::updateEndDate,
            onAlertThresholdChange = viewModel::updateAlertThreshold,
            onToggleActive = viewModel::updateActive,
            onSave = viewModel::saveBudget
        )
    }
}

@Composable
private fun EmptyBudgetsState(
    modifier: Modifier = Modifier,
    onAddBudget: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(text = "No budgets yet", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Create budgets to track spending limits for each category.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddBudget) {
                Text("Create Budget")
            }
        }
    }
}

@Composable
private fun BudgetList(
    budgets: List<BudgetUiModel>,
    modifier: Modifier = Modifier,
    onEditBudget: (String) -> Unit,
    onDeleteBudget: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(budgets, key = { it.budget.id }) { item ->
            BudgetItem(
                budgetUiModel = item,
                onEdit = { onEditBudget(item.budget.id) },
                onDelete = { onDeleteBudget(item.budget.id) }
            )
        }
    }
}

@Composable
private fun BudgetItem(
    budgetUiModel: BudgetUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val budget = budgetUiModel.budget
    val progress = budgetUiModel.progress.coerceIn(0f, 1f)
    val progressColor = when {
        budgetUiModel.isOverBudget -> MaterialTheme.colorScheme.error
        progress >= budget.alertThreshold -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budgetUiModel.category?.name ?: "Unknown Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${budget.period.name.lowercase().replaceFirstChar { it.uppercase() }} • ${formatDateRange(budget.startDate, budget.endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "KES %.2f / %.2f".format(budgetUiModel.spentAmount, budget.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (budgetUiModel.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (budgetUiModel.daysRemaining > 0) {
                            "${budgetUiModel.daysRemaining} days remaining"
                        } else {
                            "Period ended"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Alert at ${(budget.alertThreshold * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun formatDateRange(start: Long, end: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
    return "${formatter.format(java.util.Date(start))} - ${formatter.format(java.util.Date(end))}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetFormDialog(
    formState: BudgetFormState,
    onDismiss: () -> Unit,
    categories: List<com.example.moneymap.data.model.Category>,
    onCategorySelected: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPeriodChange: (BudgetPeriod) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onAlertThresholdChange: (Float) -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.startDate)
    val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = formState.endDate)

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = startDatePickerState.selectedDateMillis ?: formState.startDate
                    onStartDateChange(selected)
                    showStartDatePicker = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = endDatePickerState.selectedDateMillis ?: formState.endDate
                    onEndDateChange(selected)
                    showEndDatePicker = false
                }) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onSave, enabled = !formState.isSaving) {
                Text(if (formState.isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(if (formState.id == null) "Create Budget" else "Edit Budget") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded }
                ) {
                    val selectedCategoryName = categories.firstOrNull { it.id == formState.categoryId }?.name
                    OutlinedTextField(
                        value = selectedCategoryName ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onCategorySelected(category.id)
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = formState.amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (KES)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Period", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BudgetPeriod.values().forEach { period ->
                            FilterChip(
                                selected = formState.period == period,
                                onClick = { onPeriodChange(period) },
                                label = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Start Date", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { showStartDatePicker = true }, enabled = formState.period == BudgetPeriod.CUSTOM) {
                        Text(java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(formState.startDate)))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "End Date", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { showEndDatePicker = true }, enabled = formState.period == BudgetPeriod.CUSTOM) {
                        Text(java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(formState.endDate)))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Alert Threshold ${(formState.alertThreshold * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = formState.alertThreshold,
                        onValueChange = onAlertThresholdChange,
                        valueRange = 0.5f..1f,
                        steps = 4
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Budget", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = formState.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                formState.errorMessage?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

