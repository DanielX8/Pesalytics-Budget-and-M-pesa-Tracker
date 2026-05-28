package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentGreenDark
import com.example.ui.theme.AccentGreenLight

data class FaqItem(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqScreen(onNavigateBack: () -> Unit) {
    val faqs = listOf(
        FaqItem(
            question = "How does PesaSense track my M-Pesa transactions?",
            answer = "PesaSense securely reads your M-Pesa SMS messages stored locally on your device. It extracts the transaction details like amount, date, and merchant to give you insights without ever connecting to the internet or your bank account."
        ),
        FaqItem(
            question = "Is my financial data safe?",
            answer = "Absolutely. All your data is stored completely offline on your device. We do not use external servers to process or store your transactions. Your privacy is our ultimate premium feature."
        ),
        FaqItem(
            question = "How do I export my data?",
            answer = "You can export your transaction history at any time by going to Settings > Data Export. You have the option to generate a beautifully formatted PDF report or a CSV file for spreadsheet analysis."
        ),
        FaqItem(
            question = "What happens if I change my phone?",
            answer = "Currently, since all data is stored offline for privacy, you cannot automatically sync data between phones. However, you can export your history as a CSV file to keep your records safe."
        ),
        FaqItem(
            question = "Can I track manual cash transactions?",
            answer = "Yes! You can manually add transactions by pressing the '+' button on the Dashboard. This allows you to track cash income or expenses right alongside your M-Pesa records."
        ),
        FaqItem(
            question = "How are budget thresholds calculated?",
            answer = "Your budget threshold notifications (80% and 100%) are calculated automatically based on your set monthly Global Budget limits against your current month's recorded expenses."
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frequently Asked Questions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Everything you need to know about PesaSense and how it helps you manage your money.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(faqs) { faq ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = faq.question,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = AccentGreenDark
                            )
                        }
                        
                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
