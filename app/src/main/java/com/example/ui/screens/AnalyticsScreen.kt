package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TransactionType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: PesaViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    var monthOffset by remember { mutableStateOf(0) } // 0 = current month, -1 = last, etc.

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, monthOffset)
    val displayMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    
    // Filter transactions for this month
    val currentMonthStart = calendar.clone() as Calendar
    currentMonthStart.set(Calendar.DAY_OF_MONTH, 1)
    currentMonthStart.set(Calendar.HOUR_OF_DAY, 0)
    currentMonthStart.set(Calendar.MINUTE, 0)
    currentMonthStart.set(Calendar.SECOND, 0)
    currentMonthStart.set(Calendar.MILLISECOND, 0)
    val startTimestamp = currentMonthStart.timeInMillis
    
    currentMonthStart.add(Calendar.MONTH, 1)
    val endTimestamp = currentMonthStart.timeInMillis

    val monthTransactions = uiState.transactions.filter { it.timestamp in startTimestamp until endTimestamp && !it.isFeeTransaction }

    val totalIncome = monthTransactions.filter { it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME }.sumOf { it.amount }
    val totalExpense = monthTransactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }.sumOf { it.amount }
    val totalSaved = totalIncome - totalExpense
    val totalFeesPaid = monthTransactions.sumOf { it.fee }
    val totalOverdraftDebt = monthTransactions.sumOf { it.usedFulizaAmount }

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
                        Box {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { notificationsExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", modifier = Modifier.size(20.dp))
                                if (notifications.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseRed)
                                    )
                                }
                            }
                            
                            DropdownMenu(
                                expanded = notificationsExpanded,
                                onDismissRequest = { notificationsExpanded = false },
                                modifier = Modifier.width(280.dp),
                                shape = RoundedCornerShape(16.dp),
                                containerColor = MaterialTheme.colorScheme.surface,
                                shadowElevation = 8.dp
                            ) {
                                if (notifications.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No new notifications", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                        onClick = { }
                                    )
                                } else {
                                    notifications.forEach { notif ->
                                        DropdownMenuItem(
                                            text = { Text(notif.message, style = MaterialTheme.typography.bodyMedium) },
                                            onClick = { viewModel.dismissNotification(notif.id) }
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    DropdownMenuItem(
                                        text = { Text("Clear All", style = MaterialTheme.typography.bodyMedium, color = ExpenseRed) },
                                        onClick = { viewModel.clearNotifications() }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                Text("ANALYTICS DASHBOARD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Month Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { monthOffset -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    Text(displayMonth, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { monthOffset += 1 }, enabled = monthOffset < 0) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            item {
                // Summary Cards
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("INCOME", formatCurrency(totalIncome), AccentGreenLight, Modifier.weight(1f))
                        SummaryCard("EXPENSES", formatCurrency(totalExpense), ExpenseRed, Modifier.weight(1f))
                        SummaryCard("SAVED", formatCurrency(totalSaved), TransferBlue, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("TOTAL FEES", formatCurrency(totalFeesPaid), WarningOrange, Modifier.weight(1f))
                        SummaryCard("OVERDRAFT", formatCurrency(totalOverdraftDebt), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    }
                }
            }

            item {
                WhereItGoesChart(monthTransactions)
            }
            
            item {
                TransactionFeesCard(monthTransactions)
            }
            
            item {
                SpendingRhythmChart(monthTransactions)
            }
            
            item {
                NeedsVsWantsCard(monthTransactions)
            }

            item {
                SpendingCalendar(monthTransactions, startTimestamp)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text("KES", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp)
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun WhereItGoesChart(transactions: List<com.example.model.Transaction>) {
    val expenses = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME && !it.isFeeTransaction }
    val totalExpense = expenses.sumOf { it.amount }
    
    // Group by Tier 1 M-PESA transaction type
    val typeLabels = mapOf(
        TransactionType.SEND_MONEY to "Send Money",
        TransactionType.PAYBILL to "Paybill",
        TransactionType.BUY_GOODS to "Buy Goods",
        TransactionType.WITHDRAW to "Withdraw",
        TransactionType.AIRTIME to "Airtime",
        TransactionType.MANUAL_EXPENSE to "Other"
    )
    val typeTotals = expenses.groupBy { typeLabels[it.type] ?: "Other" }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList()
        .sortedByDescending { it.second }
    
    // Assign colors
    val colors = listOf(ExpenseRed, TransferBlue, AccentGreenLight, AccentGreenDark, Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4))
    
    var selectedCategory by remember { mutableStateOf<Pair<String, Double>?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Where It Goes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (totalExpense == 0.0) {
                Text("No expenses this month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            // Animate donut chart sweep on entry
            val animatedSweep = remember { Animatable(0f) }
            LaunchedEffect(typeTotals) {
                animatedSweep.snapTo(0f)
                animatedSweep.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
                )
            }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(220.dp)) {
                Canvas(modifier = Modifier.size(200.dp).pointerInput(typeTotals) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        var angle = Math.toDegrees(atan2((offset.y - center.y).toDouble(), (offset.x - center.x).toDouble())).toFloat()
                        if (angle < 0) angle += 360f
                        
                        var currentAngle = 270f
                        for ((index, item) in typeTotals.withIndex()) {
                            val sweep = ((item.second / totalExpense) * 360).toFloat()
                            var maxAngle = currentAngle + sweep
                            if (maxAngle > 360f) maxAngle -= 360f
                            
                            val startNorm = if (currentAngle >= 360f) currentAngle - 360f else currentAngle
                            
                            var isInSlice = false
                            if (startNorm < maxAngle) {
                                isInSlice = angle in startNorm..maxAngle
                            } else {
                                isInSlice = angle >= startNorm || angle <= maxAngle
                            }
                            
                            if (isInSlice) {
                                selectedCategory = item
                                break
                            }
                            currentAngle += sweep
                        }
                    }
                }) {
                    val totalAnimatedSweep = 360f * animatedSweep.value
                    var currentStartAngle = 270f
                    var consumedSweep = 0f
                    typeTotals.forEachIndexed { index, pair ->
                        val fullSweepAngle = ((pair.second / totalExpense) * 360).toFloat()
                        val remaining = totalAnimatedSweep - consumedSweep
                        val actualSweep = fullSweepAngle.coerceAtMost(remaining.coerceAtLeast(0f))
                        if (actualSweep > 0f) {
                            drawArc(
                                color = colors[index % colors.size],
                                startAngle = currentStartAngle,
                                sweepAngle = actualSweep,
                                useCenter = false,
                                style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Butt),
                                size = Size(size.width, size.height)
                            )
                        }
                        currentStartAngle += fullSweepAngle
                        consumedSweep += fullSweepAngle
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    if (selectedCategory != null) {
                        Text(selectedCategory!!.first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("KES ${formatCurrency(selectedCategory!!.second)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("TAP SLICE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Legend
            typeTotals.forEachIndexed { index, pair ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(colors[index % colors.size]))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pair.first, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("${((pair.second / totalExpense) * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            
            if (selectedCategory != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(selectedCategory!!.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("KES ${formatCurrency(selectedCategory!!.second)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${((selectedCategory!!.second / totalExpense) * 100).toInt()}% of expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionFeesCard(transactions: List<com.example.model.Transaction>) {
    // Only use the fee field on main (non-fee-split) transactions
    val mainTransactions = transactions.filter { !it.isFeeTransaction }
    val feeBreakdown = mainTransactions.filter { it.fee > 0 }
        .groupBy { it.type }
        .mapValues { entry -> entry.value.sumOf { it.fee } }
    
    val totalFees = feeBreakdown.values.sum()
    val totalExpense = mainTransactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }.sumOf { it.amount }
    val percentage = if (totalExpense > 0) (totalFees / totalExpense) * 100 else 0.0

    val typeLabels = mapOf(
        TransactionType.PAYBILL to "Paybill",
        TransactionType.BUY_GOODS to "Till",
        TransactionType.WITHDRAW to "Withdraw",
        TransactionType.SEND_MONEY to "Send Money",
        TransactionType.AIRTIME to "Airtime"
    )

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Transaction Fees", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("M-PESA charges this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(
                        text = "KES ${formatCurrency(totalFees)}", 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = ExpenseRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total fees paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", percentage)}%", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("of expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Fee breakdown per transaction type
            val orderedTypes = listOf(TransactionType.PAYBILL, TransactionType.BUY_GOODS, TransactionType.WITHDRAW, TransactionType.SEND_MONEY, TransactionType.AIRTIME)
            orderedTypes.forEach { type ->
                val fee = feeBreakdown[type] ?: 0.0
                if (fee > 0) {
                    val label = typeLabels[type] ?: type.name
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("KES ${formatCurrency(fee)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingRhythmChart(transactions: List<com.example.model.Transaction>) {
    val expenses = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
    
    // Last 7 days ending today
    val last7Days = (6 downTo 0).map { offset ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -offset)
        cal
    }
    
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    
    val rhythmData = last7Days.map { dayCal ->
        val daySpend = expenses.filter { 
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
        Pair(dayFormat.format(dayCal.time), daySpend)
    }
    
    val maxSpend = rhythmData.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spending Rhythm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Last 7 Days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                rhythmData.forEachIndexed { index, (day, amount) ->
                    val isToday = index == 6 // 6 is the last element because it's 6 downTo 0
                    val proportion = (amount / maxSpend).toFloat()
                    val barColor = if (isToday) TransferBlue.copy(alpha = 0.8f) else TransferBlue.copy(alpha = 0.3f)
                    val labelColor = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                        if (amount > 0) {
                            Text(
                                text = if (amount >= 1000) String.format(java.util.Locale.US, "%.1fk", amount / 1000.0) else amount.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(proportion.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(day, style = MaterialTheme.typography.labelSmall, color = labelColor, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun NeedsVsWantsCard(transactions: List<com.example.model.Transaction>) {
    val expenses = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
    
    // Naive category mapping for mockup
    val needsKeywords = listOf("Rent", "Utilities", "Groceries", "Transport", "Bills", "Health", "KPLC", "Water")
    
    var needsAmount = 0.0
    var wantsAmount = 0.0
    
    expenses.forEach { t ->
        if (needsKeywords.any { t.category.contains(it, ignoreCase = true) }) {
            needsAmount += t.amount
        } else {
            wantsAmount += t.amount
        }
    }
    
    val total = needsAmount + wantsAmount
    val needsPercent = if (total > 0) (needsAmount / total).toFloat() else 0.5f

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Needs vs Wants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (total == 0.0) {
                Text("No data to split.", style = MaterialTheme.typography.bodySmall)
                return@Column
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(needsPercent.coerceAtLeast(0.01f)).fillMaxHeight().background(AccentGreenLight))
                    Box(modifier = Modifier.weight((1f - needsPercent).coerceAtLeast(0.01f)).fillMaxHeight().background(ExpenseRed))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("NEEDS", style = MaterialTheme.typography.labelSmall, color = AccentGreenLight, fontWeight = FontWeight.Bold)
                    Text("${(needsPercent * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("KES ${formatCurrency(needsAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("WANTS", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, fontWeight = FontWeight.Bold)
                    Text("${((1f - needsPercent) * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("KES ${formatCurrency(wantsAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SpendingCalendar(transactions: List<com.example.model.Transaction>, monthStartTimestamp: Long) {
    val expenses = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
    
    val cal = Calendar.getInstance()
    cal.timeInMillis = monthStartTimestamp
    
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday
    
    // Pre-calculate spend per day (1-indexed)
    val spendMap = mutableMapOf<Int, Double>()
    for (i in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, i)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis
        
        val amt = expenses.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.amount }
        spendMap[i] = amt
    }
    
    val maxDailySpend = spendMap.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spending Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Days of week header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { 
                    Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // Grid
            var currentDay = 1
            // Compute offset for the first week (firstDayOfWeek is 1 for Sunday, which matches our array)
            val offset = firstDayOfWeek - 1
            
            val totalCells = Math.ceil((daysInMonth + offset) / 7.0).toInt() * 7
            
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0..6) {
                        if (row == 0 && col < offset) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else if (currentDay <= daysInMonth) {
                            val amount = spendMap[currentDay] ?: 0.0
                            val intensity = if (maxDailySpend > 0) (amount / maxDailySpend).toFloat() else 0f
                            
                            val cellColor = if (amount == 0.0) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // No Spend Grey/Blue
                            } else {
                                // Interpolate green
                                androidx.compose.ui.graphics.lerp(Color(0xFFA5D6A7), Color(0xFF1B5E20), intensity)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentDay.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (amount == 0.0) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                )
                            }
                            currentDay++
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Low", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFA5D6A7)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF4CAF50)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF1B5E20)))
                Spacer(modifier = Modifier.width(8.dp))
                Text("High", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

