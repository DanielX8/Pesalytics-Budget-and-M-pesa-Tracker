package com.example.ui.screens

import androidx.compose.ui.graphics.luminance

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Transaction
import com.example.model.TransactionType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.example.ui.theme.AccentGreenDark
import com.example.ui.theme.AccentGreenLight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.TransferBlue
import com.example.ui.theme.WarningOrange
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PesaViewModel,
    onNavigateToAllTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBills: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGoals: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var hasRequestedSmsPermission by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.syncMpesaSms(context)
        }
        hasRequestedSmsPermission = true
    }

    LaunchedEffect(Unit) {
        viewModel.loadUserNameAndFirstLaunch(context)
        if (!hasRequestedSmsPermission) {
            val permission = android.Manifest.permission.READ_SMS
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                viewModel.syncMpesaSms(context)
            } else {
                permissionLauncher.launch(permission)
            }
            hasRequestedSmsPermission = true
        }
    }

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionDetails by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var showCategoryEdit by remember { mutableStateOf(false) }

    if (showAddManualDialog) {
        val addManualSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { true }
        )
        val coroutineScope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showAddManualDialog = false },
            sheetState = addManualSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text("Add Manual Transaction", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                // Just placeholder fields for visual completion requested by user
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Payee/Description") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { 
                    coroutineScope.launch {
                        addManualSheetState.hide()
                        showAddManualDialog = false 
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save Transaction")
                }
            }
        }
    }

    if (showCategoryEdit && selectedTransaction != null) {
        var newCategoryName by remember { mutableStateOf(selectedTransaction!!.category) }
        val predefinedCategories = listOf("Groceries", "Utilities", "Food & Dining", "Transport", "Shopping", "Entertainment", "Health", "Airtime", "Other")
        
        AlertDialog(
            onDismissRequest = { showCategoryEdit = false },
            title = { Text("Edit Category") },
            text = {
                Column {
                    Text("Change category for all past and future transactions from:", style = MaterialTheme.typography.bodySmall)
                    Text(selectedTransaction!!.payee, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
                    FlowRow(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        label = { Text("Custom Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                    putExtra(android.content.Intent.EXTRA_TEXT, "PesaSense Transaction Receipt:\nRef: ${selectedTransaction!!.remoteRef}\nPayee: ${selectedTransaction!!.payee}\nAmount: KES ${formatCurrency(selectedTransaction!!.amount)}")
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
        )
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("PesaSense", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { notificationsExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
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
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddManualDialog = true },
                containerColor = AccentGreenDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(16.dp).size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(32.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        val groupedRecents = remember(uiState.recentTransactions) {
            uiState.recentTransactions.groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                val currentCal = Calendar.getInstance()
                val format = SimpleDateFormat("dd MMM", Locale.getDefault())
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                
                when {
                    cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR) -> "TODAY"
                    cal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR) - 1 -> "YESTERDAY"
                    currentCal.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) < 7 -> dayFormat.format(cal.time).uppercase(Locale.getDefault())
                    else -> format.format(cal.time).uppercase(Locale.getDefault())
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "${getGreetingMessage()},",
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
            
            item {
                val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                val selectedMonthIndex by viewModel.selectedMonthIndex.collectAsStateWithLifecycle()
                val selectedMonth = months.getOrNull(selectedMonthIndex) ?: months.first()
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedMonthIndex - 1))
                
                androidx.compose.foundation.lazy.LazyRow(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(months.size) { index ->
                        val month = months[index]
                        val isSelected = index == selectedMonthIndex
                        Box(
                            modifier = Modifier
                                .shadow(2.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.setSelectedMonth(index) }
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

            item {
                Box(modifier = Modifier.animateItem()) {
                    HeroCard(uiState = uiState, onToggleVisibility = { viewModel.toggleBalanceVisibility() })
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickNavButton(icon = Icons.Default.Insights, label = "Analytics", color = TransferBlue, onClick = onNavigateToAnalytics)
                    QuickNavButton(icon = Icons.Default.ReceiptLong, label = "Bills", color = AccentGreenDark, onClick = onNavigateToBills)
                    QuickNavButton(icon = Icons.Default.DonutLarge, label = "Budget", color = ExpenseRed, onClick = onNavigateToSettings)
                    QuickNavButton(icon = Icons.Default.TrackChanges, label = "Goals", color = IncomeGreen, onClick = onNavigateToGoals)
                }
            }

            if (uiState.hasBudget) {
                item {
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

            item {
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
            item {
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
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val startColor = if (isLightMode) androidx.compose.ui.graphics.Color(0xFF55D687) else androidx.compose.ui.graphics.Color(0xFF348C55)
    val endColor = if (isLightMode) androidx.compose.ui.graphics.Color(0xFF348C55) else androidx.compose.ui.graphics.Color(0xFF1A4D2E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Glowing card shadow — the single biggest visual upgrade
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = AccentGreenLight.copy(alpha = 0.4f),
                ambientColor = AccentGreenDark.copy(alpha = 0.25f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(startColor, endColor)))
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
                            contentDescription = "Toggle Balance",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // Animated balance — crossfades when value changes or visibility toggles
                AnimatedContent(
                    targetState = if (uiState.isBalanceVisible) "KES ${formatCurrency(uiState.currentBalance)}" else "KES ••••••",
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                            slideInVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) { it / 2 }) togetherWith
                            (fadeOut(animationSpec = tween(200)) +
                                slideOutVertically(animationSpec = tween(200)) { -it / 2 })
                    },
                    label = "BalanceAmount"
                ) { balanceText ->
                    Text(
                        text = balanceText,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("MONEY IN", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGreenLight)
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

                    Column {
                        Text("MONEY OUT", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = ExpenseRed)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEditCategory: () -> Unit,
    onShare: () -> Unit
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
                    modifier = Modifier.clickable {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(transaction.remoteRef))
                    }
                )
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

@Composable
fun getIconForTransaction(transaction: Transaction): androidx.compose.ui.graphics.vector.ImageVector {
    if (transaction.isFeeTransaction) return Icons.Default.ReceiptLong
    
    val payee = transaction.payee.lowercase()
    val category = transaction.category?.lowercase() ?: "other"
    
    return when {
        category == "bank/m-pesa fees" -> Icons.Default.ReceiptLong
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
fun TransactionItem(transaction: Transaction, onClick: (() -> Unit)? = null) {
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
        
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeStr = format.format(Date(transaction.timestamp))
        val typeStr = transaction.type.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        
        val subtitleStr = buildString {
            append(typeStr)
            if (transaction.category.isNotEmpty() && !transaction.category.equals("Other", ignoreCase = true) && !transaction.category.equals(typeStr, ignoreCase = true)) {
                append("  ·  ${transaction.category}")
            }
            append("  ·  $timeStr")
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = transaction.payee, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
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

fun formatCurrency(amount: Double): String {
    return NumberFormat.getInstance().apply { 
        minimumFractionDigits = 2
        maximumFractionDigits = 2 
    }.format(amount)
}

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


