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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: PesaViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val transactions = uiState.transactions.filter { !it.isFeeTransaction }
    val totalIncome = transactions.filter { it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }.sumOf { it.amount }
    val totalFees = transactions.sumOf { it.fee }
    val netSavings = totalIncome - totalExpense

    val topCategory = transactions
        .filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
        .groupBy { it.category }
        .mapValues { e -> e.value.sumOf { it.amount } }
        .maxByOrNull { it.value }

    val topCategoryPct = if (totalExpense > 0 && topCategory != null) (topCategory.value / totalExpense * 100).toInt() else 0

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

            item {
                Text("Your Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (netSavings >= 0) "You saved KES ${formatCurrency(netSavings)} across ${transactions.size} transactions."
                    else "You spent KES ${formatCurrency(-netSavings)} more than you received this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val file = com.pesalytics.utils.CsvExportHelper.exportToCsv(context, uiState.transactions)
                            viewModel.addNotification(if (file != null) "CSV saved to Downloads" else "CSV export failed")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export CSV")
                    }
                    Button(
                        onClick = {
                            com.pesalytics.utils.PdfExportHelper.generatePdf(context, uiState.transactions) {
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
