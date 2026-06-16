package com.pesasense.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pesasense.R
import com.pesasense.ui.theme.AccentGreenDark
import com.pesasense.ui.theme.AccentGreenLight
import com.pesasense.ui.theme.ExpenseRed

/**
 * Returns whether a category counts as a "Need". Uses the user's saved classification,
 * falling back to keyword heuristics for categories the user hasn't classified yet.
 */
fun isNeedCategory(category: String, classification: Map<String, Boolean>): Boolean {
    classification[category]?.let { return it }
    return DEFAULT_NEED_KEYWORDS.any { category.contains(it, ignoreCase = true) }
}

val DEFAULT_NEED_KEYWORDS = listOf(
    "Rent", "Utilities", "Groceries", "Transport", "Bills", "Health", "KPLC", "Water"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsWantsScreen(
    viewModel: PesaViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.knownCategories.collectAsStateWithLifecycle()
    val classification by viewModel.needsWantsClassification.collectAsStateWithLifecycle()

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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Needs vs Wants",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Decide which categories count as a Need and which are a Want. This drives the Needs vs Wants breakdown in Analytics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(categories) { category ->
                val isNeed = isNeedCategory(category, classification)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            category,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        NeedWantToggle(
                            isNeed = isNeed,
                            onSelect = { need -> viewModel.setCategoryClassification(category, need, context) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun NeedWantToggle(isNeed: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        SegmentChip("Need", selected = isNeed, selectedColor = AccentGreenLight) { onSelect(true) }
        SegmentChip("Want", selected = !isNeed, selectedColor = ExpenseRed) { onSelect(false) }
    }
}

@Composable
private fun SegmentChip(label: String, selected: Boolean, selectedColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) selectedColor else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
