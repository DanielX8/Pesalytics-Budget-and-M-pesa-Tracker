package com.pesalytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.pesalytics.ui.theme.AccentGreenDark
import com.pesalytics.ui.theme.ExpenseRed
import com.pesalytics.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnrichedManualEntrySheet(
    sheetState: SheetState,
    knownCategories: List<String>,
    historicalPayees: List<String>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, payee: String, isIncome: Boolean, category: String, source: String, timestamp: Long, notes: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    var isIncome by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("CASH") }
    var selectedCategory by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var timestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var payeeDropdownExpanded by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = timestamp)

    // Sources
    val sources = listOf(
        "CASH" to "Cash",
        "BANK" to "Bank",
        "CARD" to "Card/BNPL",
        "CHEQUE" to "Cheque",
        "OTHER" to "Other"
    )

    // Derived filtered payees for autocomplete
    val filteredPayees = remember(payee, historicalPayees) {
        if (payee.isBlank()) emptyList()
        else historicalPayees.filter { it.contains(payee, ignoreCase = true) && it != payee }.take(5)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Add Manual Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // 1. Direction (Expense / Income)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    listOf(false to "Expense", true to "Income").forEach { (incomeFlag, label) ->
                        val isSelected = isIncome == incomeFlag
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) (if (incomeFlag) IncomeGreen else ExpenseRed) else Color.Transparent)
                                .clickable { 
                                    isIncome = incomeFlag
                                    if (incomeFlag && selectedCategory.isEmpty()) selectedCategory = "Income"
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 2. Source
            item {
                Column {
                    Text("Payment Source", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sources.forEach { (sourceId, sourceLabel) ->
                            FilterChip(
                                selected = selectedSource == sourceId,
                                onClick = { selectedSource = sourceId },
                                label = { Text(sourceLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreenDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 3. Amount & Payee
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Amount") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Box(modifier = Modifier.weight(1.5f)) {
                        OutlinedTextField(
                            value = payee,
                            onValueChange = { 
                                payee = it 
                                payeeDropdownExpanded = true
                            },
                            label = { Text("Payee") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = payeeDropdownExpanded && filteredPayees.isNotEmpty(),
                            onDismissRequest = { payeeDropdownExpanded = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            filteredPayees.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion) },
                                    onClick = {
                                        payee = suggestion
                                        payeeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Category
            item {
                Column {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        knownCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreenDark,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 5. Date & Time
            item {
                val dateStr = remember(timestamp) {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
                }
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(dateStr, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // 6. Notes
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Memo (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }

            // 7. Save Button
            item {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0 && payee.isNotBlank()) {
                            onSave(
                                amount, 
                                payee, 
                                isIncome, 
                                selectedCategory.ifEmpty { if (isIncome) "Income" else "Other" },
                                selectedSource,
                                timestamp,
                                notes
                            )
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    enabled = amountText.toDoubleOrNull() != null && payee.isNotBlank()
                ) {
                    Text("Save Transaction")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val currentCal = Calendar.getInstance().apply { timeInMillis = timestamp }
                        val newCal = Calendar.getInstance().apply { timeInMillis = it }
                        currentCal.set(Calendar.YEAR, newCal.get(Calendar.YEAR))
                        currentCal.set(Calendar.MONTH, newCal.get(Calendar.MONTH))
                        currentCal.set(Calendar.DAY_OF_MONTH, newCal.get(Calendar.DAY_OF_MONTH))
                        timestamp = currentCal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK", color = AccentGreenDark) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = AccentGreenDark) }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.background
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = AccentGreenDark,
                    headlineContentColor = AccentGreenDark,
                    todayContentColor = AccentGreenDark,
                    todayDateBorderColor = AccentGreenDark,
                    selectedDayContainerColor = AccentGreenDark,
                    selectedDayContentColor = Color.White
                )
            )
        }
    }
}
