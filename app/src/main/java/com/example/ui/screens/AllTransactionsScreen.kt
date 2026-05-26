package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(viewModel: PesaViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTransaction by remember { mutableStateOf<com.example.model.Transaction?>(null) }
    var showTransactionDetails by remember { mutableStateOf(false) }
    var showCategoryEdit by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Supported filter chips
    val filterOptions = listOf("All", "Send Money", "Received", "Paybill", "Buy Goods", "Withdraw", "Airtime", "Fuliza")
    var selectedFilter by remember { mutableStateOf(filterOptions[0]) }

    val monthFormat = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()) }
    val currentMonth = remember { monthFormat.format(java.util.Date()) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }

    val availableMonths by remember(uiState.transactions) {
        derivedStateOf {
            val months = uiState.transactions
                .map { monthFormat.format(java.util.Date(it.timestamp)) }
                .distinct()
                .sortedByDescending { monthFormat.parse(it)?.time ?: 0L }
            if (months.contains(currentMonth)) months else (listOf(currentMonth) + months).distinct().sortedByDescending { monthFormat.parse(it)?.time ?: 0L }
        }
    }

    val filterCounts by remember(uiState.transactions, selectedMonth) {
        derivedStateOf {
            val transactionsInMonth = uiState.transactions.filter { 
                 monthFormat.format(java.util.Date(it.timestamp)) == selectedMonth 
            }
            filterOptions.associateWith { filter ->
                transactionsInMonth.count { transaction ->
                    if (transaction.isFeeTransaction) return@count false
                    when (filter) {
                        "All" -> true
                        "Send Money" -> transaction.type == com.example.model.TransactionType.SEND_MONEY
                        "Received" -> transaction.type == com.example.model.TransactionType.RECEIVE_MONEY || transaction.type == com.example.model.TransactionType.MANUAL_INCOME
                        "Paybill" -> transaction.type == com.example.model.TransactionType.PAYBILL
                        "Buy Goods" -> transaction.type == com.example.model.TransactionType.BUY_GOODS
                        "Withdraw" -> transaction.type == com.example.model.TransactionType.WITHDRAW
                        "Airtime" -> transaction.type == com.example.model.TransactionType.AIRTIME
                        "Fuliza" -> transaction.category.equals("Fuliza", ignoreCase = true)
                        else -> true
                    }
                }
            }
        }
    }

    val filteredTransactions by remember(uiState.transactions, selectedFilter, selectedMonth) {
        derivedStateOf {
            uiState.transactions.filter { transaction ->
                if (transaction.isFeeTransaction) return@filter false
                if (monthFormat.format(java.util.Date(transaction.timestamp)) != selectedMonth) return@filter false
                
                when (selectedFilter) {
                    "All" -> true
                    "Send Money" -> transaction.type == com.example.model.TransactionType.SEND_MONEY
                    "Received" -> transaction.type == com.example.model.TransactionType.RECEIVE_MONEY || transaction.type == com.example.model.TransactionType.MANUAL_INCOME
                    "Paybill" -> transaction.type == com.example.model.TransactionType.PAYBILL
                    "Buy Goods" -> transaction.type == com.example.model.TransactionType.BUY_GOODS
                    "Withdraw" -> transaction.type == com.example.model.TransactionType.WITHDRAW
                    "Airtime" -> transaction.type == com.example.model.TransactionType.AIRTIME
                    "Fuliza" -> transaction.category.equals("Fuliza", ignoreCase = true)
                    else -> true
                }
            }
        }
    }

    if (showCategoryEdit && selectedTransaction != null) {
        var newCategoryName by remember { mutableStateOf(selectedTransaction!!.category) }
        AlertDialog(
            onDismissRequest = { showCategoryEdit = false },
            title = { Text("Edit Category") },
            text = {
                Column {
                    Text("Change category for all past and future transactions from:", style = MaterialTheme.typography.bodySmall)
                    Text(selectedTransaction!!.payee, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New Category") },
                        singleLine = true
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

    if (showTransactionDetails && selectedTransaction != null) {
        TransactionDetailsSheet(
            transaction = selectedTransaction!!,
            onDismiss = { showTransactionDetails = false },
            onEditCategory = { showCategoryEdit = true },
            onShare = {
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Transaction Receipt:\nRef: ${selectedTransaction!!.remoteRef}\nPayee: ${selectedTransaction!!.payee}\nAmount: KES ${formatCurrency(selectedTransaction!!.amount)}")
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
        )
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text("PesaSense", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { /* Handle Notifications */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .size(10.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(com.example.ui.theme.ExpenseRed)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                Text("ALL TRANSACTIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Metric
            val filteredAmount = filteredTransactions.sumOf { 
                if (it.type == com.example.model.TransactionType.RECEIVE_MONEY || it.type == com.example.model.TransactionType.MANUAL_INCOME) it.amount else -it.amount 
            }
            val amtColor = if (filteredAmount >= 0) MaterialTheme.colorScheme.primary else com.example.ui.theme.ExpenseRed
            val amtPrefix = if (filteredAmount >= 0) "+KES " else "-KES "
            
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = "$amtPrefix${formatCurrency(kotlin.math.abs(filteredAmount))}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = amtColor
                )
                Text(
                    text = "${filteredTransactions.size} transactions in ${if (selectedMonth == currentMonth) "This Month" else selectedMonth}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Month Tabs
            ScrollableTabRow(
                selectedTabIndex = availableMonths.indexOf(selectedMonth).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                availableMonths.forEachIndexed { index, month ->
                    Tab(
                        selected = selectedMonth == month,
                        onClick = { selectedMonth = month },
                        text = { 
                            Text(
                                text = if (month == currentMonth) "This Month" else month, 
                                fontWeight = if (selectedMonth == month) FontWeight.Bold else FontWeight.Normal 
                            ) 
                        }
                    )
                }
            }

            // Filter Chips
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = filter == selectedFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text("$filter ${filterCounts[filter] ?: 0}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                }
            }

            // Group by date string
            val today = java.util.Calendar.getInstance()
            val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
            val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())

            val groupedTransactions = filteredTransactions.groupBy { transaction ->
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                when {
                    cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) -> "TODAY"
                    cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) - 1 -> "YESTERDAY"
                    today.get(java.util.Calendar.DAY_OF_YEAR) - cal.get(java.util.Calendar.DAY_OF_YEAR) < 7 && cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) -> dayFormat.format(cal.time).uppercase(java.util.Locale.getDefault())
                    else -> dateFormat.format(java.util.Date(transaction.timestamp)).uppercase(java.util.Locale.getDefault())
                }
            }

            // AnimatedContent keyed on selectedFilter — crossfades when user taps a chip
            AnimatedContent(
                targetState = Pair(selectedFilter, groupedTransactions),
                transitionSpec = {
                    fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                },
                label = "FilteredTransactions",
                modifier = Modifier.weight(1f)
            ) { (_, grouped) ->
                if (grouped.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No $selectedFilter transactions",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Try a different filter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        grouped.forEach { (dateString, transactionsForDate) ->
                            item(key = "header_$dateString") {
                                Text(
                                    text = dateString.uppercase(java.util.Locale.getDefault()),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp).animateItem()
                                )
                            }
                            
                            item(key = "card_$dateString") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(2.dp, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                        .animateItem(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                ) {
                                    Column {
                                        transactionsForDate.forEachIndexed { index, transaction ->
                                            TransactionItem(transaction = transaction, onClick = {
                                                selectedTransaction = transaction
                                                showTransactionDetails = true
                                            })
                                            if (index < transactionsForDate.size - 1) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
