package com.pesasense.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pesasense.ui.theme.AccentGreenDark
import com.pesasense.ui.theme.AccentGreenLight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesasense.R
import com.pesasense.model.Goal
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesasense.model.GoalType
import com.pesasense.ui.theme.ExpenseRed
import com.pesasense.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialGoalsScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    var showCreateGoalSheet by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }

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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreenDark) // Deep Emerald
                                    .clickable {
                                        if (!isPremium && goals.size >= 1) {
                                            showUpgradeDialog = true
                                        } else {
                                            showCreateGoalSheet = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Financial Goals",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Track your dreams and obligations in one place.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            if (goals.isEmpty()) {
                // Empty State
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = AccentGreenDark // Deep Emerald green
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "No goals yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            "Start your journey towards financial freedom by setting your first savings or debt payoff goal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { showCreateGoalSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Goal", maxLines = 1, color = Color.White)
                        }
                    }
                }
            } else {
                // Populated State
                var goalForContribution by remember { mutableStateOf<Goal?>(null) }
                var goalForDelete by remember { mutableStateOf<Goal?>(null) }

                goals.forEach { goal ->
                    GoalCard(
                        goal = goal,
                        onAddContribution = { goalForContribution = goal },
                        onDelete = { goalForDelete = goal }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Contribution dialog
                goalForContribution?.let { goal ->
                    var contributionAmount by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { goalForContribution = null; contributionAmount = "" },
                        title = { Text("Add Contribution") },
                        text = {
                            Column {
                                Text("Recording towards: ${goal.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = contributionAmount,
                                    onValueChange = { contributionAmount = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Amount (KES)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                val remaining = goal.targetAmount - goal.savedAmount
                                if (remaining > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("KES ${"%,.0f".format(remaining)} remaining to goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val amount = contributionAmount.toDoubleOrNull()
                                if (amount != null && amount > 0) {
                                    viewModel.addGoalContribution(goal.id, amount)
                                    goalForContribution = null
                                    contributionAmount = ""
                                }
                            }, enabled = contributionAmount.toDoubleOrNull() != null) {
                                Text("Save", color = AccentGreenDark)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { goalForContribution = null; contributionAmount = "" }) { Text("Cancel") }
                        }
                    )
                }

                // Delete confirmation dialog
                goalForDelete?.let { goal ->
                    AlertDialog(
                        onDismissRequest = { goalForDelete = null },
                        title = { Text("Delete Goal") },
                        text = { Text("Delete \"${goal.name}\"? This cannot be undone.") },
                        confirmButton = {
                            TextButton(onClick = { viewModel.deleteGoal(goal.id); goalForDelete = null }) {
                                Text("Delete", color = ExpenseRed)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { goalForDelete = null }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text("Track unlimited goals", fontWeight = FontWeight.Bold) },
            text = { Text("You've used your 1 free goal. Upgrade to Premium to track all your savings goals — KES 299/month or KES 2,000/year.") },
            confirmButton = {
                Button(onClick = { showUpgradeDialog = false; onNavigateToSubscription() }) {
                    Text("Upgrade to Premium")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text("Remind me later")
                }
            }
        )
    }

    if (showCreateGoalSheet) {
        CreateGoalBottomSheet(
            onDismiss = { showCreateGoalSheet = false },
            onSave = { goal ->
                viewModel.addGoal(goal)
                showCreateGoalSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalBottomSheet(
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var goalName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GoalType.SAVINGS) }
    var targetAmount by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<Long?>(null) }
    var monthlyContribution by remember { mutableStateOf("") }

    val colors = listOf(Color(0xFF00C853), Color(0xFF2962FF), Color(0xFFFFAB00), Color(0xFFD50000), Color(0xFFAA00FF), Color(0xFFC51162), Color(0xFF00BFA5), Color(0xFFFF6D00))
    var selectedColor by remember { mutableStateOf(colors[0]) }

    val icons = listOf("🎯", "🏠", "✈️", "🎓", "🚗", "💍", "💻", "📱", "🏋️", "💰", "🌍", "⛵")
    var selectedIcon by remember { mutableStateOf(icons[0]) }
    
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.width(32.dp).height(4.dp).clip(CircleShape).background(Color.Gray))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxHeight(0.85f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                    IconButton(onClick = onDismiss, modifier = Modifier.background(Color.LightGray.copy(alpha=0.3f), CircleShape)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("GOAL NAME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    placeholder = { Text("e.g. Emergency Fund") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("GOAL TYPE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val savingsSelected = selectedType == GoalType.SAVINGS
                    val debtSelected = selectedType == GoalType.DEBT
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { selectedType = GoalType.SAVINGS }
                            .background(if (savingsSelected) AccentGreenLight.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (savingsSelected) AccentGreenLight else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.TrackChanges, contentDescription = null, tint = if (savingsSelected) AccentGreenLight else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Savings Goal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (savingsSelected) AccentGreenLight else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { selectedType = GoalType.DEBT }
                            .background(if (debtSelected) ExpenseRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (debtSelected) ExpenseRed else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.TrendingDown, contentDescription = null, tint = if (debtSelected) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paying Off Debt", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (debtSelected) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("TARGET AMOUNT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    placeholder = { Text("e.g. 100000") },
                    leadingIcon = { Text("KES") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("TARGET DATE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                        .clickable {
                            val calendar = Calendar.getInstance()
                            if (targetDate != null) calendar.timeInMillis = targetDate!!
                            
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(year, month, dayOfMonth)
                                    targetDate = newCal.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(16.dp)
                ) {
                    Text(if (targetDate != null) dateFormat.format(Date(targetDate!!)) else "Select Date", color = if (targetDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("MONTHLY CONTRIBUTION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer).clickable {
                        val amount = targetAmount.toDoubleOrNull()
                        if (amount != null && targetDate != null) {
                            val cal1 = Calendar.getInstance()
                            val cal2 = Calendar.getInstance()
                            cal2.timeInMillis = targetDate!!
                            
                            val diffYear = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR)
                            val diffMonth = diffYear * 12 + cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH)
                            
                            val months = if (diffMonth > 0) diffMonth else 1
                            monthlyContribution = "%.0f".format(amount / months)
                        }
                    }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text("Smart Recommendation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = monthlyContribution,
                    onValueChange = { monthlyContribution = it },
                    placeholder = { Text("How much can you set aside monthly?") },
                    leadingIcon = { Text("KES") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Text("COLOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(if (selectedColor == color) 3.dp else 0.dp, if (selectedColor == color) Color.Gray else Color.Transparent, CircleShape)
                                .clickable { selectedColor = color }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text("ICON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    icons.forEach { iconStr ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (selectedIcon == iconStr) selectedColor.copy(alpha=0.2f) else Color.Transparent)
                                .clickable { selectedIcon = iconStr },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(iconStr, fontSize = 24.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Preview Card
                val previewAmount = targetAmount.ifBlank { "0" }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = selectedColor.copy(alpha=0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, selectedColor.copy(alpha=0.3f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(selectedColor.copy(alpha=0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedIcon, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(if (goalName.isBlank()) "Goal Name" else goalName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("KES $previewAmount target", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp)) // padding for sticky footer
            }
            
            // Sticky Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val amount = targetAmount.toDoubleOrNull()
                        val monthly = monthlyContribution.toDoubleOrNull()
                        if (goalName.isNotBlank() && amount != null && targetDate != null && monthly != null) {
                            onSave(Goal(name = goalName, type = selectedType, targetAmount = amount, targetDate = targetDate!!, monthlyContribution = monthly, color = selectedColor.value.toLong()))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenDark),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = goalName.isNotBlank() && targetAmount.isNotBlank() && targetDate != null && monthlyContribution.isNotBlank()
                ) {
                    Text("Confirm & Create Goal", color = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("You can adjust these settings at any time in your goal dashboard.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal, onAddContribution: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    val formattedDate = dateFormat.format(java.util.Date(goal.targetDate))

    val tagText = if (goal.type == GoalType.SAVINGS) "SAVINGS" else "DEBT"
    val subtitleText = if (goal.type == GoalType.SAVINGS) "Savings Goal" else "Paying Off Debt"
    val savedAmount = goal.savedAmount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(value = goal.color.toULong()))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(goal.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(subtitleText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val typeColor = if (goal.type == GoalType.SAVINGS) IncomeGreen else ExpenseRed
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(tagText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = typeColor, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val typeColor = if (goal.type == GoalType.SAVINGS) IncomeGreen else ExpenseRed

            // Progress Bar
            val progress = if (goal.targetAmount > 0) (savedAmount / goal.targetAmount).toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = typeColor,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amounts Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saved: KES ${"%,.2f".format(savedAmount)}", style = MaterialTheme.typography.bodySmall, color = typeColor, fontWeight = FontWeight.SemiBold)
                Text("Target: KES ${"%,.2f".format(goal.targetAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // On-track status indicator — icon + text + color (not color alone)
            val now = System.currentTimeMillis()
            val totalDuration = (goal.targetDate - 0L).coerceAtLeast(1L)
            val elapsed = (now - 0L).coerceAtLeast(0L)
            val expectedProgress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
            val actualProgress = (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
            val isOnTrack = actualProgress >= expectedProgress
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isOnTrack) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = if (isOnTrack) "On track" else "Falling behind",
                    tint = if (isOnTrack) AccentGreenLight else ExpenseRed,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isOnTrack) "On track" else "Falling behind",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnTrack) AccentGreenLight else ExpenseRed,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Footer Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = "Deadline", tint = typeColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deadline: $formattedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("KES ${"%,.2f".format(goal.monthlyContribution)}/month", style = MaterialTheme.typography.bodySmall, color = typeColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onAddContribution,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreenDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreenDark)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contribute", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete goal", tint = ExpenseRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

