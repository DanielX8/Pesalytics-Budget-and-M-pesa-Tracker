package com.pesalytics.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesalytics.model.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayeeHistoryScreen(payee: String, viewModel: PesaViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val payeeTransactions = remember(uiState.transactions, payee) {
        uiState.transactions.filter { t ->
            !t.isFeeTransaction &&
            (t.payee.contains(payee, ignoreCase = true) ||
             t.accountRef?.contains(payee, ignoreCase = true) == true)
        }.sortedByDescending { it.timestamp }
    }

    val totalPaid = payeeTransactions.sumOf { it.amount }

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showCategoryEdit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(payee, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "${payeeTransactions.size} transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "KES ${formatCurrency(totalPaid)} total",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (payeeTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions found for this payee",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(payeeTransactions) { transaction ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        TransactionItem(
                            transaction = transaction,
                            onClick = { selectedTransaction = transaction },
                            showDate = true
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showCategoryEdit && selectedTransaction != null) {
        var newCategoryName by remember { mutableStateOf(selectedTransaction!!.category) }
        val predefinedCategories = listOf("Groceries", "Utilities", "Food & Dining", "Transport", "Shopping", "Entertainment", "Health", "Airtime", "Other")
        AlertDialog(
            onDismissRequest = { showCategoryEdit = false },
            title = { Text("Edit Category") },
            text = {
                Column {
                    Text("Change category for all past and future transactions from:", style = MaterialTheme.typography.bodySmall)
                    Text(selectedTransaction!!.payee, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        predefinedCategories.forEach { category ->
                            FilterChip(
                                selected = newCategoryName.equals(category, ignoreCase = true),
                                onClick = { newCategoryName = category },
                                label = { Text(category) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Custom Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        viewModel.updateTransactionCategory(selectedTransaction!!, newCategoryName.trim())
                        selectedTransaction = selectedTransaction!!.copy(category = newCategoryName.trim())
                    }
                    showCategoryEdit = false
                }) { Text("Save", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryEdit = false }) { Text("Cancel") }
            }
        )
    }

    selectedTransaction?.let { tx ->
        TransactionDetailsSheet(
            transaction = tx,
            onDismiss = { selectedTransaction = null },
            onEditCategory = { showCategoryEdit = true },
            onShare = { selectedTransaction = null }
        )
    }
}
