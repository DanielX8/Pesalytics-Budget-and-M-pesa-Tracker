package com.pesalytics.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesalytics.R
import com.pesalytics.model.Budget
import com.pesalytics.model.TransactionType
import com.pesalytics.patterns.CategoryDelta
import com.pesalytics.patterns.FulizaMonthPoint
import com.pesalytics.patterns.MonthComparison
import com.pesalytics.patterns.SpendVelocity
import com.pesalytics.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToNeedsWants: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val patternResult by viewModel.patternResult.collectAsStateWithLifecycle()
    val needsWantsClassification by viewModel.needsWantsClassification.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val selectedMonthIndex by viewModel.selectedMonthIndex.collectAsStateWithLifecycle()
    val currentCalendarMonth = Calendar.getInstance().get(Calendar.MONTH)

    // Use the ViewModel's year-aware currentMonthStart so December in January
    // correctly resolves to the previous year rather than a future month.
    val startTimestamp by viewModel.currentMonthStart.collectAsStateWithLifecycle()
    val endTimestamp = Calendar.getInstance().apply {
        timeInMillis = startTimestamp
        add(Calendar.MONTH, 1)
    }.timeInMillis
    val displayMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startTimestamp))

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
                        Box {
                            Box(
                                modifier = Modifier.padding(end = 16.dp).size(40.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { notificationsExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", modifier = Modifier.size(20.dp))
                                if (notifications.isNotEmpty()) {
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).clip(CircleShape).background(ExpenseRed))
                                }
                            }
                            DropdownMenu(expanded = notificationsExpanded, onDismissRequest = { notificationsExpanded = false }, modifier = Modifier.width(280.dp), shape = RoundedCornerShape(16.dp), containerColor = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
                                if (notifications.isEmpty()) {
                                    DropdownMenuItem(text = { Text("No new notifications", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = {})
                                } else {
                                    notifications.forEach { notif ->
                                        DropdownMenuItem(text = { Text(notif.message, style = MaterialTheme.typography.bodyMedium) }, onClick = { viewModel.dismissNotification(notif.id) })
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    DropdownMenuItem(text = { Text("Clear All", style = MaterialTheme.typography.bodyMedium, color = ExpenseRed) }, onClick = { viewModel.clearNotifications() })
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!isPremium) {
            AnalyticsLockedPreview(
                onUpgrade = onNavigateToSubscription,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analytics Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Visualize your spending habits.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 4.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setSelectedMonth((selectedMonthIndex - 1).coerceAtLeast(0)) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                    }
                    Text(displayMonth, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { viewModel.setSelectedMonth((selectedMonthIndex + 1).coerceAtMost(currentCalendarMonth)) },
                        enabled = selectedMonthIndex < currentCalendarMonth
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                    }
                }
            }

            item {
                androidx.compose.animation.AnimatedContent(
                    targetState = selectedMonthIndex,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
                            androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(300))) togetherWith
                            (androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) +
                                androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(200)))
                    },
                    label = "MonthContentTransition"
                ) { _ ->
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
            }

            // Spend Velocity Banner
            patternResult?.spendVelocity?.let { velocity ->
                item { SpendVelocityBanner(velocity, uiState.transactions) }
            }

            // Month Comparison Card
            patternResult?.monthComparison?.let { comparison ->
                item { MonthComparisonCard(comparison) }
            }

            item { BalanceProgressionChart(monthTransactions, startTimestamp) }

            item {
                WhereItGoesChart(
                    transactions = monthTransactions,
                    categoryDeltas = patternResult?.categoryDeltas ?: emptyList()
                )
            }

            item { LargestTransactionsCard(monthTransactions) }

            item { TransactionFeesCard(monthTransactions) }

            item { TopPayeesCard(monthTransactions) }
            item { IncomeSourcesCard(monthTransactions) }

            val budgetsForCard = uiState.budgets
            if (budgetsForCard.isNotEmpty()) {
                item { BudgetVsActualCard(budgetsForCard, uiState.categorySpent) }
            }

            item {
                Text("PATTERNS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 0.dp))
            }

            item { SpendingRhythmChart(monthTransactions) }
            item { NeedsVsWantsCard(monthTransactions, needsWantsClassification, onNavigateToNeedsWants) }
            item {
                SpendingCalendar(monthTransactions, startTimestamp, displayMonth)
            }

            // Trends section
            val fulizaTrend = patternResult?.fulizaTrend ?: emptyList()
            if (fulizaTrend.isNotEmpty()) {
                item {
                    Text("TRENDS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                }
                item { FulizaTrendCard(fulizaTrend) }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SpendVelocityBanner(velocity: SpendVelocity, allTransactions: List<com.pesalytics.model.Transaction>) {
    val currentMonthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val prevMonthStart = remember(currentMonthStart) {
        Calendar.getInstance().apply { timeInMillis = currentMonthStart; add(Calendar.MONTH, -1) }.timeInMillis
    }
    val daysInPrevMonth = remember(prevMonthStart) {
        Calendar.getInstance().apply { timeInMillis = prevMonthStart }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun dailySpendForMonth(monthStart: Long, daysInMonth: Int): List<Double> = (1..daysInMonth).map { day ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = monthStart
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val s = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        allTransactions.filter {
            !it.isFeeTransaction &&
            it.type != TransactionType.RECEIVE_MONEY &&
            it.type != TransactionType.MANUAL_INCOME &&
            it.timestamp in s..cal.timeInMillis
        }.sumOf { it.amount }
    }

    val currentDailySpend = remember(allTransactions, currentMonthStart, velocity.daysInMonth) {
        dailySpendForMonth(currentMonthStart, velocity.daysInMonth)
    }
    val prevDailySpend = remember(allTransactions, prevMonthStart, daysInPrevMonth) {
        dailySpendForMonth(prevMonthStart, daysInPrevMonth)
    }
    val maxY = remember(currentDailySpend, prevDailySpend) {
        maxOf(currentDailySpend.maxOrNull() ?: 0.0, prevDailySpend.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    }
    val currMonthLabel = remember(currentMonthStart) { SimpleDateFormat("MMM", Locale.getDefault()).format(Date(currentMonthStart)) }
    val prevMonthLabel = remember(prevMonthStart) { SimpleDateFormat("MMM", Locale.getDefault()).format(Date(prevMonthStart)) }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spend Velocity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Day ${velocity.daysElapsed} of ${velocity.daysInMonth}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentGreenLight))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(currMonthLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TransferBlue.copy(alpha = 0.65f)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(prevMonthLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val w = size.width; val h = size.height

                // Faint horizontal guide lines
                repeat(3) { i ->
                    val gy = h * (1f - (i + 1) / 4f)
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                }

                // Previous month line (blue)
                val prevStep = if (daysInPrevMonth > 1) w / (daysInPrevMonth - 1).toFloat() else w
                if (prevDailySpend.isNotEmpty()) {
                    val path = Path()
                    prevDailySpend.forEachIndexed { i, amt ->
                        val x = i * prevStep
                        val y = h - (amt / maxY * h).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, TransferBlue.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }

                // Current month line (green) — only up to today
                val currStep = if (velocity.daysInMonth > 1) w / (velocity.daysInMonth - 1).toFloat() else w
                val activeDays = currentDailySpend.take(velocity.daysElapsed)
                if (activeDays.isNotEmpty()) {
                    val path = Path()
                    activeDays.forEachIndexed { i, amt ->
                        val x = i * currStep
                        val y = h - (amt / maxY * h).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, AccentGreenLight, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                    // Today marker dot
                    val lastX = (activeDays.size - 1) * currStep
                    val lastY = h - (activeDays.last() / maxY * h).toFloat()
                    drawCircle(AccentGreenLight, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats footer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Daily Avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KES ${formatCurrency(velocity.dailyAverage)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Projected Month-End", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "KES ${formatCurrency(velocity.projectedMonthEnd)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (velocity.projectedMonthEnd > velocity.dailyAverage * velocity.daysInMonth * 1.1) ExpenseRed else AccentGreenLight
                    )
                }
            }
        }
    }
}

@Composable
fun MonthComparisonCard(comparison: MonthComparison) {
    val isHigher = comparison.delta > 0
    val deltaColor = if (isHigher) ExpenseRed else AccentGreenLight
    val arrow = if (isHigher) "▲" else "▼"
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Month Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("This Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KES ${formatCurrency(comparison.currentMonthExpense)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Last Month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KES ${formatCurrency(comparison.previousMonthExpense)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(arrow, color = deltaColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "KES ${formatCurrency(kotlin.math.abs(comparison.delta))} (${String.format(Locale.US, "%.1f", kotlin.math.abs(comparison.percentChange))}%)",
                    style = MaterialTheme.typography.bodyMedium, color = deltaColor, fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isHigher) "more than last month" else "less than last month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FulizaTrendCard(trend: List<FulizaMonthPoint>) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Fuliza Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Monthly Fuliza usage history", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            trend.takeLast(6).forEach { point ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(point.month, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Used: KES ${formatCurrency(point.totalFulizaUsed)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        if (point.outstandingBalance > 0) {
                            Text("Outstanding: KES ${formatCurrency(point.outstandingBalance)}", style = MaterialTheme.typography.labelSmall, color = WarningOrange)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text("KES", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp)
            Text(amount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun WhereItGoesChart(transactions: List<com.pesalytics.model.Transaction>, categoryDeltas: List<CategoryDelta> = emptyList()) {
    val expenses = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME && !it.isFeeTransaction }
    val totalExpense = expenses.sumOf { it.amount }

    val typeLabels = mapOf(
        TransactionType.SEND_MONEY to "Send Money",
        TransactionType.PAYBILL to "Paybill",
        TransactionType.BUY_GOODS to "Buy Goods",
        TransactionType.WITHDRAW to "Withdraw",
        TransactionType.AIRTIME to "Airtime",
        TransactionType.MANUAL_EXPENSE to "Other"
    )
    val rawTypeTotals = expenses.groupBy { typeLabels[it.type] ?: "Other" }
        .mapValues { it.value.sumOf { t -> t.amount } }
        .toList().sortedByDescending { it.second }
    // Cap at 5 categories to keep chart readable (P10 — UX audit)
    val typeTotals = if (rawTypeTotals.size > 5) {
        val top4 = rawTypeTotals.take(4)
        val othersTotal = rawTypeTotals.drop(4).sumOf { it.second }
        top4 + listOf("Others" to othersTotal)
    } else rawTypeTotals

    val colors = listOf(ExpenseRed, TransferBlue, AccentGreenLight, AccentGreenDark, Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFF00BCD4))

    var selectedCategory by remember { mutableStateOf<Pair<String, Double>?>(null) }

    Card(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Where It Goes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            if (totalExpense == 0.0) {
                Text("No expenses this month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            val animatedSweep = remember { Animatable(0f) }
            LaunchedEffect(typeTotals) {
                animatedSweep.snapTo(0f)
                animatedSweep.animateTo(1f, animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing))
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
                            val isInSlice = if (startNorm < maxAngle) angle in startNorm..maxAngle else angle >= startNorm || angle <= maxAngle
                            if (isInSlice) { selectedCategory = item; break }
                            currentAngle += sweep
                        }
                    }
                }) {
                    val totalAnimatedSweep = 360f * animatedSweep.value
                    var currentStartAngle = 270f
                    var consumedSweep = 0f
                    typeTotals.forEachIndexed { index, pair ->
                        val fullSweep = ((pair.second / totalExpense) * 360).toFloat()
                        val remaining = totalAnimatedSweep - consumedSweep
                        val actualSweep = fullSweep.coerceAtMost(remaining.coerceAtLeast(0f))
                        if (actualSweep > 0f) {
                            drawArc(color = colors[index % colors.size], startAngle = currentStartAngle, sweepAngle = actualSweep, useCenter = false, style = Stroke(width = 40.dp.toPx(), cap = StrokeCap.Butt), size = Size(size.width, size.height))
                        }
                        currentStartAngle += fullSweep; consumedSweep += fullSweep
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

            // Legend with delta badges
            typeTotals.forEachIndexed { index, pair ->
                val delta = categoryDeltas.find { it.category.equals(pair.first, ignoreCase = true) }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(colors[index % colors.size]))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pair.first, style = MaterialTheme.typography.bodyMedium)
                        if (delta != null && kotlin.math.abs(delta.percentChange) >= 10.0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            val isUp = delta.percentChange > 0
                            Text(
                                text = "${if (isUp) "▲" else "▼"}${kotlin.math.abs(delta.percentChange).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUp) ExpenseRed else AccentGreenLight,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
fun TransactionFeesCard(transactions: List<com.pesalytics.model.Transaction>) {
    val mainTransactions = transactions.filter { !it.isFeeTransaction }
    val feeBreakdown = mainTransactions.filter { it.fee > 0 }.groupBy { it.type }.mapValues { entry -> entry.value.sumOf { it.fee } }
    val totalFees = feeBreakdown.values.sum()
    val totalExpense = mainTransactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }.sumOf { it.amount }
    val percentage = if (totalExpense > 0) (totalFees / totalExpense) * 100 else 0.0

    val typeLabels = mapOf(TransactionType.PAYBILL to "Paybill", TransactionType.BUY_GOODS to "Till", TransactionType.WITHDRAW to "Withdraw", TransactionType.SEND_MONEY to "Send Money", TransactionType.AIRTIME to "Airtime")

    Card(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Transaction Fees", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("M-PESA charges this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("KES ${formatCurrency(totalFees)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total fees paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${String.format(Locale.US, "%.1f", percentage)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("of expenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            val orderedTypes = listOf(TransactionType.PAYBILL, TransactionType.BUY_GOODS, TransactionType.WITHDRAW, TransactionType.SEND_MONEY, TransactionType.AIRTIME)
            orderedTypes.forEach { type ->
                val fee = feeBreakdown[type] ?: 0.0
                if (fee > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(typeLabels[type] ?: type.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("KES ${formatCurrency(fee)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun SpendingRhythmChart(transactions: List<com.pesalytics.model.Transaction>) {
    val allowedTypes = listOf(TransactionType.SEND_MONEY, TransactionType.WITHDRAW, TransactionType.PAYBILL, TransactionType.BUY_GOODS)
    val expenses = transactions.filter { it.type in allowedTypes }

    val last7Days = (6 downTo 0).map { offset ->
        val cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -offset); cal
    }
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val fullDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    var highestDayName = ""
    var highestAmount = 0.0

    val rhythmData = last7Days.map { dayCal ->
        val daySpend = expenses.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.amount }
        if (daySpend > highestAmount) { highestAmount = daySpend; highestDayName = fullDayFormat.format(dayCal.time) }
        Triple(dayFormat.format(dayCal.time), daySpend, dayCal.get(Calendar.DAY_OF_WEEK))
    }

    val maxSpend = rhythmData.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0
    var weekdaySum = 0.0; var weekdayCount = 0; var weekendSum = 0.0; var weekendCount = 0
    rhythmData.forEach { (_, amount, dow) ->
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) { weekendSum += amount; weekendCount++ }
        else { weekdaySum += amount; weekdayCount++ }
    }
    val weekdayAvg = if (weekdayCount > 0) weekdaySum / weekdayCount else 0.0
    val weekendAvg = if (weekendCount > 0) weekendSum / weekendCount else 0.0
    var showRhythmTooltip by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Spending Rhythm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(16.dp).clickable { showRhythmTooltip = true }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        DropdownMenu(expanded = showRhythmTooltip, onDismissRequest = { showRhythmTooltip = false }, modifier = Modifier.width(250.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(8.dp)) {
                            Text("Spending Rhythm", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Cumulative spending pattern over the last 7 days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = "Expand", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Last 7 Days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth().height(160.dp).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                rhythmData.forEachIndexed { index, (day, amount, _) ->
                    val isToday = index == 6
                    val proportion = (amount / maxSpend).toFloat()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                        if (amount > 0) {
                            Text(if (amount >= 1000) String.format(Locale.US, "%.0fk", amount / 1000.0) else amount.toInt().toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (isToday) AccentGreenLight else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Box(modifier = Modifier.width(32.dp).fillMaxHeight(proportion.coerceAtLeast(0.02f)).clip(RoundedCornerShape(8.dp)).background(if (isToday) AccentGreenLight else MaterialTheme.colorScheme.surfaceVariant))
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.width(32.dp).height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(day, style = MaterialTheme.typography.labelSmall, color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (highestAmount > 0) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Text("You spend most on ", style = MaterialTheme.typography.bodyMedium, color = AccentGreenLight)
                        Text(highestDayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(" (KES ${formatCurrency(highestAmount)}/day)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weekday avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("KES ${formatCurrency(weekdayAvg)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Mon - Fri", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Weekend avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("KES ${formatCurrency(weekendAvg)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Sat - Sun", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun NeedsVsWantsCard(
    transactions: List<com.pesalytics.model.Transaction>,
    classification: Map<String, Boolean>,
    onNavigateToNeedsWants: () -> Unit
) {
    val expenses = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
    var needsAmount = 0.0; var wantsAmount = 0.0
    expenses.forEach { t ->
        if (isNeedCategory(t.category, classification)) needsAmount += t.amount else wantsAmount += t.amount
    }
    val total = needsAmount + wantsAmount
    val needsPercent = if (total > 0) (needsAmount / total).toFloat() else 0f
    val needsColor = AccentGreenLight; val wantsColor = Color(0xFFFF5252)
    var showWantsTooltip by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)).clickable(onClick = onNavigateToNeedsWants), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Needs vs Wants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(16.dp).clickable { showWantsTooltip = true }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        DropdownMenu(expanded = showWantsTooltip, onDismissRequest = { showWantsTooltip = false }, modifier = Modifier.width(250.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(8.dp)) {
                            Text("Needs vs Wants", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("A breakdown of your essential expenses vs discretionary spending.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = "Go to settings", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("This month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            if (total == 0.0) { Text("No data to split.", style = MaterialTheme.typography.bodySmall); return@Column }
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (needsPercent > 0f) Box(modifier = Modifier.weight(needsPercent).fillMaxHeight().background(needsColor))
                    if (needsPercent < 1f) Box(modifier = Modifier.weight(1f - needsPercent).fillMaxHeight().background(wantsColor))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(needsColor)); Spacer(modifier = Modifier.width(6.dp)); Text("NEEDS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(needsPercent * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = needsColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("KES ${formatCurrency(needsAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(modifier = Modifier.width(1.dp).height(60.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(wantsColor)); Spacer(modifier = Modifier.width(6.dp)); Text("WANTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp) }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${((1f - needsPercent) * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = wantsColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("KES ${formatCurrency(wantsAmount)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            val isHighWants = wantsAmount > needsAmount
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isHighWants) wantsColor.copy(alpha = 0.15f) else needsColor.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (isHighWants) "High discretionary spending. Time for Hansei?" else "Great job keeping wants low!", style = MaterialTheme.typography.bodyMedium, color = if (isHighWants) wantsColor else needsColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingCalendar(transactions: List<com.pesalytics.model.Transaction>, monthStartTimestamp: Long, displayMonth: String) {
    val expenses = transactions.filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
    val cal = Calendar.getInstance(); cal.timeInMillis = monthStartTimestamp
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val spendMap = mutableMapOf<Int, Double>()
    for (i in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, i); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis; cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        spendMap[i] = expenses.filter { it.timestamp in startOfDay..cal.timeInMillis }.sumOf { it.amount }
    }
    val maxDailySpend = spendMap.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val highestDayEntry = spendMap.maxByOrNull { it.value }
    val noSpendDaysCount = spendMap.values.count { it == 0.0 }
    val highestDayText = if (highestDayEntry != null && highestDayEntry.value > 0) { cal.timeInMillis = monthStartTimestamp; cal.set(Calendar.DAY_OF_MONTH, highestDayEntry.key); SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time) } else "N/A"
    val highestAmount = highestDayEntry?.value ?: 0.0
    val baseColor = AccentGreenLight
    val colorLevel0 = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val colorLevel1 = baseColor.copy(alpha = 0.25f); val colorLevel2 = baseColor.copy(alpha = 0.5f); val colorLevel3 = baseColor.copy(alpha = 0.75f); val colorLevel4 = baseColor
    fun getColorForAmount(amount: Double): Color = when {
        amount == 0.0 -> colorLevel0
        (amount / maxDailySpend).toFloat() < 0.25f -> colorLevel1
        (amount / maxDailySpend).toFloat() < 0.5f -> colorLevel2
        (amount / maxDailySpend).toFloat() < 0.75f -> colorLevel3
        else -> colorLevel4
    }
    var showTooltip by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Spending Calendar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(16.dp).clickable { showTooltip = true }, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    DropdownMenu(expanded = showTooltip, onDismissRequest = { showTooltip = false }, modifier = Modifier.width(250.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(8.dp)) {
                        Text("Spending Calendar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Your spending by day. Brighter colour means more spent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(displayMonth, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            var currentDay = 1
            val offset = firstDayOfWeek - 1
            val totalCells = Math.ceil((daysInMonth + offset) / 7.0).toInt() * 7
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (col in 0..6) {
                        if (row == 0 && col < offset) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(2f).padding(horizontal = 2.dp))
                        } else if (currentDay <= daysInMonth) {
                            val amount = spendMap[currentDay] ?: 0.0
                            Box(modifier = Modifier.weight(1f).aspectRatio(2f).padding(horizontal = 2.dp).clip(RoundedCornerShape(4.dp)).background(getColorForAmount(amount)), contentAlignment = Alignment.Center) {
                                Text(currentDay.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (amount > 0 && amount >= maxDailySpend * 0.5f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            currentDay++
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(2f).padding(horizontal = 2.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(8.dp))
                listOf(colorLevel0, colorLevel1, colorLevel2, colorLevel3, colorLevel4).forEach { color -> Box(modifier = Modifier.size(14.dp, 10.dp).clip(RoundedCornerShape(2.dp)).background(color)); Spacer(modifier = Modifier.width(4.dp)) }
                Spacer(modifier = Modifier.width(4.dp)); Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Highest", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(highestDayText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("KES ${formatCurrency(highestAmount)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No-Spend Days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$noSpendDaysCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TransferBlue)
                        Text("this period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceProgressionChart(transactions: List<com.pesalytics.model.Transaction>, monthStartTimestamp: Long) {
    val balancePoints = transactions
        .filter { !it.isFeeTransaction && it.balanceAfter > 0 }
        .sortedBy { it.timestamp }
    if (balancePoints.isEmpty()) return

    val maxBalance = balancePoints.maxOf { it.balanceAfter }
    val minBalance = balancePoints.minOf { it.balanceAfter }
    val range = (maxBalance - minBalance).coerceAtLeast(1.0)
    val currentBalance = balancePoints.last().balanceAfter
    val monthEnd = remember(monthStartTimestamp) {
        Calendar.getInstance().apply { timeInMillis = monthStartTimestamp; add(Calendar.MONTH, 1) }.timeInMillis
    }
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Account Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Balance progression this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val w = size.width
                val h = size.height
                val timeRange = (monthEnd - monthStartTimestamp).toFloat().coerceAtLeast(1f)

                repeat(3) { i ->
                    val gy = h * (1f - (i + 1) / 4f)
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                }

                if (balancePoints.size >= 2) {
                    val points = balancePoints.map { tx ->
                        val x = ((tx.timestamp - monthStartTimestamp) / timeRange * w).coerceIn(0f, w)
                        val y = (h - ((tx.balanceAfter - minBalance) / range * h).toFloat()).coerceIn(0f, h)
                        Offset(x, y)
                    }
                    val fillPath = Path()
                    points.forEachIndexed { i, pt -> if (i == 0) fillPath.moveTo(pt.x, pt.y) else fillPath.lineTo(pt.x, pt.y) }
                    fillPath.lineTo(points.last().x, h)
                    fillPath.lineTo(0f, h)
                    fillPath.close()
                    drawPath(fillPath, AccentGreenLight.copy(alpha = 0.08f))

                    val linePath = Path()
                    points.forEachIndexed { i, pt -> if (i == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y) }
                    drawPath(linePath, AccentGreenLight, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    drawCircle(AccentGreenLight, radius = 4.dp.toPx(), center = points.last())
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Peak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KES ${formatCurrency(maxBalance)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lowest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "KES ${formatCurrency(minBalance)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (minBalance < maxBalance * 0.2) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("KES ${formatCurrency(currentBalance)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LargestTransactionsCard(transactions: List<com.pesalytics.model.Transaction>) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val typeLabels = mapOf(
        TransactionType.SEND_MONEY to "Send Money",
        TransactionType.PAYBILL to "Paybill",
        TransactionType.BUY_GOODS to "Buy Goods",
        TransactionType.WITHDRAW to "Withdraw",
        TransactionType.AIRTIME to "Airtime",
        TransactionType.MANUAL_EXPENSE to "Manual",
        TransactionType.POCHI to "Pochi"
    )
    val top = transactions
        .filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME && !it.isFeeTransaction }
        .sortedByDescending { it.amount }
        .take(5)
    if (top.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Largest Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Biggest individual expenses this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            top.forEachIndexed { index, tx ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.payee.take(24), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${typeLabels[tx.type] ?: "Other"} · ${dateFormat.format(Date(tx.timestamp))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text("KES ${formatCurrency(tx.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ExpenseRed)
                }
                if (index < top.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun TopPayeesCard(transactions: List<com.pesalytics.model.Transaction>) {
    val expenses = transactions.filter {
        it.type != TransactionType.RECEIVE_MONEY &&
        it.type != TransactionType.MANUAL_INCOME &&
        !it.isFeeTransaction
    }
    val totalExpense = expenses.sumOf { it.amount }.coerceAtLeast(1.0)
    val topPayees = expenses
        .groupBy { it.payee }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
        .take(5)
    if (topPayees.isEmpty()) return

    val maxAmount = topPayees.first().value.coerceAtLeast(1.0)
    val barColors = listOf(ExpenseRed, TransferBlue, AccentGreenLight, WarningOrange, Color(0xFF9C27B0))

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Top Payees", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Who received most of your money", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            topPayees.forEachIndexed { index, entry ->
                val barColor = barColors[index % barColors.size]
                val fraction = (entry.value / maxAmount).toFloat()
                val pctOfTotal = ((entry.value / totalExpense) * 100).toInt()

                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(barColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = barColor, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(entry.key.take(22), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("KES ${formatCurrency(entry.value)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("$pctOfTotal%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(barColor))
                    }
                }
                if (index < topPayees.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun IncomeSourcesCard(transactions: List<com.pesalytics.model.Transaction>) {
    val income = transactions.filter {
        (it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME) &&
        !it.isFeeTransaction
    }
    val totalIncome = income.sumOf { it.amount }.coerceAtLeast(1.0)
    val sources = income
        .groupBy { it.payee }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .entries.sortedByDescending { it.value }
        .take(5)
    if (sources.isEmpty()) return

    val maxAmount = sources.first().value.coerceAtLeast(1.0)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Income Sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Who paid you this month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            sources.forEachIndexed { index, entry ->
                val fraction = (entry.value / maxAmount).toFloat()
                val pctOfTotal = ((entry.value / totalIncome) * 100).toInt()

                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(AccentGreenLight.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = AccentGreenLight, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(entry.key.take(22), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("KES ${formatCurrency(entry.value)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                            Text("$pctOfTotal%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(AccentGreenLight))
                    }
                }
                if (index < sources.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
fun BudgetVsActualCard(budgets: List<Budget>, categorySpent: Map<String, Double>) {
    val overallBudget = budgets.find { it.category == "Overall" }
    val categoryBudgets = budgets.filter { it.category != "Overall" }
    if (overallBudget == null && categoryBudgets.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Budget vs Actual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("How spending tracks against your plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))

            overallBudget?.let { budget ->
                val totalSpent = categorySpent.values.sum()
                BudgetProgressRow(label = "Overall", spent = totalSpent, limit = budget.limitAmount, isOverall = true)
                if (categoryBudgets.isNotEmpty()) HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
            }

            categoryBudgets.forEachIndexed { index, budget ->
                val spent = categorySpent[budget.category] ?: 0.0
                BudgetProgressRow(label = budget.category, spent = spent, limit = budget.limitAmount, isOverall = false)
                if (index < categoryBudgets.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun BudgetProgressRow(label: String, spent: Double, limit: Double, isOverall: Boolean) {
    val fraction = if (limit > 0) (spent / limit).toFloat() else 0f
    val barColor = when {
        fraction >= 1f -> ExpenseRed
        fraction >= 0.7f -> WarningOrange
        else -> AccentGreenLight
    }
    val statusText = when {
        fraction >= 1f -> "Over by KES ${formatCurrency(spent - limit)}"
        else -> "KES ${formatCurrency(limit - spent)} remaining · ${(fraction * 100).toInt()}% used"
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(
                label,
                style = if (isOverall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isOverall) FontWeight.Bold else FontWeight.SemiBold
            )
            Text("KES ${formatCurrency(spent)} / ${formatCurrency(limit)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(if (isOverall) 8.dp else 6.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(modifier = Modifier.fillMaxWidth(fraction.coerceAtMost(1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(barColor))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(statusText, style = MaterialTheme.typography.labelSmall, color = barColor)
    }
}

@Composable
fun AnalyticsLockedPreview(onUpgrade: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Analytics — Premium Feature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Unlock spending breakdowns, fee analysis, spending rhythm, and the monthly heat map with Premium.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(onClick = onUpgrade) {
                Text("View Premium Plans")
            }
        }
    }
}
