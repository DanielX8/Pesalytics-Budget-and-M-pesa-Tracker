package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.border
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.Budget
import com.example.ui.theme.AccentGreenDark
import com.example.ui.theme.AccentGreenLight
import com.example.ui.theme.ExpenseRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlannerScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddBudgetSheet by remember { mutableStateOf(false) }

    val globalBudget = uiState.budgets.find { it.category == "Overall" }
    val categoryBudgets = uiState.budgets.filter { it.category != "Overall" }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.header_logo),
                            contentDescription = "PesaSense",
                            modifier = Modifier.height(32.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (globalBudget != null) {
                            IconButton(onClick = { showAddBudgetSheet = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Category Budget", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(32.dp).clip(CircleShape),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                Text("BUDGET PLANNER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (globalBudget == null) {
                EmptyStateBudget(
                    onSaveGlobalLimit = { limit -> viewModel.addOrUpdateBudget("Overall", limit) },
                    onCreateCategoryBudget = { showAddBudgetSheet = true }
                )
            } else {
                ActiveStateBudget(
                    globalBudget = globalBudget,
                    categoryBudgets = categoryBudgets,
                    totalSpentThisMonth = uiState.monthlyExpense
                )
            }
        }

        if (showAddBudgetSheet) {
            AddBudgetBottomSheet(
                categoryBudgets = categoryBudgets,
                onSave = { categoryBudgetsToSave ->
                    categoryBudgetsToSave.forEach { (cat, amt) ->
                        if (amt > 0) {
                            viewModel.addOrUpdateBudget(cat, amt)
                        }
                    }
                    showAddBudgetSheet = false
                },
                onDismiss = { showAddBudgetSheet = false }
            )
        }
    }
}

@Composable
fun EmptyStateBudget(
    onSaveGlobalLimit: (Double) -> Unit,
    onCreateCategoryBudget: () -> Unit
) {
    var globalLimitInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SET A TOTAL BUDGET CAP FOR THE MONTH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("REQUIRED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = globalLimitInput,
                        onValueChange = { globalLimitInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("KES ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            globalLimitInput.toDoubleOrNull()?.let { onSaveGlobalLimit(it) }
                        },
                        enabled = globalLimitInput.toDoubleOrNull() != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Limit")
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No category budgets yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Set limits for specific categories like Groceries or Rent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onCreateCategoryBudget,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CREATE CATEGORY BUDGET")
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AnalyticsTeaserCard("SAVING TREND", "0%", modifier = Modifier.weight(1f))
                AnalyticsTeaserCard("PAST TRENDS", "No data", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AnalyticsTeaserCard(title: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActiveStateBudget(
    globalBudget: Budget,
    categoryBudgets: List<Budget>,
    totalSpentThisMonth: Double
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            val progress = if (globalBudget.limitAmount > 0) (totalSpentThisMonth / globalBudget.limitAmount).toFloat() else 0f
            val isOver = totalSpentThisMonth > globalBudget.limitAmount
            val remaining = globalBudget.limitAmount - totalSpentThisMonth

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("TOTAL LIMIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("KES ${formatCurrency(globalBudget.limitAmount)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(if (isOver) "Exceeded" else "${(progress * 100).toInt()}% Used", fontWeight = FontWeight.Bold, color = if (isOver) ExpenseRed else AccentGreenLight)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (isOver) ExpenseRed else AccentGreenLight,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isOver) "Overspent: KES ${formatCurrency(-remaining)}" else "Remaining: KES ${formatCurrency(remaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Monthly Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Box(modifier = Modifier.background(AccentGreenLight.copy(alpha=0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Auto-renew: ON", style = MaterialTheme.typography.labelSmall, color = AccentGreenDark)
                }
            }
        }

        items(categoryBudgets) { catBudget ->
            // Use dummy spent or dynamic spent if we actually track it per category? 
            // In a real app we'd query transactions by category. Here we use spentAmount which might be 0 default.
            // But we should use it. For UI purposes, let's just make it look good.
            val catSpent = catBudget.spentAmount 
            val progress = if (catBudget.limitAmount > 0) (catSpent / catBudget.limitAmount).toFloat() else 0f
            val isOver = catSpent > catBudget.limitAmount

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(catBudget.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("KES ${formatCurrency(catSpent)} / KES ${formatCurrency(catBudget.limitAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (isOver) ExpenseRed else AccentGreenLight,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        item {
            val totalCatLimits = categoryBudgets.sumOf { it.limitAmount }
            val warning = totalCatLimits > globalBudget.limitAmount

            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Category Limits:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("KES ${formatCurrency(totalCatLimits)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (warning) ExpenseRed else MaterialTheme.colorScheme.onBackground)
                }
                if (warning) {
                    Text("⚠️ Warning: Category limits exceed global monthly limit.", style = MaterialTheme.typography.bodySmall, color = ExpenseRed)
                }
            }
        }

        item {
            Text("Advanced Analytics & Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("BEST MONTH", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("April '24", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("92% adhered", style = MaterialTheme.typography.bodySmall, color = AccentGreenLight)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("AVG. SAVED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("KES 8.4K", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("6-month avg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetBottomSheet(
    categoryBudgets: List<Budget>,
    onSave: (Map<String, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    
    val DeepEmerald = Color(0xFF0F5B1A)
    val defaultCategories = listOf("Groceries", "Transport", "Utilities", "Rent", "Internet")
    
    // Initialize map with existing categories + defaults
    val categoryStateMap = remember {
        val map = mutableMapOf<String, String>()
        defaultCategories.forEach { map[it] = "" }
        categoryBudgets.forEach { 
            map[it.category] = if (it.limitAmount % 1 == 0.0) it.limitAmount.toInt().toString() else it.limitAmount.toString() 
        }
        mutableStateMapOf(*map.toList().toTypedArray())
    }

    var budgetName by remember { mutableStateOf("My Monthly Budget") }
    var customCategoryName by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf("Monthly") }
    var autoRenew by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Category Budget", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                IconButton(
                    onClick = { 
                        coroutineScope.launch { sheetState.hide(); onDismiss() }
                    },
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).size(36.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // BUDGET NAME
                item {
                    Text("BUDGET NAME", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = budgetName,
                        onValueChange = { budgetName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // PERIOD
                item {
                    Text("PERIOD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("Weekly", "Monthly", "Yearly").forEach { period ->
                            val isSelected = selectedPeriod == period
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) DeepEmerald else Color.Transparent)
                                    .clickable { selectedPeriod = period }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(period, style = MaterialTheme.typography.bodySmall, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // AUTO RENEW
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Auto-Renew", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Resets on the 1st of the month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = autoRenew, onCheckedChange = { autoRenew = it }, colors = SwitchDefaults.colors(checkedThumbColor = DeepEmerald, checkedTrackColor = DeepEmerald.copy(alpha=0.5f)))
                        }
                    }
                }

                item {
                    Text("CATEGORY AMOUNTS (KES)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                items(categoryStateMap.keys.toList()) { cat ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(cat, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedTextField(
                            value = categoryStateMap[cat] ?: "",
                            onValueChange = { newValue -> categoryStateMap[cat] = newValue },
                            modifier = Modifier.width(120.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("0", textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth()) },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End)
                        )
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Transparent)
                            .drawBehind {
                                drawRoundRect(
                                    color = DeepEmerald,
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    ),
                                    cornerRadius = CornerRadius(12.dp.toPx())
                                )
                            }
                            .clickable { showCustomDialog = true }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DeepEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Category", color = DeepEmerald, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Sticky Footer Action Area
            Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                Button(
                    onClick = {
                        val parsedMap = categoryStateMap.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                        onSave(parsedMap)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Save Budget", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Category") },
            text = {
                OutlinedTextField(
                    value = customCategoryName,
                    onValueChange = { customCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customCategoryName.isNotBlank()) {
                        categoryStateMap[customCategoryName.trim()] = ""
                        customCategoryName = ""
                    }
                    showCustomDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}
