package com.example.moneymap.ui.screen.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.ui.viewmodel.ReportPeriod
import com.example.moneymap.ui.viewmodel.ReportsUiState
import com.example.moneymap.ui.viewmodel.ReportsViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ReportsContent(
            uiState = uiState,
            onPeriodSelected = viewModel::onPeriodSelected,
            contentPadding = paddingValues
        )
    }
}

@Composable
private fun ReportsContent(
    uiState: ReportsUiState,
    onPeriodSelected: (ReportPeriod) -> Unit,
    contentPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PeriodFilterRow(
            selectedPeriod = uiState.selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )

        SummaryCards(
            totalIncome = uiState.totalIncome,
            totalExpense = uiState.totalExpense
        )

        SpendingByCategoryChart(uiState)

        IncomeVsExpenseChart(uiState)
    }
}

@Composable
private fun PeriodFilterRow(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit
) {
    Text(
        text = "Time Period",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReportPeriod.values().forEach { period ->
            FilterChip(
                selected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                label = { Text(period.label) }
            )
        }
    }
}

@Composable
private fun SummaryCards(
    totalIncome: Double,
    totalExpense: Double
) {
    val balance = totalIncome - totalExpense
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard(title = "Total Income", amount = totalIncome, color = MaterialTheme.colorScheme.primary)
        SummaryCard(title = "Total Expenses", amount = totalExpense, color = MaterialTheme.colorScheme.error)
        SummaryCard(title = "Net Balance", amount = balance, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${if (amount >= 0) "" else "-"}%.2f".format(kotlin.math.abs(amount)),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SpendingByCategoryChart(uiState: ReportsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Spending by Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (uiState.spendingByCategory.isEmpty()) {
            Text("No expense data available for the selected period.")
        } else {
            val entries = remember(uiState.spendingByCategory) {
                uiState.spendingByCategory.mapIndexed { index, data ->
                    entryOf(index.toFloat(), data.amount.toFloat())
                }
            }
            val labels = remember(uiState.spendingByCategory) {
                uiState.spendingByCategory.map { it.categoryName }
            }
            val modelProducer = remember(entries) {
                ChartEntryModelProducer(listOf(entries))
            }

            Chart(
                chart = columnChart(),
                model = modelProducer.getModel() ?: return,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = { value, _ ->
                    labels.getOrNull(value.toInt()) ?: ""
                })
            )
        }
    }
}

@Composable
private fun IncomeVsExpenseChart(uiState: ReportsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Income vs Expense",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (uiState.monthlyIncomeExpense.isEmpty()) {
            Text("No transaction data available for the selected period.")
        } else {
            val incomeEntries = remember(uiState.monthlyIncomeExpense) {
                uiState.monthlyIncomeExpense.mapIndexed { index, data ->
                    entryOf(index.toFloat(), data.income.toFloat())
                }
            }
            val expenseEntries = remember(uiState.monthlyIncomeExpense) {
                uiState.monthlyIncomeExpense.mapIndexed { index, data ->
                    entryOf(index.toFloat(), data.expense.toFloat())
                }
            }
            val labels = remember(uiState.monthlyIncomeExpense) {
                uiState.monthlyIncomeExpense.map { it.monthLabel }
            }
            val modelProducer = remember(incomeEntries, expenseEntries) {
                ChartEntryModelProducer(listOf(incomeEntries, expenseEntries))
            }

            Chart(
                chart = columnChart(),
                model = modelProducer.getModel() ?: return,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(valueFormatter = { value, _ ->
                    labels.getOrNull(value.toInt()) ?: ""
                })
            )
        }
    }
}

