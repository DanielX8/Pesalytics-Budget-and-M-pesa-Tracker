package com.pesasense.patterns

import com.pesasense.model.Transaction
import com.pesasense.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


data class CategoryDelta(
    val category: String,
    val currentAmount: Double,
    val previousAmount: Double
) {
    val delta: Double get() = currentAmount - previousAmount
    val percentChange: Double get() = when {
        previousAmount > 0 -> (delta / previousAmount) * 100
        currentAmount > 0 -> 100.0
        else -> 0.0
    }
}

data class SpendVelocity(
    val dailyAverage: Double,
    val projectedMonthEnd: Double,
    val daysElapsed: Int,
    val daysInMonth: Int
)

data class FulizaMonthPoint(
    val month: String,
    val totalFulizaUsed: Double,
    val outstandingBalance: Double
)

data class MonthComparison(
    val currentMonthExpense: Double,
    val previousMonthExpense: Double
) {
    val delta: Double get() = currentMonthExpense - previousMonthExpense
    val percentChange: Double get() = if (previousMonthExpense > 0) (delta / previousMonthExpense) * 100 else 0.0
}

// v1.2 stubs
data class AnomalyFlag(val transactionId: Int, val reason: String)
data class PredictedExpense(val category: String, val amount: Double)

data class PatternResult(
    val categoryDeltas: List<CategoryDelta> = emptyList(),
    val spendVelocity: SpendVelocity? = null,
    val fulizaTrend: List<FulizaMonthPoint> = emptyList(),
    val monthComparison: MonthComparison? = null,
    val anomalyFlags: List<AnomalyFlag> = emptyList(),
    val predictedExpenses: List<PredictedExpense> = emptyList()
)

class PatternEngine {

    fun compute(transactions: List<Transaction>): PatternResult {
        return PatternResult(
            categoryDeltas = computeCategoryDeltas(transactions),
            spendVelocity = computeSpendVelocity(transactions),
            fulizaTrend = computeFulizaTrend(transactions),
            monthComparison = computeMonthComparison(transactions)
        )
    }

    private fun isExpense(t: Transaction): Boolean =
        t.type != TransactionType.RECEIVE_MONEY &&
        t.type != TransactionType.MANUAL_INCOME &&
        t.type != TransactionType.MANUAL_TRANSFER &&
        !t.isFeeTransaction

    private fun monthStart(monthsAgo: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.MONTH, -monthsAgo)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun computeCategoryDeltas(transactions: List<Transaction>): List<CategoryDelta> {
        val currentStart = monthStart(0)
        val prevStart = monthStart(1)

        val current = transactions
            .filter { isExpense(it) && it.timestamp >= currentStart }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }

        val previous = transactions
            .filter { isExpense(it) && it.timestamp in prevStart until currentStart }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }

        return (current.keys + previous.keys).toSet().map { cat ->
            CategoryDelta(
                category = cat,
                currentAmount = current[cat] ?: 0.0,
                previousAmount = previous[cat] ?: 0.0
            )
        }
            .filter { it.currentAmount > 0 || it.previousAmount > 0 }
            .sortedByDescending { it.currentAmount }
    }

    private fun computeSpendVelocity(transactions: List<Transaction>): SpendVelocity? {
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysElapsed = cal.get(Calendar.DAY_OF_MONTH)
        if (daysElapsed == 0) return null

        val currentStart = monthStart(0)
        val currentExpense = transactions
            .filter { isExpense(it) && it.timestamp >= currentStart }
            .sumOf { it.amount }

        val dailyAverage = currentExpense / daysElapsed
        return SpendVelocity(
            dailyAverage = dailyAverage,
            projectedMonthEnd = dailyAverage * daysInMonth,
            daysElapsed = daysElapsed,
            daysInMonth = daysInMonth
        )
    }

    private fun computeFulizaTrend(transactions: List<Transaction>): List<FulizaMonthPoint> {
        val monthFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return transactions
            .filter { it.usedFulizaAmount > 0 || it.category.equals("Fuliza", ignoreCase = true) }
            .groupBy { monthFmt.format(Date(it.timestamp)) }
            .map { (month, txns) ->
                FulizaMonthPoint(
                    month = month,
                    totalFulizaUsed = txns.sumOf { it.usedFulizaAmount },
                    outstandingBalance = txns.maxOfOrNull { it.fulizaOutstandingBalance } ?: 0.0
                )
            }
            .sortedBy { it.month }
    }

    private fun computeMonthComparison(transactions: List<Transaction>): MonthComparison? {
        val currentStart = monthStart(0)
        val prevStart = monthStart(1)

        val currentExpense = transactions
            .filter { isExpense(it) && it.timestamp >= currentStart }
            .sumOf { it.amount }

        val prevExpense = transactions
            .filter { isExpense(it) && it.timestamp in prevStart until currentStart }
            .sumOf { it.amount }

        if (currentExpense == 0.0 && prevExpense == 0.0) return null

        return MonthComparison(
            currentMonthExpense = currentExpense,
            previousMonthExpense = prevExpense
        )
    }
}
