package com.pesalytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pesalytics.ui.theme.AccentGreenDark
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DayCarousel(
    selectedMonth: Int, // 0-indexed (Calendar.JANUARY = 0)
    selectedYear: Int,
    selectedDay: Int?, // The currently selected day, if any
    onDaySelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val realCurrentYear = calendar.get(Calendar.YEAR)
    val realCurrentMonth = calendar.get(Calendar.MONTH)
    val realCurrentDay = calendar.get(Calendar.DAY_OF_MONTH)

    // Calculate the number of days in the selected month
    val daysInMonth = remember(selectedMonth, selectedYear) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Determine what day to scroll to initially
    val initialScrollDay = remember(selectedMonth, selectedYear) {
        if (selectedYear == realCurrentYear && selectedMonth == realCurrentMonth) {
            maxOf(0, realCurrentDay - 3) // Center current day roughly
        } else if (selectedYear > realCurrentYear || (selectedYear == realCurrentYear && selectedMonth > realCurrentMonth)) {
            0 // Future month, start at beginning
        } else {
            maxOf(0, daysInMonth - 5) // Past month, start near the end
        }
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollDay)

    // Auto-scroll when the month changes
    LaunchedEffect(selectedMonth, selectedYear) {
        listState.animateScrollToItem(initialScrollDay)
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(daysInMonth) { index ->
            val dayNumber = index + 1
            
            // Format the day name (Mon, Tue, etc.)
            val dayName = remember(selectedMonth, selectedYear, dayNumber) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, dayNumber)
                }
                SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            }

            val isSelected = dayNumber == selectedDay
            val isToday = selectedYear == realCurrentYear && selectedMonth == realCurrentMonth && dayNumber == realCurrentDay
            
            val isFuture = (selectedYear > realCurrentYear) || 
                           (selectedYear == realCurrentYear && selectedMonth > realCurrentMonth) || 
                           (selectedYear == realCurrentYear && selectedMonth == realCurrentMonth && dayNumber > realCurrentDay)

            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> AccentGreenDark
                            isToday -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surface
                        }
                    )
                    .alpha(if (isFuture) 0.3f else 1f)
                    .clickable(enabled = !isFuture) {
                        if (isSelected) {
                            onDaySelected(null) // Deselect if already selected
                        } else {
                            onDaySelected(dayNumber)
                        }
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayNumber.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isToday && !isSelected) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(AccentGreenDark)
                        )
                    }
                }
            }
        }
    }
}
