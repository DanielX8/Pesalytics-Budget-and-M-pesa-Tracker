package com.pesalytics.ui.screens

import androidx.compose.ui.graphics.luminance

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Payment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesalytics.model.Transaction
import com.pesalytics.model.TransactionType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.rememberLazyListState
import com.pesalytics.ui.theme.AccentGreenDark
import com.pesalytics.ui.theme.AccentGreenLight
import com.pesalytics.ui.theme.ExpenseRed
import com.pesalytics.ui.theme.IncomeGreen
import com.pesalytics.ui.theme.HeroGradientEnd
import com.pesalytics.ui.theme.TransferBlue
import com.pesalytics.ui.theme.WarningOrange
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

private val currencyFormat = java.text.NumberFormat.getInstance().apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
private val dateTimeFormat = java.text.SimpleDateFormat("dd MMM · HH:mm", java.util.Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PesaViewModel,
    onNavigateToAllTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToBudgetPlanner: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    val syncTotalMessages by viewModel.syncTotalMessages.collectAsStateWithLifecycle()
    val isFirstSync by viewModel.isFirstSync.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasRequestedSmsPermission by rememberSaveable { mutableStateOf(false) }
    var showSmsDisclosure by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncMpesaSms(context)
        }
        hasRequestedSmsPermission = true
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedSmsPermission) {
            val permission = android.Manifest.permission.READ_SMS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                viewModel.syncMpesaSms(context)
            } else {
                showSmsDisclosure = true
            }
            hasRequestedSmsPermission = true
        }
    }

    val trialJustStarted by viewModel.trialJustStarted.collectAsStateWithLifecycle()
    var showTrialSheet by rememberSaveable { mutableStateOf(false) }
    val trialSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetScope = rememberCoroutineScope()
    LaunchedEffect(trialJustStarted) {
        if (trialJustStarted) {
            showTrialSheet = true
            viewModel.consumeTrialStartedEvent()
        }
    }
    if (showTrialSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTrialSheet = false },
            sheetState = trialSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎉", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "14-Day Free Trial Started!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "You now have full access to all Pesalytics Premium features for 14 days — completely free. Enjoy the analytics, budget planner, bill tracker, and data exports. No payment required to start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        sheetScope.launch { trialSheetState.hide() }.invokeOnCompletion {
                            showTrialSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenDark)
                ) {
                    Text("Let's Go!", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showSmsDisclosure) {
        AlertDialog(
            onDismissRequest = { showSmsDisclosure = false },
            title = { Text("M-PESA SMS Access") },
            text = {
                Text(
                    "Pesalytics reads your M-PESA SMS messages to automatically track your transactions.\n\n" +
                    "Your messages are processed entirely on your device — no data is ever uploaded or shared. " +
                    "Only M-PESA messages from Safaricom are read; all other SMS are ignored."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSmsDisclosure = false
                    permissionLauncher.launch(android.Manifest.permission.READ_SMS)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showSmsDisclosure = false }) { Text("Not now") }
            }
        )
    }

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionDetails by rememberSaveable { mutableStateOf(false) }
    var showAddManualDialog by rememberSaveable { mutableStateOf(false) }
    var showCategoryEdit by rememberSaveable { mutableStateOf(false) }

    if (showAddManualDialog) {
        val addManualSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()
        var manualAmount by remember { mutableStateOf("") }
        var manualPayee by remember { mutableStateOf("") }
        var isManualIncome by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showAddManualDialog = false },
            sheetState = addManualSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Add Manual Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    listOf("Expense" to false, "Income" to true).forEach { (label, isIncome) ->
                        val isSelected = isManualIncome == isIncome
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreenDark else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { isManualIncome = isIncome }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSelected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = manualAmount,
                    onValueChange = { manualAmount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (KES)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualPayee,
                    onValueChange = { manualPayee = it },
                    label = { Text("Payee / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val amount = manualAmount.toDoubleOrNull()
                        if (amount != null && amount > 0 && manualPayee.isNotBlank()) {
                            viewModel.insertManualTransaction(amount, manualPayee, isManualIncome)
                            coroutineScope.launch {
                                addManualSheetState.hide()
                                showAddManualDialog = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = manualAmount.toDoubleOrNull() != null && manualPayee.isNotBlank()
                ) {
                    Text("Save Transaction")
                }
            }
        }
    }

    if (showCategoryEdit && selectedTransaction != null) {
        val txn = selectedTransaction!!
        val categorySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val categoryScope = rememberCoroutineScope()
        var newCategoryName by remember { mutableStateOf(txn.category) }
        val predefinedCategories = listOf("Groceries", "Utilities", "Food & Dining", "Transport", "Shopping", "Entertainment", "Health", "Airtime", "Other")

        ModalBottomSheet(
            onDismissRequest = { showCategoryEdit = false },
            sheetState = categorySheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        categoryScope.launch { categorySheetState.hide() }.invokeOnCompletion { showCategoryEdit = false }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Recategorise all transactions from:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    txn.payee,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
                FlowRow(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    label = { Text("Custom category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.updateTransactionCategory(txn, newCategoryName.trim())
                            selectedTransaction = txn.copy(category = newCategoryName.trim())
                        }
                        categoryScope.launch { categorySheetState.hide() }.invokeOnCompletion { showCategoryEdit = false }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenDark),
                    shape = RoundedCornerShape(12.dp),
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Save Category", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (showTransactionDetails && selectedTransaction != null) {
        val txn = selectedTransaction!!
        TransactionDetailsSheet(
            transaction = txn,
            onDismiss = { showTransactionDetails = false },
            onEditCategory = { showCategoryEdit = true },
            onShare = {
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Pesalytics Transaction Receipt:\nRef: ${txn.remoteRef}\nPayee: ${txn.payee}\nAmount: KES ${formatCurrency(txn.amount)}")
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
            com.pesalytics.ui.components.PesalyticsTopBar(
                viewModel = viewModel,
                titleContent = {
                    Text("Pesalytics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
            )
            if (isSyncing) {
                val firstSyncFraction = if (isFirstSync && syncTotalMessages > 0)
                    (syncProgress.toFloat() / syncTotalMessages.toFloat()).coerceIn(0f, 1f)
                else -1f  // sentinel: use indeterminate bar

                if (firstSyncFraction >= 0f) {
                    // First sync — determinate bar with percentage label
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LinearProgressIndicator(
                            progress = { firstSyncFraction },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = AccentGreenLight,
                            trackColor = AccentGreenDark.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "Importing M-PESA history… ${(firstSyncFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 2.dp, bottom = 4.dp)
                        )
                    }
                } else {
                    // Subsequent sync — fast, indeterminate
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = AccentGreenLight,
                        trackColor = AccentGreenDark.copy(alpha = 0.3f)
                    )
                }
            }
          }
        },
        floatingActionButton = {
            if (uiState.recentTransactions.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val permission = android.Manifest.permission.READ_SMS
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            viewModel.syncMpesaSms(context)
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    },
                    icon = { Icon(Icons.Default.Sync, contentDescription = null) },
                    text = { Text("Sync M-PESA") },
                    containerColor = AccentGreenDark,
                    contentColor = Color.White
                )
            } else {
                FloatingActionButton(
                    onClick = { showAddManualDialog = true },
                    containerColor = AccentGreenDark,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(32.dp))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val groupedRecents = remember(uiState.recentTransactions) {
            uiState.recentTransactions.groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                val currentCal = Calendar.getInstance()
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                val format = SimpleDateFormat("dd MMM", Locale.getDefault())
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

                when {
                    cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR) -> "TODAY"
                    cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "YESTERDAY"
                    currentCal.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) < 7 -> dayFormat.format(cal.time).uppercase(Locale.getDefault())
                    else -> format.format(cal.time).uppercase(Locale.getDefault())
                }
            }
        }

        val dashboardScrollState = rememberLazyListState()

        LazyColumn(
            state = dashboardScrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item(key = "greeting") {
                val greeting = remember { getGreetingMessage() }
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "$greeting,",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val userName by viewModel.userName.collectAsStateWithLifecycle()
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            item(key = "month-selector") {
                val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                val selectedMonthIndex by viewModel.selectedMonthIndex.collectAsStateWithLifecycle()
                val selectedYear by viewModel.selectedYear.collectAsStateWithLifecycle()
                val selectedMonth = months.getOrNull(selectedMonthIndex) ?: months.first()
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedMonthIndex - 1))
                val monthAccent = interactiveGreen
                val nowCalendar = java.util.Calendar.getInstance()
                val realCurrentYear = nowCalendar.get(java.util.Calendar.YEAR)
                val realCurrentMonth = nowCalendar.get(java.util.Calendar.MONTH)

                androidx.compose.foundation.lazy.LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(months.size) { index ->
                        val month = months[index]
                        val isSelected = index == selectedMonthIndex
                        // A chip is only "future" when it belongs to the real current year
                        // and its month is after the real current month — same-year past
                        // months and any month in a prior year remain selectable.
                        val isFuture = selectedYear == realCurrentYear && index > realCurrentMonth
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) monthAccent else MaterialTheme.colorScheme.surface)
                                .alpha(if (isFuture) 0.4f else 1f)
                                .clickable(enabled = !isFuture) { viewModel.setSelectedMonth(index) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = month,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item(key = "hero-card") {
                Box(modifier = Modifier
                    .animateItem()
                    .graphicsLayer {
                        val index = dashboardScrollState.firstVisibleItemIndex
                        val offset = dashboardScrollState.firstVisibleItemScrollOffset.toFloat()
                        
                        // Estimate total scroll amount based on which item is at the top
                        val estimatedTotalScroll = when (index) {
                            0 -> offset
                            1 -> (60f * density) + offset
                            2 -> (130f * density) + offset
                            else -> 1000f * density
                        }
                        
                        // Parallax shrink and fade
                        val progress = (estimatedTotalScroll / (300f * density)).coerceIn(0f, 1f)
                        scaleX = 1f - (progress * 0.15f)
                        scaleY = 1f - (progress * 0.15f)
                        // Fade out completely before the item gets removed by LazyColumn
                        this.alpha = 1f - (progress * 1.5f).coerceIn(0f, 1f)
                        // Parallax translation (slower scroll)
                        translationY = estimatedTotalScroll * 0.5f
                    }
                ) {
                    val pageCount = 1 +
                        (if (uiState.hasMshwari) 1 else 0) +
                        (if (uiState.hasPochi) 1 else 0)
                    if (pageCount > 1) {
                        val pagerState = rememberPagerState(pageCount = { pageCount })
                        Column {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val absOffset = kotlin.math.abs(pageOffset)
                                
                                Box(
                                    modifier = Modifier.graphicsLayer {
                                        // 3D Perspective Carousel effects
                                        rotationY = pageOffset * -15f // tilt pages towards the center
                                        scaleX = 1f - (absOffset * 0.15f)
                                        scaleY = 1f - (absOffset * 0.15f)
                                        alpha = 1f - (absOffset * 0.5f)
                                        cameraDistance = 8f * density
                                    }
                                ) {
                                    var idx = 0
                                    when {
                                        page == idx -> HeroCard(uiState = uiState, onToggleVisibility = { viewModel.toggleBalanceVisibility() })
                                        uiState.hasMshwari && page == ++idx -> MshwariHeroCard(uiState = uiState, onToggleVisibility = { viewModel.toggleBalanceVisibility() })
                                        else -> PochiHeroCard(uiState = uiState, onToggleVisibility = { viewModel.toggleBalanceVisibility() })
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(pageCount) { index ->
                                    val selected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(if (selected) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) AccentGreenLight
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        HeroCard(uiState = uiState, onToggleVisibility = { viewModel.toggleBalanceVisibility() })
                    }
                }
            }
            
            item(key = "quick-nav") {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickNavButton(icon = Icons.Default.Insights, label = "Analytics", color = TransferBlue, onClick = onNavigateToAnalytics)
                    QuickNavButton(icon = Icons.AutoMirrored.Filled.ReceiptLong, label = "Bills", color = AccentGreenDark, onClick = onNavigateToBills)
                    QuickNavButton(icon = Icons.Default.DonutLarge, label = "Budget", color = ExpenseRed, onClick = onNavigateToBudgetPlanner)
                    QuickNavButton(icon = Icons.Default.TrackChanges, label = "Goals", color = IncomeGreen, onClick = onNavigateToGoals)
                }
            }

            if (uiState.hasBudget) {
                item(key = "budget-progress") {
                    val spent = uiState.monthlyExpense
                    val limit = uiState.currentBudgetLimit
                    val progress = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
                    val monthlyLabel = "Monthly Limit" // Can be made dynamic from user settings later
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .animateItem()
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(monthlyLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${(progress * 100).toInt()}% Used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (progress > 0.9f) ExpenseRed else AccentGreenDark,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("KES ${formatCurrency(spent)} spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("KES ${formatCurrency(limit - spent)} left", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            // Dynamic Insights
            if (uiState.insights.isNotEmpty()) {
                item { InsightsCarousel(uiState.insights) }
            }            
            
            item(key = "recent-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onNavigateToAllTransactions) {
                        Text("View All")
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // AnimatedContent: smoothly crossfades between empty state and populated list
            item(key = "recent-content") {
                AnimatedContent(
                    targetState = groupedRecents.isEmpty(),
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
                    },
                    label = "RecentActivityState"
                ) { isEmpty ->
                    if (isEmpty) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 0.dp, bottom = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                if (isSyncing) {
                                    if (isFirstSync) {
                                        FirstSyncLoader()
                                    } else {
                                        CircularProgressIndicator(color = AccentGreenLight, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "Syncing your M-PESA history...",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        "No transactions yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Sync your M-PESA SMS to get started",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        Column {
                            groupedRecents.forEach { (dateHeader, transactions) ->
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                                )
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(2.dp, RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column {
                                        transactions.forEachIndexed { index, transaction ->
                                            TransactionItem(transaction = transaction, onClick = {
                                                selectedTransaction = transaction
                                                showTransactionDetails = true
                                            })
                                            if (index < transactions.size - 1) {
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

@Composable
fun QuickNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                // Colored glow shadow — the key upgrade from flat to premium
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = color.copy(alpha = 0.55f),
                    ambientColor = color.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(16.dp))
                // Gradient background instead of flat 10% alpha
                .background(
                    Brush.linearGradient(
                        listOf(color.copy(alpha = 0.75f), color.copy(alpha = 0.35f))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HeroCard(uiState: HomeUiState, onToggleVisibility: () -> Unit) {
    val gradient = rememberBrandGradient()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Glowing card shadow — the single biggest visual upgrade
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = AccentGreenLight.copy(alpha = 0.4f),
                ambientColor = AccentGreenDark.copy(alpha = 0.25f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // Radial glow overlay for mesh-gradient depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            radius = 500f
                        )
                    )
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available M-PESA Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (uiState.isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.isBalanceVisible) "Hide balance" else "Show balance",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Spring count-up animation — rolls from 0 → actual balance when revealed.
                // Snaps back to •••••• instantly when hidden. Auto-hide timer lives in ViewModel.
                val animatedBalance by animateFloatAsState(
                    targetValue = if (uiState.isBalanceVisible) uiState.currentBalance.toFloat() else 0f,
                    animationSpec = if (uiState.isBalanceVisible)
                        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
                    else tween(0),
                    label = "HeroBalanceSpring"
                )

                if (uiState.isBalanceVisible) {
                    Text(
                        text = "KES ${formatCurrency(animatedBalance.toDouble())}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "KES ••••••",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(
                        modifier = Modifier.semantics {
                            contentDescription = "Money In, KES ${formatCurrency(uiState.monthlyIncome)}"
                        }
                    ) {
                        Text("MONEY IN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = if (uiState.isBalanceVisible) "KES ${formatCurrency(uiState.monthlyIncome)}" else "••••",
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "MoneyIn"
                            ) { text ->
                                Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.semantics {
                            contentDescription = "Money Out, KES ${formatCurrency(uiState.monthlyExpense)}"
                        }
                    ) {
                        Text("MONEY OUT", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = if (uiState.isBalanceVisible) "KES ${formatCurrency(uiState.monthlyExpense)}" else "••••",
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "MoneyOut"
                            ) { text ->
                                Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MshwariHeroCard(uiState: HomeUiState, onToggleVisibility: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0xFF348C55).copy(alpha = 0.4f),
                ambientColor = Color(0xFF071F16).copy(alpha = 0.3f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF0B4631), Color(0xFF071F16))))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                            radius = 500f
                        )
                    )
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "M-Shwari Savings",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (uiState.isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Spring count-up animation — rolls from 0 → actual balance when revealed.
                // Snaps back to •••••• instantly when hidden. Auto-hide timer lives in ViewModel.
                val animatedBalance by animateFloatAsState(
                    targetValue = if (uiState.isBalanceVisible) uiState.mshwariBalance.toFloat() else 0f,
                    animationSpec = if (uiState.isBalanceVisible)
                        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
                    else tween(0),
                    label = "HeroBalanceSpring"
                )

                if (uiState.isBalanceVisible) {
                    Text(
                        text = "KES ${formatCurrency(animatedBalance.toDouble())}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "KES ••••••",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SAVED", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AccentGreenLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = if (uiState.isBalanceVisible) "KES ${formatCurrency(uiState.mshwariTotalSaved)}" else "••••",
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "MshwariSaved"
                            ) { text -> Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White) }
                        }
                    }
                    Column {
                        Text("WITHDRAWN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = if (uiState.isBalanceVisible) "KES ${formatCurrency(uiState.mshwariTotalWithdrawn)}" else "••••",
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "MshwariWithdrawn"
                            ) { text -> Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PochiHeroCard(uiState: HomeUiState, onToggleVisibility: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = TransferBlue.copy(alpha = 0.4f),
                ambientColor = Color(0xFF0D47A1).copy(alpha = 0.25f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF0D47A1))))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.1f), Color.Transparent),
                            radius = 500f
                        )
                    )
            )
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pochi la Biashara",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (uiState.isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Spring count-up animation — rolls from 0 → actual balance when revealed.
                // Snaps back to •••••• instantly when hidden. Auto-hide timer lives in ViewModel.
                val animatedBalance by animateFloatAsState(
                    targetValue = if (uiState.isBalanceVisible) uiState.pochiBalance.toFloat() else 0f,
                    animationSpec = if (uiState.isBalanceVisible)
                        spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
                    else tween(0),
                    label = "HeroBalanceSpring"
                )

                if (uiState.isBalanceVisible) {
                    Text(
                        text = "KES ${formatCurrency(animatedBalance.toDouble())}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "KES ••••••",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("RECEIVED", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = AccentGreenLight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = if (uiState.isBalanceVisible) "KES ${formatCurrency(uiState.pochiTotalReceived)}" else "••••",
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "PochiReceived"
                            ) { text -> Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White) }
                        }
                    }
                    Column {
                        Text("SENT", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            AnimatedContent(
                                targetState = if (uiState.isBalanceVisible) "KES ${formatCurrency(uiState.pochiTotalSent)}" else "••••",
                                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                                label = "PochiSent"
                            ) { text -> Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TransactionDetailsSheet(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEditCategory: () -> Unit,
    onShare: () -> Unit,
    onViewPayeeHistory: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Close and Share Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    coroutineScope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
                Text("Transaction details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Hero Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isIncome = transaction.type == TransactionType.RECEIVE_MONEY || transaction.type == TransactionType.MANUAL_INCOME
                val amountText = if (isIncome) "+ KES ${formatCurrency(transaction.amount)}" else "- KES ${formatCurrency(transaction.amount)}"
                val amountColor = if (isIncome) AccentGreenLight else ExpenseRed
                
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = getIconForTransaction(transaction)
                    val tint = if (isIncome) AccentGreenLight else ExpenseRed
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transaction.type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Metadata List
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
            
            MetadataRow(label = "Payee/Party", value = transaction.payee)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditCategory() }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(transaction.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Edit, contentDescription = "Edit Category", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            MetadataRow(label = "Date", value = SimpleDateFormat("EEEE 'at' HH:mm", Locale.getDefault()).format(Date(transaction.timestamp)))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            
            run {
                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("M-PESA Ref", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = transaction.remoteRef,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(transaction.remoteRef))
                                },
                                onLongClick = {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(transaction.remoteRef))
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                            )
                            .semantics {
                                customActions = listOf(
                                    androidx.compose.ui.semantics.CustomAccessibilityAction("Copy M-PESA reference") {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(transaction.remoteRef))
                                        true
                                    }
                                )
                            }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            if (!transaction.accountRef.isNullOrBlank()) {
                MetadataRow(label = "Account No.", value = transaction.accountRef)
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }

            if (transaction.usedFulizaAmount > 0.0) {
                MetadataRow(label = "Fuliza Overdraft", value = "KES ${formatCurrency(transaction.usedFulizaAmount)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
            
            if (transaction.fee > 0.0) {
                MetadataRow(label = "Carrier Fees", value = "KES ${formatCurrency(transaction.fee)}")
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            }
            
            MetadataRow(label = "Balance After", value = "KES ${formatCurrency(transaction.balanceAfter)}")

            if (transaction.originalSms != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Original SMS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = transaction.originalSms ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (onViewPayeeHistory != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onViewPayeeHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View all transactions from ${transaction.payee}")
                }
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

fun getIconForTransaction(transaction: Transaction): androidx.compose.ui.graphics.vector.ImageVector {
    if (transaction.isFeeTransaction) return Icons.AutoMirrored.Filled.ReceiptLong
    
    val payee = transaction.payee.lowercase()
    val category = transaction.category?.lowercase() ?: "other"
    
    return when {
        category == "bank/m-pesa fees" -> Icons.AutoMirrored.Filled.ReceiptLong
        category == "airtime" -> Icons.Default.Phone
        category == "cash" || category == "withdraw" -> Icons.Default.AttachMoney
        category == "transfer" || category == "send money" -> Icons.Default.AttachMoney
        category == "received money" || category == "income" || transaction.type == TransactionType.RECEIVE_MONEY || transaction.type == TransactionType.MANUAL_INCOME -> Icons.AutoMirrored.Filled.TrendingDown
        category == "shopping" || category == "buy goods" -> Icons.Default.ShoppingCart
        category == "bills" || category == "paybill" -> Icons.Default.Payment
        category == "fuliza" -> Icons.Default.AccountBalance
        payee.contains("bank") -> Icons.Default.AccountBalance
        payee.contains("kplc") -> Icons.Default.Bolt
        else -> Icons.AutoMirrored.Filled.TrendingUp
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: (() -> Unit)? = null, onPayeeTap: (() -> Unit)? = null, showDate: Boolean = false) {
    // Skip rendering fee-only records (legacy data from before the fix)
    if (transaction.isFeeTransaction) return

    val isIncome = transaction.type == TransactionType.RECEIVE_MONEY || transaction.type == TransactionType.MANUAL_INCOME
    val color = if (isIncome) AccentGreenLight else ExpenseRed
    val icon = getIconForTransaction(transaction)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color)
        }

        Spacer(modifier = Modifier.width(16.dp))

        val timeStr = if (showDate) dateTimeFormat.format(Date(transaction.timestamp)) else timeFormat.format(Date(transaction.timestamp))
        val typeStr = transaction.type.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        val subtitleStr = buildString {
            append(typeStr)
            if (transaction.category.isNotEmpty() && !transaction.category.equals("Other", ignoreCase = true) && !transaction.category.equals(typeStr, ignoreCase = true)) {
                append("  ·  ${transaction.category}")
            }
            append("  ·  $timeStr")
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.payee,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = if (onPayeeTap != null) Modifier.clickable { onPayeeTap() } else Modifier
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitleStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (transaction.usedFulizaAmount > 0.0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Fuliza Overdraft: KES ${formatCurrency(transaction.usedFulizaAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningOrange,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else "-"}KES ${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (transaction.fee > 0.0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Fee: KES ${formatCurrency(transaction.fee)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

fun getGreetingMessage(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..10 -> "Good Morning 🌅"
        11 -> "Good Day ☕"
        in 12..15 -> "Good Afternoon ☀️"
        16 -> "Hope you're having an amazing day ✨"
        in 17..19 -> "Good evening 🌆"
        else -> "Lovely night 🌙"
    }
}


// ── First-sync branded loader ────────────────────────────────────────────────

@Composable
private fun FirstSyncLoader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BarChartLoader()
        Spacer(modifier = Modifier.height(12.dp))
        CyclingWordText()
    }
}

@Composable
private fun BarChartLoader() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "barLoader")

    // Bar scale definitions: Triple(initialScale, midScale, label)
    // Bar 3 is fixed — no animation needed
    val bar1Scale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 4000
                0.2f at 0
                0.2f at 1600   // 40%
                1.0f at 2000   // 50%
                1.0f at 3600   // 90%
                0.2f at 4000   // 100%
            }
        ),
        label = "bar1"
    )
    val bar2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.4f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 4000
                0.4f at 0
                0.4f at 1600
                0.8f at 2000
                0.8f at 3600
                0.4f at 4000
            }
        ),
        label = "bar2"
    )
    val bar4Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.8f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 4000
                0.8f at 0
                0.8f at 1600
                0.4f at 2000
                0.4f at 3600
                0.8f at 4000
            }
        ),
        label = "bar4"
    )
    val bar5Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 4000
                1.0f at 0
                1.0f at 1600
                0.2f at 2000
                0.2f at 3600
                1.0f at 4000
            }
        ),
        label = "bar5"
    )

    // Ball X offset (dp): 0 → 60 → 0 across the 4s cycle
    val ballX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 4000
                0f at 0
                6f at 200
                12f at 400
                18f at 680
                24f at 800
                30f at 1080
                36f at 1200
                42f at 1480
                48f at 1600  // 40%
                48f at 2000  // 50%
                42f at 2280
                36f at 2400
                30f at 2680
                24f at 2800
                18f at 3080
                12f at 3200
                6f at 3480
                0f at 3600   // 90%
                0f at 4000   // 100%
            }
        ),
        label = "ballX"
    )
    // Ball Y offset (dp): bouncing up over each bar
    val ballY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.keyframes {
                durationMillis = 4000
                0f at 0
                (-14f) at 200
                (-10f) at 400
                (-24f) at 680
                (-20f) at 800
                (-34f) at 1080
                (-30f) at 1200
                (-44f) at 1480
                (-40f) at 1600
                0f at 2000
                (-14f) at 2280
                (-10f) at 2400
                (-24f) at 2680
                (-20f) at 2800
                (-34f) at 3080
                (-30f) at 3200
                (-44f) at 3480
                (-40f) at 3600
                0f at 4000
            }
        ),
        label = "ballY"
    )

    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val barScales = listOf(bar1Scale, bar2Scale, 0.6f, bar4Scale, bar5Scale)

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(80.dp)
    ) {
        // Draw the 5 bars anchored to the bottom
        barScales.forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (index * 12).dp, y = 0.dp)
                    .width(10.dp)
                    .height(40.dp)
                    .graphicsLayer {
                        scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(barColor)
            )
        }

        // Draw the bouncing ball
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(
                    x = ballX.dp,
                    y = (ballY - 10f).dp   // -10 keeps ball bottom at bottom edge when Y=0
                )
                .size(10.dp)
                .clip(CircleShape)
                .background(AccentGreenLight)
        )
    }
}

