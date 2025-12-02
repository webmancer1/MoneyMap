package com.example.moneymap.ui.screen.insights

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.ui.viewmodel.InsightsViewModel
import com.example.moneymap.ui.viewmodel.InsightType
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val insights = viewModel.getInsights()
    val topCategories = viewModel.getTopSpendingCategories()
    val monthlyExpenses = viewModel.getMonthlyExpenseTotal()
    val monthlyIncome = viewModel.getMonthlyIncomeTotal()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Insights") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "This Month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = currencyFormat.format(monthlyIncome),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = currencyFormat.format(monthlyExpenses),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (monthlyIncome > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val expenseRatio = (monthlyExpenses / monthlyIncome) * 100
                            LinearProgressIndicator(
                                progress = (expenseRatio / 100).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = if (expenseRatio > 90) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Spending ${String.format("%.1f", expenseRatio)}% of income",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Top Spending Categories
            if (topCategories.isNotEmpty()) {
                item {
                    Text(
                        text = "Top Spending Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(topCategories) { category ->
                    CategorySpendingCard(category = category, currencyFormat = currencyFormat)
                }
            }

            // Insights
            if (insights.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Insights & Recommendations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(insights) { insight ->
                    InsightCard(insight = insight, currencyFormat = currencyFormat)
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add more transactions to get personalized insights!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: com.example.moneymap.ui.viewmodel.InsightSpendingInsight,
    currencyFormat: NumberFormat
) {
    val (icon, color) = when (insight.type) {
        InsightType.WARNING -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        InsightType.SUCCESS -> Icons.Default.CheckCircle to MaterialTheme.colorScheme.primary
        InsightType.INFO -> Icons.Default.Info to MaterialTheme.colorScheme.primary
        InsightType.SUGGESTION -> Icons.Default.Star to MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.type) {
                InsightType.WARNING -> MaterialTheme.colorScheme.errorContainer
                InsightType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                InsightType.INFO -> MaterialTheme.colorScheme.surfaceVariant
                InsightType.SUGGESTION -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                insight.amount?.let { amount ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Amount: ${currencyFormat.format(amount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySpendingCard(
    category: com.example.moneymap.ui.viewmodel.InsightCategorySpending,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${category.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(category.totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${String.format("%.1f", category.percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = (category.percentage / 100).toFloat().coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

