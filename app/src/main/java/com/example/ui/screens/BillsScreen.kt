package com.pesalytics.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesalytics.R
import com.pesalytics.model.Bill
import com.pesalytics.model.BillCycle
import com.pesalytics.ui.theme.AccentGreenLight
import com.pesalytics.ui.theme.ExpenseRed
import com.pesalytics.ui.theme.WarningOrange
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

    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfWeek = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, 7)
    val endOfWeek = calendar.timeInMillis

    // Only count bills that are still unpaid — so "Mark as Paid" immediately
    // deducts from the displayed total, giving the user accurate feedback.
    val dueThisWeek = bills
        .filter { !it.isPaid && it.nextDueDate in startOfWeek..endOfWeek }
        .sumOf { it.amount }

    if (showAddBillDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddBillDialog = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
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
            dragHandle = { BottomSheetDefaults.DragHandle() }
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
                    onDismiss = { isEditingBill = false }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (selectedBill!!.isPaid) "Paid" else "Unpaid",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedBill!!.isPaid) AccentGreenLight else ExpenseRed
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Mark as Paid button (only if not already paid)
                    if (!selectedBill!!.isPaid) {
                        Button(
                            onClick = {
                                viewModel.markBillAsPaid(selectedBill!!)
                                coroutineScope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Paid", color = Color.White)
                        }
                    }
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
                            Text("Remove", color = Color.White)
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
                        Image(
                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.header_logo),
                            contentDescription = "Pesalytics",
                            modifier = Modifier.height(32.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
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
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Bills Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Manage and track your upcoming bills.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            item {
                val totalBillsAmount = bills.sumOf { it.amount }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Due This Week", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("KES ${formatCurrency(dueThisWeek)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Bills", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("KES ${formatCurrency(totalBillsAmount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Text("Upcoming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

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
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No bills tracked yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap + to add a recurring bill", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            bills.sortedBy { it.nextDueDate }.forEach { bill ->
                                BillItem(bill = bill, onClick = {
                                    selectedBill = bill
                                    showBottomSheet = true
                                })
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
    var isAutoPay by remember { mutableStateOf(initialBill?.isAutoPay ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Auto-set a sensible due date when cycle changes (new bills only)
    LaunchedEffect(cycle) {
        if (initialBill == null) {
            val cal = Calendar.getInstance().apply {
                when (cycle) {
                    BillCycle.DAILY -> add(Calendar.DAY_OF_YEAR, 1)
                    BillCycle.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                    BillCycle.MONTHLY -> add(Calendar.MONTH, 1)
                    BillCycle.YEARLY -> add(Calendar.YEAR, 1)
                }
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            nextDue = cal.timeInMillis
        }
    }

    val icons = listOf(Icons.Default.Phone, Icons.Default.Home, Icons.Default.Payment, Icons.Default.Movie, Icons.Default.ShoppingCart)
    var selectedIcon by remember { mutableStateOf(icons[0]) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (initialBill != null) "Edit Recurring Bill" else "Add Recurring Bill", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(icons) { icon ->
                val isSelected = icon == selectedIcon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) AccentGreenLight else Color.Transparent, CircleShape)
                        .clickable { selectedIcon = icon },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = if (isSelected) AccentGreenLight else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("BILL NAME", style = MaterialTheme.typography.labelSmall) }, placeholder = { Text("e.g. Zuku Internet") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("AMOUNT (KES)", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = payee, onValueChange = { payee = it }, label = { Text("PAYBILL / PAYEE", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        Spacer(modifier = Modifier.height(24.dp))
        Text("BILLING CYCLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        val cycleAccent = interactiveGreen
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.SpaceEvenly) {
            BillCycle.entries.forEach { option ->
                val isSelected = cycle == option
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) cycleAccent else Color.Transparent).clickable { cycle = option }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(option.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Auto Pay toggle
        val autoPayAccent = interactiveGreen
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Auto Pay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Advance due date automatically when paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = isAutoPay,
                onCheckedChange = { isAutoPay = it },
                colors = SwitchDefaults.colors(checkedThumbColor = autoPayAccent, checkedTrackColor = autoPayAccent.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = dateFormatter.format(java.util.Date(nextDue)),
                onValueChange = {},
                readOnly = true,
                label = { Text("NEXT DUE DATE", style = MaterialTheme.typography.labelSmall) },
                trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, "Pick Date") } },
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true }.background(Color.Transparent))
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextDue)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { nextDue = it }; showDatePicker = false }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
            ) { DatePicker(state = datePickerState) }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull()
                if (name.isNotBlank() && parsedAmount != null && payee.isNotBlank()) {
                    onAddBill(name, parsedAmount, payee, cycle, nextDue, isAutoPay)
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
fun BillItem(bill: Bill, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val hoursToDue = (bill.nextDueDate - now) / (1000 * 60 * 60)
    val daysToDue = maxOf(0L, hoursToDue / 24)

    val dueDateStr = when {
        bill.isPaid -> "Paid"
        hoursToDue < 0 -> "Overdue"
        daysToDue == 0L -> "Due today"
        else -> "In $daysToDue days"
    }

    val statusColor = when {
        bill.isPaid -> AccentGreenLight
        hoursToDue < 0 -> ExpenseRed
        daysToDue <= 3 -> ExpenseRed
        else -> WarningOrange
    }

    val iconImage = when {
        bill.name.contains("TV", ignoreCase = true) -> Icons.Default.Tv
        bill.name.contains("Electricity", ignoreCase = true) || bill.name.contains("KPLC", ignoreCase = true) -> Icons.Default.Bolt
        bill.name.contains("Fiber", ignoreCase = true) || bill.name.contains("Internet", ignoreCase = true) || bill.name.contains("Zuku", ignoreCase = true) -> Icons.Default.Wifi
        else -> Icons.AutoMirrored.Filled.ReceiptLong
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
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = if (bill.isPaid) Icons.Default.CheckCircle else iconImage, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = bill.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Paybill ${bill.payee}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(text = bill.cycle.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = "KES ${formatCurrency(bill.amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(text = dueDateStr, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
            // Urgency indicator — icon + text (not color alone)
            val isOverdue = hoursToDue < 0 && !bill.isPaid
            val isUrgent = hoursToDue in 0..48 && !bill.isPaid
            if (isUrgent || isOverdue) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (isOverdue) Icons.Default.Error else Icons.Default.Warning,
                        contentDescription = if (isOverdue) "Overdue" else "Urgent",
                        tint = if (isOverdue) ExpenseRed else WarningOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isOverdue) "Overdue" else "Due soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) ExpenseRed else WarningOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
