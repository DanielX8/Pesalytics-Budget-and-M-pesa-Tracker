package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Goal
import com.example.model.GoalType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialGoalsScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit
) {
    val goals by viewModel.goals.collectAsState()
    var showCreateGoalSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text("PesaSense", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
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
                                    .background(Color(0xFF0F5B1A)) // Deep Emerald
                                    .clickable { showCreateGoalSheet = true },
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
                                tint = Color(0xFF0F5B1A) // Deep Emerald green
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5B1A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Goal", maxLines = 1, color = Color.White)
                        }
                    }
                }
            } else {
                // Populated State
                goals.forEach { goal ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(goal.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Target: KES ${goal.targetAmount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            // More details based on actual progress
                        }
                    }
                }
            }
        }
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
                    Text("Create Goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF0F5B1A))
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
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedType = GoalType.SAVINGS }
                            .background(if (savingsSelected) Color(0xFF0F5B1A).copy(alpha = 0.1f) else Color.Transparent)
                            .border(1.dp, if (savingsSelected) Color(0xFF0F5B1A) else Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Savings Goal", color = if (savingsSelected) Color(0xFF0F5B1A) else MaterialTheme.colorScheme.onSurface)
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedType = GoalType.DEBT }
                            .background(if (debtSelected) ExpenseRed.copy(alpha = 0.1f) else Color.Transparent)
                            .border(1.dp, if (debtSelected) ExpenseRed else Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Paying Off Debt", color = if (debtSelected) ExpenseRed else MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("TARGET AMOUNT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
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
                    }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("Smart Rec", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = monthlyContribution,
                    onValueChange = { monthlyContribution = it },
                    leadingIcon = { Text("KES") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                val monthlyValue = monthlyContribution.toDoubleOrNull() ?: 0.0
                val targetValue = targetAmount.toDoubleOrNull() ?: 0.0
                
                if (monthlyValue > 0 && targetValue > 0 && targetDate != null) {
                    val cal1 = Calendar.getInstance()
                    val cal2 = Calendar.getInstance()
                    cal2.timeInMillis = targetDate!!
                    
                    val diffYear = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR)
                    val diffMonth = diffYear * 12 + cal2.get(Calendar.MONTH) - cal1.get(Calendar.MONTH)
                    val monthsRemaining = if (diffMonth > 0) diffMonth else 1
                    
                    val requiredMonthly = targetValue / monthsRemaining
                    
                    val isOnTrackOrAhead = monthlyValue >= requiredMonthly
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if(isOnTrackOrAhead) IncomeGreen.copy(alpha=0.1f) else ExpenseRed.copy(alpha=0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = if(isOnTrackOrAhead) IncomeGreen else ExpenseRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PesaSense Insight", fontWeight = FontWeight.Bold, color = if(isOnTrackOrAhead) IncomeGreen else ExpenseRed, style = MaterialTheme.typography.labelMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isOnTrackOrAhead) {
                                val remainingMonths = Math.ceil(targetValue / monthlyValue).toInt()
                                val earlier = monthsRemaining - remainingMonths
                                val earlierText = if (earlier > 0) "you will reach your goal $earlier months earlier than your target date!" else "you are exactly on track to meet your target date."
                                Text("At KES $monthlyValue monthly, $earlierText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            } else {
                                val newAmount = "%.0f".format(targetValue / monthsRemaining)
                                Text("At KES $monthlyValue monthly, you will fall short. Consider increasing to KES $newAmount or extending your target date.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (isOnTrackOrAhead) 1f else (monthlyValue / requiredMonthly).toFloat() },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                color = if(isOnTrackOrAhead) IncomeGreen else ExpenseRed,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
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
                            onSave(Goal(name = goalName, type = selectedType, targetAmount = amount, targetDate = targetDate!!, monthlyContribution = monthly))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5B1A)),
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
