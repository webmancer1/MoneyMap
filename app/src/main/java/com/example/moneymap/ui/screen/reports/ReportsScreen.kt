package com.example.moneymap.ui.screen.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.ui.viewmodel.ReportPeriod
import com.example.moneymap.ui.viewmodel.ReportsUiState
import com.example.moneymap.ui.viewmodel.ReportsViewModel
import com.example.moneymap.ui.viewmodel.CategorySpending
import com.example.moneymap.ui.viewmodel.PaymentMethodSpending
import com.example.moneymap.ui.viewmodel.SpendingInsight
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import android.graphics.Color as AndroidColor

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
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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

        SpendingInsightsSection(uiState.spendingInsights)

        SpendingByCategoryChart(uiState)

        TopCategoriesList(uiState.spendingByCategory)

        PaymentMethodBreakdown(uiState.paymentMethodSpending)

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No expense data available for the selected period.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val totalAmount = remember(uiState.spendingByCategory) {
                uiState.spendingByCategory.sumOf { it.amount }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PieChart(
                        data = uiState.spendingByCategory,
                        totalAmount = totalAmount,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Legend
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.spendingByCategory.forEach { category ->
                         val percentage = if (totalAmount > 0) {
                            (category.amount / totalAmount) * 100
                        } else 0.0
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(color = parseColor(category.color))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = category.categoryName,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${String.format("%.1f", percentage)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<CategorySpending>,
    totalAmount: Double,
    modifier: Modifier = Modifier
) {
    val animateStroke = remember { androidx.compose.animation.core.Animatable(0f) }
    
    LaunchedEffect(data) {
        animateStroke.snapTo(0f)
        animateStroke.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000)
        )
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val radius = size.minDimension / 2.5f
        var startAngle = -90f

        data.forEach { category ->
            val sweepAngle = if (totalAmount > 0) {
                (category.amount / totalAmount * 360f).toFloat()
            } else 0f
            
            // Animate swipe
            val animatedSweep = sweepAngle * animateStroke.value

            drawArc(
                color = parseColor(category.color),
                startAngle = startAngle,
                sweepAngle = animatedSweep,
                useCenter = true,
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )
            startAngle += sweepAngle
        }
        
        // Draw inner circle for donut effect (optional, removed for pure pie)
        // drawCircle(
        //     color = Color.White, // Use background color
        //     radius = radius * 0.5f
        // )
    }
}

fun parseColor(colorString: String): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Gray
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

        // Only show chart if there is data
        val hasData = remember(uiState.chartData) {
            uiState.chartData.any { it.income > 0 || it.expense > 0 }
        }

        if (!hasData) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No transaction data available for the selected period.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // Prepare data entries
            // Prepare data entries
            val incomeEntries = remember(uiState.chartData) {
                uiState.chartData.mapIndexed { index, data ->
                    BarEntry(index.toFloat(), data.income.toFloat())
                }
            }
            val expenseEntries = remember(uiState.chartData) {
                uiState.chartData.mapIndexed { index, data ->
                    BarEntry(index.toFloat(), data.expense.toFloat())
                }
            }
            val labels = remember(uiState.chartData) {
                uiState.chartData.map { it.label }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), // Increased height for MPAndroidChart
                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // White background for chart clarity
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    factory = { ctx ->
                        BarChart(ctx).apply {
                            description.isEnabled = false
                            setDrawGridBackground(false)
                            setDrawBarShadow(false)
                            
                            // Axis configuration
                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                granularity = 1f
                                valueFormatter = IndexAxisValueFormatter(labels)
                            }
                            
                            axisLeft.apply {
                                setDrawGridLines(true)
                                axisMinimum = 0f // Start from 0
                            }
                            axisRight.isEnabled = false
                            
                            legend.isEnabled = true
                            
                            // Interactions
                            setPinchZoom(true)
                            setScaleEnabled(true)
                        }
                    },
                    update = { chart ->
                        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
                            color = AndroidColor.parseColor("#4CAF50") // Green
                            valueTextSize = 10f
                        }
                        
                        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
                            color = AndroidColor.parseColor("#F44336") // Red
                            valueTextSize = 10f
                        }

                        val barData = BarData(incomeDataSet, expenseDataSet)
                        barData.barWidth = 0.3f // Narrow bars
                        
                        chart.data = barData
                        
                        // Group bars logic
                        val groupSpace = 0.2f
                        val barSpace = 0.05f // x2 dataset
                        // (barWidth + barSpace) * 2 + groupSpace = 1.00 -> (0.3 + 0.05) * 2 + 0.3 = 1.0
                        // 0.35 * 2 = 0.7 + 0.3 = 1.0
                        // Correct grouping: (0.3 + 0.05) * 2 + 0.2 = 0.9? Wait
                        // Formula: (barWidth + barSpace) * count + groupSpace = 1.00
                        // Let's set groupSpace = 0.3f, barSpace = 0.05f. barWidth = 0.3f.
                        // (0.3 + 0.05) * 2 + 0.3 = 0.7 + 0.3 = 1.0 OK.
                        
                        // Note: we need to reset axis min/max to fit groups
                        chart.xAxis.axisMinimum = 0f
                        chart.xAxis.axisMaximum = labels.size.toFloat() // start + size (?)
                        
                        chart.groupBars(0f, 0.3f, 0.05f) // fromX, groupSpace, barSpace
                        
                        chart.invalidate() // Refresh
                    }
                )
            }
        }
    }
}

@Composable
private fun SpendingInsightsSection(insights: List<SpendingInsight>) {
    if (insights.isEmpty()) return
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Spending Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            insights.take(2).forEach { insight ->
                InsightCard(
                    insight = insight,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        if (insights.size > 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                insights.drop(2).take(2).forEach { insight ->
                    InsightCard(
                        insight = insight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: SpendingInsight,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = insight.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TopCategoriesList(categories: List<CategorySpending>) {
    if (categories.isEmpty()) return
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Top Spending Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        categories.take(5).forEach { category ->
            CategorySpendingItem(category = category)
        }
    }
}

@Composable
private fun CategorySpendingItem(category: CategorySpending) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${category.transactionCount} transactions • Avg: ${currencyFormat.format(category.averageAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = currencyFormat.format(category.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PaymentMethodBreakdown(paymentMethods: List<PaymentMethodSpending>) {
    if (paymentMethods.isEmpty()) return
    
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Payment Method Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        paymentMethods.forEach { method ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = method.paymentMethod,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${method.transactionCount} transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = currencyFormat.format(method.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

