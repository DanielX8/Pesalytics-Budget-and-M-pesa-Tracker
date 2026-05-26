package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Bill
import com.example.model.BillCycle
import com.example.ui.theme.AccentGreenLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(viewModel: PesaViewModel, onNavigateBack: () -> Unit) {
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    var showAddBillDialog by remember { mutableStateOf(false) }
    var selectedBill by remember { mutableStateOf<Bill?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val now = System.currentTimeMillis()
    val sevenDaysFromNow = now + 7 * 24 * 60 * 60 * 1000L

    val dueThisWeek = bills.filter { it.nextDueDate in now..sevenDaysFromNow }.sumOf { it.amount }

    if (showAddBillDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddBillDialog = false },
            sheetState = sheetState,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
        ) {
            AddBillBottomSheetContent(
                onAddBill = { name, amount, payee, cycle, nextDue, isAutoPay ->
                    viewModel.addBill(Bill(name = name, amount = amount, payee = payee, cycle = cycle, nextDueDate = nextDue, isAutoPay = isAutoPay))
                    coroutineScope.launch {
                        sheetState.hide()
                        showAddBillDialog = false
                    }
                },
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showAddBillDialog = false
                    }
                }
            )
        }
    }

    var isEditingBill by remember { mutableStateOf(false) }

    if (showBottomSheet && selectedBill != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() }
        ) {
            if (isEditingBill) {
                AddBillBottomSheetContent(
                    initialBill = selectedBill,
                    onAddBill = { name, amount, payee, cycle, nextDue, isAutoPay ->
                        val updated = selectedBill!!.copy(name = name, amount = amount, payee = payee, cycle = cycle, nextDueDate = nextDue, isAutoPay = isAutoPay)
                        viewModel.updateBill(updated)
                        coroutineScope.launch {
                            sheetState.hide()
                            showBottomSheet = false
                            isEditingBill = false
                        }
                    },
                    onDismiss = {
                        isEditingBill = false
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    Text(selectedBill!!.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount:", style = MaterialTheme.typography.bodyMedium)
                        Text("KES ${formatCurrency(selectedBill!!.amount)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Paybill/Till:", style = MaterialTheme.typography.bodyMedium)
                        Text(selectedBill!!.payee, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Billing Cycle:", style = MaterialTheme.typography.bodyMedium)
                        Text(selectedBill!!.cycle.name, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(onClick = { isEditingBill = true }, modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Edit")
                        }
                        Button(
                            onClick = { 
                                viewModel.deleteBill(selectedBill!!)
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                }
                            }, 
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                        ) {
                            Text("Remove", color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
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
                        IconButton(onClick = { showAddBillDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Bill", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                Text("BILLS TRACKER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Due This Week", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("KES ${formatCurrency(dueThisWeek)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item {
                Text("Upcoming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // AnimatedContent: crossfade between empty state and bill list
            item {
                AnimatedContent(
                    targetState = bills.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                    },
                    label = "BillsListState"
                ) { isEmpty ->
                    if (isEmpty) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "No bills tracked yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tap + to add a recurring bill",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            bills.sortedBy { it.nextDueDate }.forEach { bill ->
                                BillItem(
                                    bill = bill,
                                    onClick = {
                                        selectedBill = bill
                                        showBottomSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillBottomSheetContent(
    initialBill: Bill? = null,
    onAddBill: (String, Double, String, BillCycle, Long, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialBill?.name ?: "") }
    var amount by remember { mutableStateOf(initialBill?.amount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var payee by remember { mutableStateOf(initialBill?.payee ?: "") }
    var cycle by remember { mutableStateOf(initialBill?.cycle ?: BillCycle.MONTHLY) }
    var nextDue by remember { mutableStateOf(initialBill?.nextDueDate ?: System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }

    val icons = listOf(Icons.Default.Phone, Icons.Default.Home, Icons.Default.Payment, Icons.Default.Movie, Icons.Default.ShoppingCart)
    var selectedIcon by remember { mutableStateOf(icons[0]) }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (initialBill != null) "Edit Recurring Bill" else "Add Recurring Bill", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Icon Selector
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(icons) { icon ->
                val isSelected = icon == selectedIcon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) AccentGreenLight else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                        .clickable { selectedIcon = icon },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = if (isSelected) AccentGreenLight else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("BILL NAME", style = MaterialTheme.typography.labelSmall) },
            placeholder = { Text("e.g. Zuku Internet") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("AMOUNT (KES)", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = payee,
            onValueChange = { payee = it },
            label = { Text("PAYBILL / PAYEE", style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("BILLING CYCLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.SpaceEvenly) {
            BillCycle.entries.forEach { option ->
                val isSelected = cycle == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentGreenLight else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { cycle = option }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = dateFormatter.format(java.util.Date(nextDue)),
            onValueChange = { },
            readOnly = true,
            label = { Text("NEXT DUE DATE", style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { Icon(Icons.Default.DateRange, "Pick Date") },
            modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                detectTapGestures(onTap = { showDatePicker = true })
            }
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextDue)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { nextDue = it }
                        showDatePicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull()
                if (name.isNotBlank() && parsedAmount != null && payee.isNotBlank()) {
                    onAddBill(name, parsedAmount, payee, cycle, nextDue, initialBill?.isAutoPay ?: false)
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight),
            shape = RoundedCornerShape(24.dp),
            enabled = name.isNotBlank() && amount.toDoubleOrNull() != null && payee.isNotBlank()
        ) {
            Text(if (initialBill != null) "Save Bill" else "Add Bill", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BillItem(
    bill: Bill,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val hoursToDue = (bill.nextDueDate - now) / (1000 * 60 * 60)
    
    val isUrgent = !bill.isAutoPay && hoursToDue in 0..48
    
    val daysToDue = hoursToDue / 24
    
    val dueDateStr = when {
        bill.isAutoPay -> "Auto-Pays in ${Math.max(0, daysToDue)} days"
        hoursToDue < 0 -> "Overdue"
        hoursToDue < 24 -> "Due Tomorrow"
        hoursToDue <= 48 -> "Due in 2 days"
        else -> "Due in $daysToDue days"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBgColor = if (isUrgent) ExpenseRed.copy(alpha = 0.2f) else if (bill.isAutoPay) AccentGreenLight.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        val iconTint = if (isUrgent) ExpenseRed else if (bill.isAutoPay) AccentGreenLight else MaterialTheme.colorScheme.onSurfaceVariant

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isUrgent) Icons.Default.Warning else if (bill.isAutoPay) Icons.Default.CheckCircle else Icons.Default.CheckCircle, 
                contentDescription = null, 
                tint = iconTint
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = bill.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            if (isUrgent) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ExpenseRed.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Urgent", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(text = bill.payee, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = "KES ${formatCurrency(bill.amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isUrgent) ExpenseRed else MaterialTheme.colorScheme.onSurface)
            Text(text = dueDateStr, style = MaterialTheme.typography.bodySmall, color = if (isUrgent) ExpenseRed else if (bill.isAutoPay) AccentGreenLight else WarningOrange)
        }
    }
}
