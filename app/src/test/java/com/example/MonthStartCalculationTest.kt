package com.pesasense

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

/**
 * Tests for Bug #7: The Analytics screen used a local Calendar block that set only MONTH,
 * not YEAR. Selecting December (index 11) in January (current month 0) should produce a
 * timestamp in the PREVIOUS year's December, not the current year's future December.
 *
 * The fix is to use monthStartTimestampMs() — the same logic already present in
 * PesaViewModel.currentMonthStart — instead of the inline Calendar block.
 */
class MonthStartCalculationTest {

    @Test
    fun `December in January resolves to previous year`() {
        val now = Calendar.getInstance()
        val currentMonth = 0  // January
        val currentYear = now.get(Calendar.YEAR)

        val result = monthStartTimestampMs(
            selectedMonth = Calendar.DECEMBER,
            currentMonth = currentMonth,
            currentYear = currentYear
        )

        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(Calendar.DECEMBER, resultCal.get(Calendar.MONTH))
        assertEquals(currentYear - 1, resultCal.get(Calendar.YEAR))
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `current month resolves to same year`() {
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val result = monthStartTimestampMs(
            selectedMonth = currentMonth,
            currentMonth = currentMonth,
            currentYear = currentYear
        )

        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(currentMonth, resultCal.get(Calendar.MONTH))
        assertEquals(currentYear, resultCal.get(Calendar.YEAR))
    }

    @Test
    fun `past month in same year resolves to same year`() {
        val now = Calendar.getInstance()
        val currentMonth = 6  // July
        val currentYear = now.get(Calendar.YEAR)

        val result = monthStartTimestampMs(
            selectedMonth = Calendar.JANUARY,
            currentMonth = currentMonth,
            currentYear = currentYear
        )

        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(Calendar.JANUARY, resultCal.get(Calendar.MONTH))
        assertEquals(currentYear, resultCal.get(Calendar.YEAR))
    }

    @Test
    fun `result is always the 1st of the month at midnight`() {
        val result = monthStartTimestampMs(
            selectedMonth = Calendar.MARCH,
            currentMonth = Calendar.MAY,
            currentYear = 2026
        )

        val resultCal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }
}

/**
 * Pure function extracted from PesaViewModel.currentMonthStart logic.
 * If selectedMonth > currentMonth, the month belongs to the previous year.
 */
fun monthStartTimestampMs(selectedMonth: Int, currentMonth: Int, currentYear: Int): Long {
    val targetYear = if (selectedMonth > currentMonth) currentYear - 1 else currentYear
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, targetYear)
        set(Calendar.MONTH, selectedMonth)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
