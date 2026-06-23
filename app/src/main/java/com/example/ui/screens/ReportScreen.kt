package com.pesalytics.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesalytics.model.TransactionType
import com.pesalytics.ui.theme.AccentGreenLight
import com.pesalytics.ui.theme.ExpenseRed
import java.util.Calendar

private enum class ReportPeriod(val label: String) {
    DAILY("Today"),
    WEEKLY("This Week"),
    MONTHLY("This Month"),
    ALL("All Time")
}

private fun periodStartMs(period: ReportPeriod): Long {
    val cal = Calendar.getInstance()
    return when (period) {
        ReportPeriod.DAILY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        ReportPeriod.WEEKLY -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        ReportPeriod.MONTHLY -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        ReportPeriod.ALL -> 0L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: PesaViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val savedFrequency = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        .getString("report_frequency", "Monthly") ?: "Monthly"
    val initialPeriod = when (savedFrequency) {
        "Daily"  -> ReportPeriod.DAILY
        "Weekly" -> ReportPeriod.WEEKLY
        else     -> ReportPeriod.MONTHLY
    }
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }

    val cutoff = remember(selectedPeriod) { periodStartMs(selectedPeriod) }

    val transactions = remember(uiState.transactions, cutoff) {
        uiState.transactions
            .filter { !it.isFeeTransaction && it.timestamp >= cutoff }
    }

    val totalIncome = transactions.filter {
        it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME
    }.sumOf { it.amount }
    val totalExpense = transactions.filter {
        it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME
    }.sumOf { it.amount }
    val totalFees = transactions.sumOf { it.fee }
    val netSavings = totalIncome - totalExpense

    val topCategory = transactions
        .filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
        .groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amount } }
        .maxByOrNull { it.value }

    val topCategoryPct = if (totalExpense > 0 && topCategory != null)
        (topCategory.value / totalExpense * 100).toInt() else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Periodic Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Period selector
            item {
                Column {
                    Text(
                        "Report Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SegmentedRow(
                        options = ReportPeriod.entries.map { it.label },
                        selected = selectedPeriod.label
                    ) { label ->
                        val period = ReportPeriod.entries.first { it.label == label }
                        selectedPeriod = period
                        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .putString(
                                "report_frequency", when (period) {
                                    ReportPeriod.DAILY   -> "Daily"
                                    ReportPeriod.WEEKLY  -> "Weekly"
                                    ReportPeriod.MONTHLY -> "Monthly"
                                    ReportPeriod.ALL     -> "Monthly"
                                }
                            ).apply()
                    }
                }
            }

            item {
                Text("Your Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                if (transactions.isEmpty()) {
                    Text(
                        "No transactions in this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        if (netSavings >= 0) "You saved KES ${formatCurrency(netSavings)} across ${transactions.size} transactions."
                        else "You spent KES ${formatCurrency(-netSavings)} more than you received this period.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Income", "KES ${formatCurrency(totalIncome)}", AccentGreenLight, Modifier.weight(1f))
                    StatCard("Expense", "KES ${formatCurrency(totalExpense)}", ExpenseRed, Modifier.weight(1f))
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Net Savings", "KES ${formatCurrency(netSavings)}", if (netSavings >= 0) AccentGreenLight else ExpenseRed, Modifier.weight(1f))
                    StatCard("Fees Paid", "KES ${formatCurrency(totalFees)}", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
                }
            }

            if (topCategory != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Top Spending Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${topCategory.key}  ·  $topCategoryPct% of expenses  ·  KES ${formatCurrency(topCategory.value)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Text("Export Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Exports the transactions for the selected period: ${selectedPeriod.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val file = com.pesalytics.utils.CsvExportHelper.exportToCsv(context, transactions)
                            viewModel.addNotification(if (file != null) "CSV saved to Downloads" else "CSV export failed")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export CSV")
                    }
                    Button(
                        onClick = {
                            com.pesalytics.utils.PdfExportHelper.generatePdf(context, transactions) {
                                viewModel.addNotification("PDF print dialog opened.")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight)
                    ) {
                        Text("Export PDF")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
