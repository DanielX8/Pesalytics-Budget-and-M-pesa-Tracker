package com.pesalytics.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.pesalytics.model.Transaction
import com.pesalytics.model.TransactionType
import com.pesalytics.ui.theme.AccentGreenDark
import com.pesalytics.ui.theme.ExpenseRed
import com.pesalytics.utils.CsvExportHelper
import com.pesalytics.utils.JsonExportHelper
import com.pesalytics.utils.PdfExportHelper
import com.pesalytics.data.PesaRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class ExportPeriod {
    THIS_MONTH, LAST_MONTH, LAST_3_MONTHS, LAST_6_MONTHS, YTD, ALL_TIME, CUSTOM
}

enum class ExportFormat {
    PDF, CSV, JSON
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportStationSheet(
    transactions: List<Transaction>,
    repository: PesaRepository,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onShowPremiumGate: () -> Unit,
    onNotification: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedPeriod by remember { mutableStateOf(ExportPeriod.THIS_MONTH) }
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    
    // Custom Date Range
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState()
    
    // Calculate Date Bounds
    val calendar = Calendar.getInstance()
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(0L) }
    var periodLabel by remember { mutableStateOf("") }
    
    val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    LaunchedEffect(selectedPeriod, datePickerState.selectedStartDateMillis, datePickerState.selectedEndDateMillis) {
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        
        when (selectedPeriod) {
            ExportPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                startMs = calendar.timeInMillis
                endMs = now
                periodLabel = "This Month"
            }
            ExportPeriod.LAST_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.add(Calendar.MILLISECOND, -1)
                endMs = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                startMs = calendar.timeInMillis
                periodLabel = "Last Month"
            }
            ExportPeriod.LAST_3_MONTHS -> {
                calendar.add(Calendar.MONTH, -3)
                startMs = calendar.timeInMillis
                endMs = now
                periodLabel = "Last 3 Months"
            }
            ExportPeriod.LAST_6_MONTHS -> {
                calendar.add(Calendar.MONTH, -6)
                startMs = calendar.timeInMillis
                endMs = now
                periodLabel = "Last 6 Months"
            }
            ExportPeriod.YTD -> {
                calendar.set(Calendar.MONTH, Calendar.JANUARY)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                startMs = calendar.timeInMillis
                endMs = now
                periodLabel = "Year to Date"
            }
            ExportPeriod.ALL_TIME -> {
                startMs = 0L
                endMs = now
                periodLabel = "All Time"
            }
            ExportPeriod.CUSTOM -> {
                if (datePickerState.selectedStartDateMillis != null && datePickerState.selectedEndDateMillis != null) {
                    startMs = datePickerState.selectedStartDateMillis!!
                    endMs = datePickerState.selectedEndDateMillis!! + 86400000L - 1 // End of day
                    periodLabel = "${df.format(Date(startMs))} - ${df.format(Date(endMs))}"
                } else {
                    startMs = 0L
                    endMs = now
                    periodLabel = "Custom Range"
                }
            }
        }
    }

    // Force ALL_TIME for JSON Backup
    if (selectedFormat == ExportFormat.JSON && selectedPeriod != ExportPeriod.ALL_TIME) {
        selectedPeriod = ExportPeriod.ALL_TIME
    }

    // Filter transactions
    val filteredTxns = transactions.filter { it.timestamp in startMs..endMs }
    
    // Summaries
    val incomeTypes = setOf(TransactionType.RECEIVE_MONEY, TransactionType.MANUAL_INCOME)
    val totalIncome = filteredTxns.filter { it.type in incomeTypes && !it.isFeeTransaction }.sumOf { it.amount }
    val totalExpense = filteredTxns.filter { it.type !in incomeTypes && it.type != TransactionType.MANUAL_TRANSFER && !it.isFeeTransaction }.sumOf { it.amount }
    val net = totalIncome - totalExpense

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Export Financial Statement",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Period Selector
            Text("PERIOD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = ExportPeriod.values()
                periods.forEach { period ->
                    val isSelected = selectedPeriod == period
                    val label = when(period) {
                        ExportPeriod.THIS_MONTH -> "This Month"
                        ExportPeriod.LAST_MONTH -> "Last Month"
                        ExportPeriod.LAST_3_MONTHS -> "Last 3 Months"
                        ExportPeriod.LAST_6_MONTHS -> "Last 6 Months"
                        ExportPeriod.YTD -> "YTD"
                        ExportPeriod.ALL_TIME -> "All Time"
                        ExportPeriod.CUSTOM -> "Custom"
                    }
                    val isJsonMode = selectedFormat == ExportFormat.JSON
                    val isDisabled = isJsonMode && period != ExportPeriod.ALL_TIME

                    FilterChip(
                        selected = isSelected,
                        onClick = { 
                            if (!isDisabled) {
                                selectedPeriod = period
                                if (period == ExportPeriod.CUSTOM) showDatePicker = true 
                            }
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreenDark,
                            selectedLabelColor = Color.White
                        ),
                        enabled = !isDisabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Live Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(periodLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("${filteredTxns.size} transactions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("INCOME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("KSh ${"%,.0f".format(totalIncome)}", style = MaterialTheme.typography.bodyMedium, color = AccentGreenDark, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("KSh ${"%,.0f".format(totalExpense)}", style = MaterialTheme.typography.bodyMedium, color = ExpenseRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("NET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "KSh ${"%,.0f".format(abs(net))}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (net >= 0) AccentGreenDark else ExpenseRed,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Format Selection
            Text("FORMAT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            
            FormatCard(
                title = "PDF Statement",
                description = "Formatted statement with charts",
                icon = Icons.Rounded.PictureAsPdf,
                isSelected = selectedFormat == ExportFormat.PDF,
                isPremium = true,
                onClick = { selectedFormat = ExportFormat.PDF }
            )
            FormatCard(
                title = "CSV Spreadsheet",
                description = "Raw data for Excel / Google Sheets",
                icon = Icons.Rounded.TableChart,
                isSelected = selectedFormat == ExportFormat.CSV,
                isPremium = true,
                onClick = { selectedFormat = ExportFormat.CSV }
            )
            FormatCard(
                title = "JSON Backup",
                description = "Full app backup (Bills, Budgets, Rules)",
                icon = Icons.Rounded.Save,
                isSelected = selectedFormat == ExportFormat.JSON,
                isPremium = false,
                onClick = { selectedFormat = ExportFormat.JSON }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Button
            Button(
                onClick = {
                    if ((selectedFormat == ExportFormat.PDF || selectedFormat == ExportFormat.CSV) && !isPremium) {
                        onShowPremiumGate()
                        return@Button
                    }
                    
                    coroutineScope.launch {
                        onNotification("Generating...")
                        val file = when(selectedFormat) {
                            ExportFormat.PDF -> {
                                var pdfFile: java.io.File? = null
                                val lock = java.util.concurrent.CountDownLatch(1)
                                PdfExportHelper.generatePdf(context, filteredTxns, periodLabel) { 
                                    pdfFile = it
                                    lock.countDown()
                                }
                                kotlinx.coroutines.Dispatchers.IO.invoke { lock.await() }
                                pdfFile
                            }
                            ExportFormat.CSV -> CsvExportHelper.exportToCsv(context, filteredTxns)
                            ExportFormat.JSON -> JsonExportHelper.generateBackup(context, repository)
                        }

                        if (file != null) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.applicationContext.packageName}.fileprovider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = when(selectedFormat) {
                                    ExportFormat.PDF -> "application/pdf"
                                    ExportFormat.CSV -> "text/csv"
                                    ExportFormat.JSON -> "application/json"
                                }
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Export"))
                            onDismiss()
                        } else {
                            onNotification("Failed to generate export.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreenDark)
            ) {
                Text("Generate & Share", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun FormatCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AccentGreenDark else Color.Transparent
    val bgColor = if (isSelected) AccentGreenDark.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) AccentGreenDark else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isPremium) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFB8860B), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = AccentGreenDark)
        }
    }
}
