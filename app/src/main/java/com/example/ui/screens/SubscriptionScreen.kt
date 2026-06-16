package com.pesalytics.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import com.pesalytics.ui.theme.AccentGreenDark
import com.pesalytics.ui.theme.AccentGreenLight
import com.pesalytics.ui.theme.ExpenseRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit
) {
    val allBills by viewModel.bills.collectAsStateWithLifecycle()
    val monthlyBills = allBills.filter { it.cycle == BillCycle.MONTHLY }
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
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
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isPremium) "You're Premium" else "Pesalytics Premium",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isPremium) "Thanks for supporting an ad-free, private finance app."
                        else "Unlock deep analytics, unlimited budgets and bills — no ads, ever.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Pricing grid (hidden once premium)
            if (!isPremium) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            PricingCard("Monthly", "KES 299", isSelected = selectedPlan == "Monthly", modifier = Modifier.weight(1f)) { selectedPlan = "Monthly" }
                            PricingCard("Quarterly", "KES 699", isSelected = selectedPlan == "Quarterly", modifier = Modifier.weight(1f)) { selectedPlan = "Quarterly" }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            PricingCard("Yearly", "KES 2,000", badge = "BEST VALUE", isSelected = selectedPlan == "Yearly", modifier = Modifier.weight(1f)) { selectedPlan = "Yearly" }
                            PricingCard("Lifetime", "KES 9,999", isSelected = selectedPlan == "Lifetime", modifier = Modifier.weight(1f)) { selectedPlan = "Lifetime" }
                        }
                    }
                }

                // Promo code
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = promoCode,
                            onValueChange = { promoCode = it },
                            label = { Text("Have a promo code?") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { if (promoCode.isNotBlank()) viewModel.redeemPromoCode(promoCode.trim()) },
                            enabled = promoCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreenLight)
                        ) { Text("Apply Code", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // Tier comparison cards
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Free Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            FeatureBullet("Automated M-PESA SMS parsing")
                            FeatureBullet("Manual cash & non-M-PESA entry")
                            FeatureBullet("Spending Cap (global monthly limit)")
                            FeatureBullet("1 financial goal")
                            if (!isPremium) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) { Text("CURRENTLY ACTIVE") }
                            }
                        }
                    }

                    val gradient = rememberBrandGradient()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .brandGlow(elevation = 18.dp, radius = 20.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(gradient)
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                                        radius = 700f
                                    )
                                )
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pesalytics Premium", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            FeatureBullet("Full Analytics suite (donut, calendar, rhythm)", color = Color.White, iconTint = Color.White)
                            FeatureBullet("Per-category Budget Planner + trends", color = Color.White, iconTint = Color.White)
                            FeatureBullet("Unlimited recurring bills + smart alerts", color = Color.White, iconTint = Color.White)
                            FeatureBullet("Unlimited financial goals", color = Color.White, iconTint = Color.White)
                            FeatureBullet("Data export (CSV & PDF)", color = Color.White, iconTint = Color.White)
                            FeatureBullet("100% offline — your data never leaves the phone", color = Color.White, iconTint = Color.White)
                            if (!isPremium) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickableScale {
                                            val activity = context as? android.app.Activity ?: return@clickableScale
                                            val sku = when (selectedPlan) {
                                                "Monthly" -> com.pesalytics.data.billing.BillingConfig.SKU_MONTHLY
                                                "Quarterly" -> com.pesalytics.data.billing.BillingConfig.SKU_QUARTERLY
                                                "Yearly" -> com.pesalytics.data.billing.BillingConfig.SKU_YEARLY
                                                "Lifetime" -> com.pesalytics.data.billing.BillingConfig.SKU_LIFETIME
                                                else -> com.pesalytics.data.billing.BillingConfig.SKU_YEARLY
                                            }
                                            viewModel.subscriptionManager?.launchBillingFlow(activity, sku)
                                        }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                                        Text("UPGRADE TO PREMIUM", fontWeight = FontWeight.Bold, color = AccentGreenDark)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Comparison table
            item {
                Text("Feature Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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
                        ComparisonRow("Analytics", "Home only", "Full suite")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ComparisonRow("Budget Planner", "Global cap", "Per-category")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ComparisonRow("Bill Tracker", "—", "Unlimited")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ComparisonRow("Financial Goals", "1", "Unlimited")
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        ComparisonRow("Data Export", "—", "CSV & PDF")
                    }
                }
            }

            // My subscriptions (monthly bills)
            item {
                Text("My Subscriptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Text("Monthly recurring bills tracked in the Bills screen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                if (monthlyBills.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No monthly subscriptions tracked yet.\nAdd them in the Bills tab.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            monthlyBills.forEachIndexed { index, bill ->
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { viewModel.restorePurchases() }) {
                        Text("Restore Purchases", color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        text = "Subscriptions renew automatically unless canceled. Manage or cancel anytime in Google Play.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
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
                .then(if (isSelected) Modifier.brandGlow(elevation = 12.dp, radius = 12.dp) else Modifier)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) AccentGreenLight else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickableScale { onClick() },
            colors = CardDefaults.cardColors(containerColor = if (isSelected) AccentGreenLight.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                    .background(AccentGreenDark)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(badge, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FeatureBullet(text: String, color: Color = MaterialTheme.colorScheme.onSurface, iconTint: Color = AccentGreenLight) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.Check, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
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
