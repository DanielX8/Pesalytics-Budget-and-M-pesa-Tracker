package com.pesalytics.ui.screens

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesalytics.R
import com.pesalytics.model.BillCycle
import com.pesalytics.ui.theme.AccentGreenLight
import com.pesalytics.ui.theme.ExpenseRed
import com.pesalytics.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit
) {
    val allBills by viewModel.bills.collectAsStateWithLifecycle()
    val monthlyBills = allBills.filter { it.cycle == BillCycle.MONTHLY }
    var selectedPlan by remember { mutableStateOf("Yearly") }
    var promoCode by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val promoMessage by viewModel.promoMessage.collectAsStateWithLifecycle()
    LaunchedEffect(promoMessage) {
        promoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPromoMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Subscription",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unlock your full financial potential with Pesalytics Premium.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Pricing Grid
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PricingCard(
                            title = "Monthly",
                            price = "KES 299",
                            isSelected = selectedPlan == "Monthly",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPlan = "Monthly" }
                        )
                        PricingCard(
                            title = "Quarterly",
                            price = "KES 699",
                            isSelected = selectedPlan == "Quarterly",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPlan = "Quarterly" }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        PricingCard(
                            title = "Yearly",
                            price = "KES 2,000",
                            badge = "BEST VALUE",
                            isSelected = selectedPlan == "Yearly",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPlan = "Yearly" }
                        )
                        PricingCard(
                            title = "Lifetime",
                            price = "KES 9,999",
                            isSelected = selectedPlan == "Lifetime",
                            modifier = Modifier.weight(1f),
                            onClick = { selectedPlan = "Lifetime" }
                        )
                    }
                }
            }

            // Promo Code
            item {
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    label = { Text("Have a promo code?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Promo Code Apply Button
            item {
                Button(
                    onClick = { if (promoCode.isNotBlank()) viewModel.redeemPromoCode(promoCode.trim()) },
                    enabled = promoCode.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight)
                ) {
                    Text("Apply Code", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Tier Comparison
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Free Tier Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Free Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            FeatureBullet("Basic transaction tracking")
                            FeatureBullet("Limited standard budgets")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("CURRENTLY ACTIVE")
                            }
                        }
                    }

                    // Premium Tier
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pesalytics Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            FeatureBullet("Unlimited transaction tracking", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            FeatureBullet("Smart categorization algorithms", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            FeatureBullet("Cloud sync & backup", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                val activity = context as? android.app.Activity ?: return@Button
                                val sku = when (selectedPlan) {
                                    "Monthly"   -> com.pesalytics.data.billing.BillingConfig.SKU_MONTHLY
                                    "Quarterly" -> com.pesalytics.data.billing.BillingConfig.SKU_QUARTERLY
                                    "Yearly"    -> com.pesalytics.data.billing.BillingConfig.SKU_YEARLY
                                    "Lifetime"  -> com.pesalytics.data.billing.BillingConfig.SKU_LIFETIME
                                    else -> com.pesalytics.data.billing.BillingConfig.SKU_YEARLY
                                }
                                viewModel.subscriptionManager?.launchBillingFlow(activity, sku)
                            },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("UPGRADE TO PREMIUM", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Detailed Comparison Table
            item {
                Text(
                    text = "Feature Comparison",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                            Text("Feature", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("FREE", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("PREMIUM", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        ComparisonRow("Analytics", "Basic (Home only)", "Deep Analytics Suite")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ComparisonRow("Bill Tracker", "—", "Unlimited")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ComparisonRow("Data Export", "—", "CSV & PDF")
                    }
                }
            }

            // My Subscriptions (monthly bills)
            item {
                Text(
                    "My Subscriptions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                Text(
                    "Monthly recurring bills tracked in the Bills screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                if (monthlyBills.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No monthly subscriptions tracked yet.\nAdd them in the Bills tab.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            monthlyBills.forEachIndexed { index, bill ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bill.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(bill.payee, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("KES ${formatCurrency(bill.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.deleteBill(bill) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (index < monthlyBills.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total / month", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("KES ${formatCurrency(monthlyBills.sumOf { it.amount })}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentGreenLight)
                            }
                        }
                    }
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(onClick = { /* Restore Purchases */ }) {
                        Text("Restore Purchases", color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "Subscriptions will automatically renew unless canceled. Manage your subscription through your settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PricingCard(title: String, price: String, badge: String? = null, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (badge != null) 8.dp else 0.dp)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) AccentGreenLight else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = if (isSelected) AccentGreenLight.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(50))
                    .background(AccentGreenLight)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FeatureBullet(text: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = AccentGreenLight, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun ComparisonRow(feature: String, free: String, premium: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text(feature, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(free, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(premium, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentGreenLight)
    }
}
