package com.pesalytics.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pesalytics.model.AppNotificationEntity
import com.pesalytics.model.NotificationType
import java.text.SimpleDateFormat
import java.util.*
import com.pesalytics.ui.screens.PesaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: PesaViewModel
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            com.pesalytics.ui.components.PesalyticsTopBar(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You're all caught up!",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Insights and alerts will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Stay updated with your latest alerts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (notifications.isNotEmpty()) {
                                TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                                    Text("Mark all read", color = com.pesalytics.ui.theme.AccentGreenDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                items(notifications, key = { it.id }) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            viewModel.markNotificationAsRead(notification.id)
                            notification.actionRoute?.let { route ->
                                // Optional deep linking inside app
                                // navController.navigate(route)
                            }
                        },
                        onDelete = { viewModel.deleteNotification(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: AppNotificationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (notification.type) {
        NotificationType.BUDGET_WARNING -> Icons.Default.Warning
        NotificationType.DAILY_BRIEF, NotificationType.MONTHLY_REPORT -> Icons.Default.Assessment
        NotificationType.TARIFF_ALERT -> Icons.Default.Info
        NotificationType.MERCHANT_INSIGHT -> Icons.Default.Store
        NotificationType.GOAL_REMINDER -> Icons.Default.Star
        NotificationType.SYSTEM -> Icons.Default.Notifications
    }
    
    val iconTint = when (notification.type) {
        NotificationType.BUDGET_WARNING -> Color(0xFFE53935)
        NotificationType.DAILY_BRIEF, NotificationType.MONTHLY_REPORT -> MaterialTheme.colorScheme.primary
        NotificationType.TARIFF_ALERT -> Color(0xFFFFB300)
        NotificationType.MERCHANT_INSIGHT -> MaterialTheme.colorScheme.tertiary
        NotificationType.GOAL_REMINDER -> Color(0xFF43A047)
        NotificationType.SYSTEM -> MaterialTheme.colorScheme.secondary
    }

    val iconBg = iconTint.copy(alpha = 0.1f)
    val timeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(Date(notification.timestamp))

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (notification.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close, 
                    contentDescription = "Dismiss", 
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}





