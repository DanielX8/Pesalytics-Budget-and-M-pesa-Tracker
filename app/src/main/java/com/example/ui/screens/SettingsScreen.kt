package com.pesasense.ui.screens

import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolunteerActivism
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
import androidx.compose.ui.unit.dp
import com.pesasense.R
import com.pesasense.ui.theme.AccentGreenLight
import com.pesasense.ui.theme.AccentGreenDark
import com.pesasense.ui.theme.WarningOrange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesasense.model.ThemeMode
import com.pesasense.ui.theme.ExpenseRed
import androidx.compose.foundation.border
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
    onNavigateToFaq: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    val billAlertsEnabled by viewModel.billAlertsEnabled.collectAsStateWithLifecycle()
    val budgetAlertsEnabled by viewModel.budgetAlertsEnabled.collectAsStateWithLifecycle()
    val goalRemindersEnabled by viewModel.goalRemindersEnabled.collectAsStateWithLifecycle()
    val highSpendingAlertsEnabled by viewModel.highSpendingAlertsEnabled.collectAsStateWithLifecycle()
    val smartAlertsEnabled by viewModel.smartAlertsEnabled.collectAsStateWithLifecycle()

    var themeSelection by remember { mutableStateOf("System") }
    val themeOptions = listOf("Light", "Dark", "System")

    val savedFrequency = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        .getString("report_frequency", "Daily") ?: "Daily"
    var notificationFrequency by remember { mutableStateOf(savedFrequency) }
    val frequencyOptions = listOf("Daily", "Weekly", "Monthly")

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatarIndex by viewModel.userAvatar.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    var showEditNameDialog by remember { mutableStateOf(false) }

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

            }
        },
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Manage your preferences and profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 1. Profile Card
            item {
                if (showEditNameDialog) {
                    var tempName by remember { mutableStateOf(userName) }
                    val context = androidx.compose.ui.platform.LocalContext.current
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
                                    if (tempName.isNotBlank()) {
                                        viewModel.setUserName(tempName.trim(), context)
                                    }
                                    showEditNameDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                }

                val avatarIcons = listOf(
                    Icons.Rounded.Person,
                    Icons.Rounded.Face,
                    Icons.Rounded.SentimentSatisfied,
                    Icons.Rounded.CrueltyFree,
                    Icons.Rounded.Pets
                )
                val currentAvatar = avatarIcons.getOrNull(userAvatarIndex) ?: Icons.Rounded.Person
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(AccentGreenLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(currentAvatar, contentDescription = null, modifier = Modifier.size(30.dp), tint = AccentGreenDark)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FINANCIAL IDENTITY", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = AccentGreenDark,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = userName, 
                                style = MaterialTheme.typography.titleLarge, 
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showEditNameDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Edit Profile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val sharedPrefs = context.getSharedPreferences("PesaSensePrefs", android.content.Context.MODE_PRIVATE)
                var installDate = sharedPrefs.getLong("install_date", 0L)
                if (installDate == 0L) {
                    installDate = System.currentTimeMillis()
                    sharedPrefs.edit().putLong("install_date", installDate).apply()
                }
                val diffMs = System.currentTimeMillis() - installDate
                val daysPassed = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMs)
                val daysRemaining = maxOf(0L, 45L - daysPassed)

                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = "PLAN", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentGreenDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToSubscription() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF4CAF50)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("PREMIUM TRIAL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F5B1A), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (daysRemaining > 0) "Expires in $daysRemaining days" else "Trial Expired", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text("Free Trial", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onNavigateToSubscription() },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F5B1A)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F5B1A)),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Manage Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onNavigateToSubscription() },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F5B1A), contentColor = Color.White),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Compare plans", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.syncMpesaSms(context)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        val permission = android.Manifest.permission.READ_SMS
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            viewModel.syncMpesaSms(context)
                        } else {
                            permissionLauncher.launch(permission)
                        }
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sync MPESA Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Read SMS to update transactions locally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha=0.8f))
                        }
                    }
                }
            }


            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = "FINANCIAL FRAMEWORK", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentGreenDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Goals Card
                        Card(
                            modifier = Modifier.weight(1f).clickable { onNavigateToFinancialGoals() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.TrackChanges,
                                        contentDescription = "Goals",
                                        tint = AccentGreenDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    val activeGoalsCount = goals.size
                                    val goalsText = if (activeGoalsCount == 0) "Set up" else "$activeGoalsCount Active"
                                    val goalsBgColor = if (activeGoalsCount == 0) Color(0xFFF1F5F9) else Color(0xFFE0E7FF)
                                    val goalsTextColor = if (activeGoalsCount == 0) Color(0xFF64748B) else Color(0xFF3730A3)
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(goalsBgColor)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(goalsText, style = MaterialTheme.typography.labelSmall, color = goalsTextColor, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Strategy & Tracking", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Budget Card
                        Card(
                            modifier = Modifier.weight(1f).clickable { onNavigateToBudgetPlanner() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBalanceWallet,
                                        contentDescription = "Budget",
                                        tint = AccentGreenDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    val budgetPct = if (uiState.currentBudgetLimit > 0) uiState.monthlyExpense / uiState.currentBudgetLimit else 0.0
                                    val budgetText = when {
                                        !uiState.hasBudget -> "Not Set"
                                        budgetPct >= 0.9 -> "Critical"
                                        budgetPct >= 0.75 -> "Warning"
                                        else -> "On Track"
                                    }
                                    val budgetBgColor = when {
                                        !uiState.hasBudget -> Color(0xFFF1F5F9)
                                        budgetPct >= 0.9 -> Color(0xFFFFE4E6)
                                        budgetPct >= 0.75 -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFDCFCE7)
                                    }
                                    val budgetTextColor = when {
                                        !uiState.hasBudget -> Color(0xFF64748B)
                                        budgetPct >= 0.9 -> Color(0xFFBE123C)
                                        budgetPct >= 0.75 -> Color(0xFFB45309)
                                        else -> Color(0xFF166534)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(budgetBgColor)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(budgetText, style = MaterialTheme.typography.labelSmall, color = budgetTextColor, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Budget", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Monthly Allocation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // 4. Appearance & Data Export
            item {
                val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
                
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = "APPEARANCE", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentGreenDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ThemeMode.values().forEach { mode ->
                                val option = mode.name.lowercase().replaceFirstChar { it.uppercase() }
                                val isSelected = currentTheme == mode

                                val icon = when (mode) {
                                    ThemeMode.LIGHT -> Icons.Default.WbSunny
                                    ThemeMode.DARK -> Icons.Default.Nightlight
                                    ThemeMode.SYSTEM -> Icons.Default.DesktopMac
                                }

                                val containerColor = if (isSelected) AccentGreenLight.copy(alpha = 0.1f) else Color.Transparent
                                val contentColor = if (isSelected) AccentGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                                val borderColor = if (isSelected) AccentGreenLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                                var buttonCenter by remember(mode) { mutableStateOf(Offset.Zero) }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(containerColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .onGloballyPositioned { coords ->
                                            buttonCenter = coords.boundsInWindow().center
                                        }
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
            // 5. Notifications
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = "NOTIFICATIONS", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentGreenDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Upcoming Bill Alerts", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = billAlertsEnabled, onCheckedChange = { viewModel.setNotificationPref("bill_alerts", it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Budget Threshold Alerts", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = budgetAlertsEnabled, onCheckedChange = { viewModel.setNotificationPref("budget_alerts", it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Goal Reminders", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = goalRemindersEnabled, onCheckedChange = { viewModel.setNotificationPref("goal_reminders", it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("High Spending Alerts", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = highSpendingAlertsEnabled, onCheckedChange = { viewModel.setNotificationPref("high_spending", it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text("Smart Alerts", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = smartAlertsEnabled, onCheckedChange = { viewModel.setNotificationPref("smart_alerts", it, context) }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                            }
                        }
                    }
                }
            }

            item {
                Text("Report Frequency", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val reports = listOf("Daily", "Weekly", "Monthly")
                    reports.forEach { option ->
                        val isSelected = notificationFrequency == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreenLight else Color.Transparent)
                                .clickable {
                                    notificationFrequency = option
                                    context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
                                        .edit().putString("report_frequency", option).apply()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(option, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // 6. Data Portability
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
                    Text(
                        text = "DATA EXPORT", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentGreenDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE0E7FF)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, tint = Color(0xFF166534)) // green icon
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Data Portability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("Export transaction history", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E7FF)).clickable {
                                    val file = com.pesasense.utils.CsvExportHelper.exportToCsv(context, uiState.transactions)
                                    viewModel.addNotification(if (file != null) "CSV saved to Downloads" else "Export failed")
                                    if (file != null) android.widget.Toast.makeText(context, "CSV exported successfully", android.widget.Toast.LENGTH_SHORT).show()
                                }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(".CSV", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                                }
                                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE0E7FF)).clickable {
                                    com.pesasense.utils.PdfExportHelper.generatePdf(context, uiState.transactions) {
                                        viewModel.addNotification("Print dialog opened for PDF generation.")
                                        android.widget.Toast.makeText(context, "PDF generated successfully", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(".PDF", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 7. Support
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = "SUPPORT", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentGreenDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SupportListItem(Icons.Rounded.VolunteerActivism, "Support the Developer", "Tip jar") {}
                        }
                    }
                }
            }

            // 8. About & Help Centre
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)) {
                    Text(
                        text = "ABOUT & HELP CENTRE", 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AccentGreenDark,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SupportListItem(Icons.Rounded.Star, "Rate App and Review", null) {}
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            SupportListItem(Icons.Rounded.People, "Refer a Friend", null) {}
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            SupportListItem(Icons.Rounded.Share, "Share App", null) {}
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            SupportListItem(Icons.Rounded.Description, "Privacy Policy", null) {}
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            SupportListItem(Icons.Rounded.Description, "Terms of Service", null) {}
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            SupportListItem(Icons.Rounded.Info, "FAQ", null) { onNavigateToFaq() }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                            SupportListItem(Icons.Rounded.ChatBubbleOutline, "Live Support Chat with AI", "Coming Soon") {}
                        }
                    }
                }
            }

            // 9. Footer Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    0.0f to Color(0xFF031006),
                                    0.35f to Color(0xFF082612),
                                    0.47f to Color(0xFF135224),
                                    0.5f to Color(0xFF39FF14), // bright neon glow
                                    0.53f to Color(0xFF135224),
                                    0.65f to Color(0xFF082612),
                                    1.0f to Color(0xFF031006),
                                    start = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY),
                                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.header_logo),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Financial Discretion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your privacy is our ultimate premium feature.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE5E7EB))
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Version 1.0.0", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SupportListItem(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

