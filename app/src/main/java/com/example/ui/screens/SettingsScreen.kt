package com.pesasense.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.DesktopMac
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pesasense.BuildConfig
import com.pesasense.R
import com.pesasense.ui.theme.AccentGreenLight
import com.pesasense.ui.theme.WarningOrange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesasense.model.ThemeMode
import com.pesasense.ui.theme.ExpenseRed
import com.pesasense.util.AppLinks
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToBudgetPlanner: () -> Unit,
    onNavigateToFinancialGoals: () -> Unit,
    onNavigateToFaq: () -> Unit,
    onNavigateToNeedsWants: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    val billAlertsEnabled by viewModel.billAlertsEnabled.collectAsStateWithLifecycle()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val goalRemindersEnabled by viewModel.goalRemindersEnabled.collectAsStateWithLifecycle()
    val highSpendingAlertsEnabled by viewModel.highSpendingAlertsEnabled.collectAsStateWithLifecycle()
    val smartAlertsEnabled by viewModel.smartAlertsEnabled.collectAsStateWithLifecycle()

    val savedFrequency = context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
        .getString("report_frequency", "Daily") ?: "Daily"
    var notificationFrequency by remember { mutableStateOf(savedFrequency) }

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatarIndex by viewModel.userAvatar.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncProgress by viewModel.syncProgress.collectAsStateWithLifecycle()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showExportGateDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showTipJarDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
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
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Manage your preferences and profile.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { ProfileCard(userName, userAvatarIndex) { showEditNameDialog = true } }

            item { PlanCard(viewModel, isPremium, onNavigateToSubscription) }

            item { SyncCard(isSyncing, syncProgress) {
                val permission = android.Manifest.permission.READ_SMS
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    viewModel.syncMpesaSms(context)
                }
            } }

            item { SettingsSection("FINANCIAL FRAMEWORK") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FrameworkCard("Goals", "Strategy & Tracking", Icons.Rounded.TrackChanges,
                        badgeText = if (goals.isEmpty()) "Set up" else "${goals.size} Active",
                        badgeColor = if (goals.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else AccentGreenLight,
                        modifier = Modifier.weight(1f), onClick = onNavigateToFinancialGoals)
                    val budgetPct = if (uiState.currentBudgetLimit > 0) uiState.monthlyExpense / uiState.currentBudgetLimit else 0.0
                    val budgetBadge = when {
                        !uiState.hasBudget -> "Not Set"
                        budgetPct >= 0.9 -> "Critical"
                        budgetPct >= 0.75 -> "Warning"
                        else -> "On Track"
                    }
                    val budgetColor = when {
                        !uiState.hasBudget -> MaterialTheme.colorScheme.onSurfaceVariant
                        budgetPct >= 0.9 -> ExpenseRed
                        budgetPct >= 0.75 -> WarningOrange
                        else -> AccentGreenLight
                    }
                    FrameworkCard("Budget", "Monthly Allocation", Icons.Rounded.AccountBalanceWallet,
                        badgeText = budgetBadge, badgeColor = budgetColor,
                        modifier = Modifier.weight(1f), onClick = onNavigateToBudgetPlanner)
                }
            } }

            item { SettingsSection("SPENDING INSIGHTS") {
                SettingsCard {
                    SupportListItem(Icons.Rounded.Balance, "Needs vs Wants", "Classify your categories", onNavigateToNeedsWants)
                }
            } }

            item { AppearanceSection(viewModel, context) }

            item { SettingsSection("NOTIFICATIONS") {
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                        ToggleRow("Upcoming Bill Alerts", billAlertsEnabled) { viewModel.setNotificationPref("bill_alerts", it, context) }
                        SettingsDivider()
                        ToggleRow("Budget Threshold Alerts", budgetAlertsEnabled) { viewModel.setNotificationPref("budget_alerts", it, context) }
                        SettingsDivider()
                        ToggleRow("Goal Reminders", goalRemindersEnabled) { viewModel.setNotificationPref("goal_reminders", it, context) }
                        SettingsDivider()
                        ToggleRow("High Spending Alerts", highSpendingAlertsEnabled) { viewModel.setNotificationPref("high_spending", it, context) }
                        SettingsDivider()
                        ToggleRow("Smart Alerts", smartAlertsEnabled) { viewModel.setNotificationPref("smart_alerts", it, context) }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Report Frequency", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                SegmentedRow(listOf("Daily", "Weekly", "Monthly"), notificationFrequency) { option ->
                    notificationFrequency = option
                    context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE).edit().putString("report_frequency", option).apply()
                }
            } }

            item { SettingsSection("DATA EXPORT") {
                SettingsCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(AccentGreenLight.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, tint = HeroGreen)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Data Portability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Export transaction history", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExportChip(".CSV") {
                                if (!isPremium) showExportGateDialog = true
                                else {
                                    val file = com.pesasense.utils.CsvExportHelper.exportToCsv(context, uiState.transactions)
                                    viewModel.addNotification(if (file != null) "CSV saved to Downloads" else "Export failed")
                                    if (file != null) android.widget.Toast.makeText(context, "CSV exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            ExportChip(".PDF") {
                                if (!isPremium) showExportGateDialog = true
                                else com.pesasense.utils.PdfExportHelper.generatePdf(context, uiState.transactions) {
                                    viewModel.addNotification("Print dialog opened for PDF generation.")
                                    android.widget.Toast.makeText(context, "PDF generated successfully", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            } }

            // Danger zone
            item {
                Column {
                    Text("DANGER ZONE", style = MaterialTheme.typography.labelSmall, color = ExpenseRed, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showDeleteAllDialog = true }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete All Data", style = MaterialTheme.typography.bodyLarge, color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                Text("Permanently remove all transactions and settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { SettingsSection("SUPPORT") {
                SettingsCard {
                    SupportListItem(Icons.Rounded.VolunteerActivism, "Support the Developer", "Tip jar") { showTipJarDialog = true }
                }
            } }

            item { SettingsSection("ABOUT & HELP CENTRE") {
                SettingsCard {
                    SupportListItem(Icons.Rounded.Star, "Rate App and Review", null) { rateApp(context) }
                    SettingsDivider(padded = true)
                    SupportListItem(Icons.Rounded.People, "Refer a Friend", null) { shareText(context, AppLinks.SHARE_MESSAGE) }
                    SettingsDivider(padded = true)
                    SupportListItem(Icons.Rounded.Share, "Share App", null) { shareText(context, AppLinks.SHARE_MESSAGE) }
                    SettingsDivider(padded = true)
                    SupportListItem(Icons.Rounded.Description, "Privacy Policy", null) { openUrl(context, AppLinks.PRIVACY_POLICY_URL) }
                    SettingsDivider(padded = true)
                    SupportListItem(Icons.Rounded.Description, "Terms of Service", null) { openUrl(context, AppLinks.TERMS_OF_SERVICE_URL) }
                    SettingsDivider(padded = true)
                    SupportListItem(Icons.Rounded.Info, "FAQ", null) { onNavigateToFaq() }
                    SettingsDivider(padded = true)
                    SupportListItem(Icons.Rounded.ChatBubbleOutline, "Live Support Chat with AI", "Coming Soon") {}
                }
            } }

            item { FooterBanner() }

            item {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showEditNameDialog) {
        var tempName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Display Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) viewModel.setUserName(tempName.trim(), context)
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showExportGateDialog) {
        AlertDialog(
            onDismissRequest = { showExportGateDialog = false },
            title = { Text("Export to CSV or PDF") },
            text = { Text("Export your transactions as CSV or PDF — available with Premium.") },
            confirmButton = { Button(onClick = { showExportGateDialog = false; onNavigateToSubscription() }) { Text("View Premium") } },
            dismissButton = { TextButton(onClick = { showExportGateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed) },
            title = { Text("Delete all data?") },
            text = { Text("This permanently removes all transactions, bills, budgets, goals and category rules from this device. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteAllData(context); showDeleteAllDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) { Text("Delete Everything", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTipJarDialog) {
        AlertDialog(
            onDismissRequest = { showTipJarDialog = false },
            icon = { Icon(Icons.Rounded.VolunteerActivism, contentDescription = null, tint = HeroGreen) },
            title = { Text("Support the Developer") },
            text = { Text("If PesaSense helps you, you can send a tip via M-PESA to:\n\n${AppLinks.TIP_JAR_MPESA}\n\nThank you! 💚") },
            confirmButton = { Button(onClick = { showTipJarDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight)) { Text("Close", color = Color.White) } }
        )
    }
}

// ── Section helpers ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = HeroGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        content()
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().softCard(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsDivider(padded: Boolean = false) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = if (padded) Modifier.padding(horizontal = 16.dp) else Modifier
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun SegmentedRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.SpaceEvenly) {
        options.forEach { option ->
            val isSelected = selected == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AccentGreenLight else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(option, style = MaterialTheme.typography.bodySmall, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun ExportChip(label: String, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AccentGreenLight.copy(alpha = 0.15f)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HeroGreen, fontWeight = FontWeight.SemiBold)
    }
}

// ── Cards ────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileCard(userName: String, avatarIndex: Int, onEdit: () -> Unit) {
    val avatarIcons = listOf(Icons.Rounded.Person, Icons.Rounded.Face, Icons.Rounded.SentimentSatisfied, Icons.Rounded.CrueltyFree, Icons.Rounded.Pets)
    val currentAvatar = avatarIcons.getOrNull(avatarIndex) ?: Icons.Rounded.Person
    SettingsCard {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(AccentGreenLight.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(currentAvatar, contentDescription = null, modifier = Modifier.size(30.dp), tint = HeroGreen)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("FINANCIAL IDENTITY", style = MaterialTheme.typography.labelSmall, color = HeroGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = HeroGreen), shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanCard(viewModel: PesaViewModel, isPremium: Boolean, onNavigateToSubscription: () -> Unit) {
    val subState by viewModel.subscriptionStateFlow.collectAsStateWithLifecycle()
    val daysRemaining = subState.trialDaysRemaining.toLong()
    val gradient = rememberBrandGradient()
    SettingsSection("PLAN") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .brandGlow(elevation = 18.dp, radius = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(gradient)
                .clickableScale { onNavigateToSubscription() }
        ) {
            // Subtle radial sheen for depth, mirroring the Dashboard hero card.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                            radius = 600f
                        )
                    )
            )
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SubscriptionTierBadge(isPremium = isPremium, isTrial = !isPremium && daysRemaining > 0, trialDaysLeft = daysRemaining.toInt())
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isPremium) "Active" else if (daysRemaining > 0) "Expires in $daysRemaining days" else "Trial Expired",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(if (isPremium) "Premium" else "Free Trial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                        Text(if (isPremium) "Manage Plan" else "Compare Plans", fontWeight = FontWeight.Bold, color = HeroGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncCard(isSyncing: Boolean, syncProgress: Int, onSync: () -> Unit) {
    // Uses the same brand hero gradient as the Plan card so the green matches the rest of the
    // app (previously a flat primary that read too light in dark / too dark in light mode).
    val gradient = rememberBrandGradient()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .brandGlow(elevation = 14.dp, radius = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickableScale { if (!isSyncing) onSync() }
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = isSyncing,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "sync-icon"
            ) { syncing ->
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (isSyncing) "Syncing…" else "Sync MPESA Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                AnimatedContent(
                    targetState = if (isSyncing) "$syncProgress new records found" else "Read SMS to update transactions locally",
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "sync-subtitle"
                ) { subtitle ->
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun FrameworkCard(title: String, subtitle: String, icon: ImageVector, badgeText: String, badgeColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.softCard(16.dp).clickableScale { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = HeroGreen, modifier = Modifier.size(24.dp))
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(badgeColor.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppearanceSection(viewModel: PesaViewModel, context: Context) {
    val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
    SettingsSection("APPEARANCE") {
        SettingsCard {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ThemeMode.values().forEach { mode ->
                    val option = mode.name.lowercase().replaceFirstChar { it.uppercase() }
                    val isSelected = currentTheme == mode
                    val icon = when (mode) {
                        ThemeMode.LIGHT -> Icons.Default.WbSunny
                        ThemeMode.DARK -> Icons.Default.Nightlight
                        ThemeMode.SYSTEM -> Icons.Default.DesktopMac
                    }
                    val contentColor = if (isSelected) HeroGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    val borderColor = if (isSelected) AccentGreenLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                    var buttonCenter by remember(mode) { mutableStateOf(Offset.Zero) }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentGreenLight.copy(alpha = 0.1f) else Color.Transparent)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .onGloballyPositioned { coords -> buttonCenter = coords.boundsInWindow().center }
                            .clickable { viewModel.setThemeMode(mode, context, buttonCenter) }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = option, tint = contentColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(option, style = MaterialTheme.typography.bodyMedium, color = contentColor, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterBanner() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    // Brand-aligned diagonal glow: deep brand green → bright accent → deep green.
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        0.0f to Color(0xFF031006),
                        0.3f to HeroGreen,
                        0.5f to AccentGreenLight,
                        0.7f to HeroGreen,
                        1.0f to Color(0xFF031006),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = androidx.compose.ui.res.painterResource(id = R.drawable.header_logo), contentDescription = null, modifier = Modifier.size(48.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Financial Discretion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your privacy is our ultimate premium feature.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE5E7EB))
            }
        }
    }
}

@Composable
fun SupportListItem(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickableScale(pressedScale = 0.99f) { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SubscriptionTierBadge(isPremium: Boolean, isTrial: Boolean = false, trialDaysLeft: Int = 0) {
    // Rendered on the green gradient hero card, so the chip is an opaque white pill with
    // strong-colored text for clear contrast in both light and dark mode.
    val (text, contentColor) = when {
        isPremium -> "Premium ✓" to HeroGreen
        isTrial -> "Trial — $trialDaysLeft days left" to WarningOrange
        else -> "Free plan" to Color(0xFF444444)
    }
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = contentColor, fontWeight = FontWeight.Bold)
    }
}

// ── Intent helpers ───────────────────────────────────────────────────────────

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share PesaSense")) }
}

private fun rateApp(context: Context) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.PLAY_STORE_MARKET_URI))) }
        .onFailure { openUrl(context, AppLinks.PLAY_STORE_WEB_URL) }
}
