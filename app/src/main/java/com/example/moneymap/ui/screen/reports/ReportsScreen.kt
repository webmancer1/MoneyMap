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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.toArgb

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
            onNavigateMonth = viewModel::navigateMonth,
            onNavigateYear = viewModel::navigateYear,
            contentPadding = paddingValues
        )
    }
}

@Composable
private fun ReportsContent(
    uiState: ReportsUiState,
    onPeriodSelected: (ReportPeriod) -> Unit,
    onNavigateMonth: (Int) -> Unit,
    onNavigateYear: (Int) -> Unit,
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

        TopCategoriesList(
            categories = uiState.spendingByCategory,
            currency = uiState.currency
        )

        PaymentMethodBreakdown(
            paymentMethods = uiState.paymentMethodSpending,
            currency = uiState.currency
        )

        IncomeVsExpenseChart(
            title = "Monthly Activity",
            data = uiState.currentMonthData,
            periodLabel = uiState.currentMonthLabel,
            onPreviousClick = { onNavigateMonth(-1) },
            onNextClick = { onNavigateMonth(1) }
        )

        IncomeVsExpenseChart(
            title = "Yearly Overview",
            data = uiState.yearlyData,
            periodLabel = uiState.currentYearLabel,
            onPreviousClick = { onNavigateYear(-1) },
            onNextClick = { onNavigateYear(1) }
        )
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
                                    color = MaterialTheme.colorScheme.onSurface // Changed from onSurfaceVariant for better visibility
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
    }
}

fun parseColor(colorString: String): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(AndroidColor.parseColor(colorString))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Gray
    }
}

@Composable
private fun IncomeVsExpenseChart(
    title: String,
    data: List<com.example.moneymap.ui.viewmodel.ChartDataPoint>,
    periodLabel: String? = null,
    onPreviousClick: (() -> Unit)? = null,
    onNextClick: (() -> Unit)? = null
) {
    val contentColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            if (periodLabel != null && onPreviousClick != null && onNextClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onPreviousClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Previous"
                        )
                    }
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }

        // Only show chart if there is data
        val hasData = remember(data) {
            data.any { it.income > 0 || it.expense > 0 }
        }

        if (!hasData) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No transaction data available.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Prepare data entries
            val incomeEntries = remember(data) {
                data.mapIndexed { index, point ->
                    BarEntry(index.toFloat(), point.income.toFloat())
                }
            }
            val expenseEntries = remember(data) {
                data.mapIndexed { index, point ->
                    BarEntry(index.toFloat(), point.expense.toFloat())
                }
            }
            val labels = remember(data) {
                data.map { it.label }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                textColor = contentColor // Apply theme color
                                axisLineColor = contentColor // Apply theme color to axis line
                            }
                            
                            axisLeft.apply {
                                setDrawGridLines(true)
                                axisMinimum = 0f
                                textColor = contentColor // Apply theme color
                                axisLineColor = contentColor // Apply theme color to axis line
                            }
                            axisRight.isEnabled = false
                            
                            legend.apply {
                                isEnabled = true
                                textColor = contentColor // Apply theme color to legend
                            }
                            
                            setPinchZoom(true)
                            setScaleEnabled(true)
                        }
                    },
                    update = { chart ->
                        val incomeDataSet = BarDataSet(incomeEntries, "Income").apply {
                            color = AndroidColor.parseColor("#4CAF50")
                            valueTextSize = 10f
                            valueTextColor = contentColor // Apply theme color to values
                        }
                        
                        val expenseDataSet = BarDataSet(expenseEntries, "Expense").apply {
                            color = AndroidColor.parseColor("#F44336")
                            valueTextSize = 10f
                            valueTextColor = contentColor // Apply theme color to values
                        }

                        val barData = BarData(incomeDataSet, expenseDataSet)
                        barData.barWidth = 0.3f
                        
                        chart.data = barData
                        
                        // Update colors on update as well to handle theme changes
                        chart.xAxis.textColor = contentColor
                        chart.xAxis.axisLineColor = contentColor
                        chart.axisLeft.textColor = contentColor
                        chart.axisLeft.axisLineColor = contentColor
                        chart.legend.textColor = contentColor

                        // Group bars logic
                        val groupSpace = 0.2f
                        val barSpace = 0.05f
                        chart.xAxis.axisMinimum = 0f
                        chart.xAxis.axisMaximum = labels.size.toFloat()
                        
                        chart.groupBars(0f, 0.3f, 0.05f)
                        
                        chart.invalidate()
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
private fun TopCategoriesList(
    categories: List<CategorySpending>,
    currency: String
) {
    if (categories.isEmpty()) return
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Top Spending Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        categories.take(5).forEach { category ->
            CategorySpendingItem(
                category = category,
                currency = currency
            )
        }
    }
}

@Composable
private fun CategorySpendingItem(
    category: CategorySpending,
    currency: String
) {
    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            this.currency = java.util.Currency.getInstance(currency)
        }
    }
    
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
private fun PaymentMethodBreakdown(
    paymentMethods: List<PaymentMethodSpending>,
    currency: String
) {
    if (paymentMethods.isEmpty()) return
    
    val currencyFormat = remember(currency) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            this.currency = java.util.Currency.getInstance(currency)
        }
    }
    
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

