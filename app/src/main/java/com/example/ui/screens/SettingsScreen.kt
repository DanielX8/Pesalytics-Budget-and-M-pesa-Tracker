package com.example.ui.screens

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
import com.example.R
import com.example.ui.theme.AccentGreenLight
import com.example.ui.theme.AccentGreenDark
import com.example.ui.theme.WarningOrange
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ThemeMode
import com.example.ui.theme.ExpenseRed
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToBudgetPlanner: () -> Unit,
    onNavigateToFinancialGoals: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    var smartAlertsEnabled by remember { mutableStateOf(true) }
    var upcomingBillAlerts by remember { mutableStateOf(true) }
    var budgetAlerts by remember { mutableStateOf(true) }
    var goalReminders by remember { mutableStateOf(false) }

    var themeSelection by remember { mutableStateOf("System") }
    val themeOptions = listOf("Light", "Dark", "System")

    var notificationFrequency by remember { mutableStateOf("Weekly") }
    val frequencyOptions = listOf("Daily", "Weekly", "Monthly")

    val userName by viewModel.userName.collectAsStateWithLifecycle()
    var showEditNameDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadUserNameAndFirstLaunch(context)
    }

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
                Text("SETTINGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp))
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tap to link Google Account", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(
                            onClick = { showEditNameDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AccentGreenLight)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.onPrimary)
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
            // 2. Subscription Plan
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToSubscription() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Free Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Essential tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(modifier = Modifier.background(AccentGreenLight.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = AccentGreenLight, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("CURRENT PRICE: KES 0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item {
                Text("Plan & Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }

            // 3. Plan & Goals
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Budget Planner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("CURRENT LIMIT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("KES 45,000", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Smart Alerts Enabled", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = smartAlertsEnabled, onCheckedChange = { smartAlertsEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { onNavigateToBudgetPlanner() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight), shape = RoundedCornerShape(8.dp)) {
                            Text("Configure Limits")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToFinancialGoals() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("SAVINGS GOAL AND DEBT PAYOFF TRACKER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AccentGreenLight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View all active trackers", style = MaterialTheme.typography.bodyMedium, color = AccentGreenLight, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 4. Appearance & Data Export
            item {
                val currentTheme by viewModel.themeMode.collectAsStateWithLifecycle()
                
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "APPEARANCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        
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
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(containerColor)
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.themeMode.value = mode }
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
            item {
                Text("Data Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(8.dp)) {
                        Text("Export as CSV")
                    }
                    Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(8.dp)) {
                        Text("Export as PDF")
                    }
                }
            }

            // 5. Notifications
            item {
                Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("Upcoming Bill Alerts", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = upcomingBillAlerts, onCheckedChange = { upcomingBillAlerts = it }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("Budget Alerts (nearing limits)", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = budgetAlerts, onCheckedChange = { budgetAlerts = it }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("Goal Reminders", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = goalReminders, onCheckedChange = { goalReminders = it }, colors = SwitchDefaults.colors(checkedThumbColor = AccentGreenLight, checkedTrackColor = AccentGreenLight.copy(alpha=0.5f)))
                        }
                    }
                }
            }

            item {
                Text("Summary Notifications", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), horizontalArrangement = Arrangement.SpaceEvenly) {
                    frequencyOptions.forEach { option ->
                        val isSelected = notificationFrequency == option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreenLight else Color.Transparent)
                                .clickable { notificationFrequency = option }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(option, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // 6. About & Support
            item {
                Text("About & Support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SupportListItem(Icons.Default.VolunteerActivism, "Support the Developer", "Tip jar & 0719713362")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        SupportListItem(Icons.Default.People, "Refer a Friend", "Get 1 month Premium")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        SupportListItem(Icons.Default.Star, "App Rating & Review", null)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        SupportListItem(Icons.Default.Description, "Privacy Policy & Terms", null)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        SupportListItem(Icons.Default.ContactSupport, "Contact Support", null)
                    }
                }
            }
            
            // 7. Footer Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGreenLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGreenLight)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Financial Discretion", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Your privacy is our ultimate premium feature.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SupportListItem(icon: ImageVector, title: String, subtitle: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
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