@Composable
private fun CyclingWordText() {
    val words = listOf("transactions", "SMS history", "savings", "spending")
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1200L)
            currentIndex = (currentIndex + 1) % words.size
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Analysing your",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))

        // Clipped window showing one word at a time
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> height } togetherWith
                            slideOutVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { height -> -height }
                },
                label = "wordAnimation"
            ) { targetIndex ->
                Box(
                    modifier = Modifier.height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = words[targetIndex],
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGreenLight
                    )
                }
            }

            // Fade mask at top and bottom to match CSS ::after gradient
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background,
                            0.2f to Color.Transparent,
                            0.8f to Color.Transparent,
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
            )
        }
    }
}

@Composable
fun InsightsCarousel(insights: List<com.pesalytics.patterns.Insight>) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(insights.size) { index ->
            val insight = insights[index]
            val bgColor = when (insight.type) {
                com.pesalytics.patterns.InsightType.WARNING -> ExpenseRed.copy(alpha = 0.1f)
                com.pesalytics.patterns.InsightType.SUCCESS -> AccentGreenLight.copy(alpha = 0.15f)
                com.pesalytics.patterns.InsightType.INFO -> TransferBlue.copy(alpha = 0.1f)
            }
            val accentColor = when (insight.type) {
                com.pesalytics.patterns.InsightType.WARNING -> ExpenseRed
                com.pesalytics.patterns.InsightType.SUCCESS -> Color(0xFF2E7D32)
                com.pesalytics.patterns.InsightType.INFO -> TransferBlue
            }
            val icon = when (insight.type) {
                com.pesalytics.patterns.InsightType.WARNING -> Icons.Default.Warning
                com.pesalytics.patterns.InsightType.SUCCESS -> Icons.Default.CheckCircle
                com.pesalytics.patterns.InsightType.INFO -> Icons.Default.Info
            }
            
            Card(
                modifier = Modifier.width(260.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = insight.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
