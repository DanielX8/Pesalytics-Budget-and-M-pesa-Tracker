package com.pesasense.ui.screens

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesasense.R
import com.pesasense.model.TransactionType
import com.pesasense.patterns.CategoryDelta
import com.pesasense.patterns.FulizaMonthPoint
import com.pesasense.patterns.MonthComparison
import com.pesasense.patterns.SpendVelocity
import com.pesasense.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit
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
                            contentDescription = "PesaSense",
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
                item { SpendVelocityBanner(velocity) }
            }

            // Month Comparison Card
            patternResult?.monthComparison?.let { comparison ->
                item { MonthComparisonCard(comparison) }
            }

            item {
                WhereItGoesChart(
                    transactions = monthTransactions,
                    categoryDeltas = patternResult?.categoryDeltas ?: emptyList()
                )
            }

            item { TransactionFeesCard(monthTransactions) }

            item {
                Text("PATTERNS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 0.dp))
            }

            item { SpendingRhythmChart(monthTransactions) }
            item { NeedsVsWantsCard(monthTransactions, needsWantsClassification) }
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
fun SpendVelocityBanner(velocity: SpendVelocity) {
    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Spend Velocity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Day ${velocity.daysElapsed} of ${velocity.daysInMonth}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
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
fun WhereItGoesChart(transactions: List<com.pesasense.model.Transaction>, categoryDeltas: List<CategoryDelta> = emptyList()) {
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
fun TransactionFeesCard(transactions: List<com.pesasense.model.Transaction>) {
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
fun SpendingRhythmChart(transactions: List<com.pesasense.model.Transaction>) {
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
    transactions: List<com.pesasense.model.Transaction>,
    classification: Map<String, Boolean>
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

    Card(modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp)), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
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
                Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun SpendingCalendar(transactions: List<com.pesasense.model.Transaction>, monthStartTimestamp: Long, displayMonth: String) {
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
