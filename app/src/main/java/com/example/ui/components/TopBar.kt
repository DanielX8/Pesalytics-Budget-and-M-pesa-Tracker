package com.pesalytics.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesalytics.R
import com.pesalytics.ui.screens.PesaViewModel
import com.pesalytics.ui.theme.ExpenseRed
import com.pesalytics.ui.theme.AccentGreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesalyticsTopBar(
    viewModel: PesaViewModel,
    titleContent: @Composable () -> Unit = {
        Image(
            painter = painterResource(id = R.drawable.header_logo),
            contentDescription = "Pesalytics",
            modifier = Modifier.height(32.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    },
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    var notificationsExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = titleContent,
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            actions()
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
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", modifier = Modifier.size(20.dp), tint = AccentGreenDark)
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
                            onClick = { notificationsExpanded = false }
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
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}
