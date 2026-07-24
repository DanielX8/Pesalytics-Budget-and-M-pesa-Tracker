package com.pesalytics.patterns

import com.pesalytics.model.Transaction
import java.util.Calendar

data class Insight(
    val title: String,
    val description: String,
    val type: InsightType,
    val category: String? = null
)

enum class InsightType {
    WARNING, SUCCESS, INFO
}

object InsightEngine {
    fun generateInsights(transactions: List<Transaction>, periodDays: Int = 30): List<Insight> {
        val insights = mutableListOf<Insight>()
        if (transactions.isEmpty() || periodDays <= 0) return insights
        
        val now = Calendar.getInstance().timeInMillis
        val periodMs = periodDays * 24L * 60 * 60 * 1000L
        
        val currentPeriodStart = now - periodMs
        val previousPeriodStart = currentPeriodStart - periodMs
        
        val currentTransactions = transactions.filter { it.timestamp in currentPeriodStart..now }
        val previousTransactions = transactions.filter { it.timestamp in previousPeriodStart until currentPeriodStart }

        val timeframeStr = if (periodDays == 30) "the last 30 days" else "the last $periodDays days"

        // Helper to get pure expenses
        val currentExpenses = currentTransactions.filter { 
            !it.isFeeTransaction && 
            it.type != com.pesalytics.model.TransactionType.RECEIVE_MONEY && 
            it.type != com.pesalytics.model.TransactionType.MANUAL_INCOME 
        }
        val previousExpenses = previousTransactions.filter { 
            !it.isFeeTransaction && 
            it.type != com.pesalytics.model.TransactionType.RECEIVE_MONEY && 
            it.type != com.pesalytics.model.TransactionType.MANUAL_INCOME 
        }
        
        val totalCurrentExpense = currentExpenses.sumOf { it.amount }
        val totalPreviousExpense = previousExpenses.sumOf { it.amount }

        // Rule 1: Consolidate Spending Spikes
        val currentByCategory = currentExpenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val previousByCategory = previousExpenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        
        var maxSpikeAmount = 0.0
        var topSpikeCategory: String? = null
        var topSpikeMultiplier = 1.0

        currentByCategory.forEach { (category, currentAmount) ->
            if (category.isBlank() || category.contains("Fuliza", ignoreCase = true) || category == "Other" || category == "Transfer") return@forEach
            val previousAmount = previousByCategory[category] ?: 0.0
            
            // If spent more than 30% more than last period, and amount is significant (at least 1000 more)
            if (currentAmount > previousAmount * 1.3 && (currentAmount - previousAmount) > 1000.0) {
                val spikeAmount = currentAmount - previousAmount
                if (spikeAmount > maxSpikeAmount) {
                    maxSpikeAmount = spikeAmount
                    topSpikeCategory = category
                    topSpikeMultiplier = currentAmount / (if(previousAmount == 0.0) 1.0 else previousAmount)
                }
            }
        }

        if (topSpikeCategory != null) {
            val descriptor = if (topSpikeMultiplier > 10.0 || previousByCategory[topSpikeCategory] == 0.0) {
                "you usually spend"
            } else {
                "${String.format(java.util.Locale.US, "%.1f", topSpikeMultiplier)}x more than last period"
            }
            insights.add(
                Insight(
                    title = "Spending Spike",
                    description = "$topSpikeCategory is up! You spent $descriptor.",
                    type = InsightType.WARNING,
                    category = topSpikeCategory
                )
            )
        }
        
        // Rule 2: High Fees (Aligned with AnalyticsScreen)
        val mainTransactions = currentTransactions.filter { !it.isFeeTransaction }
        val fulizaFeeTotal = mainTransactions.filter { it.fee > 0 && it.fulizaOutstandingBalance > 0 }.sumOf { it.fee }
        val feeBreakdown = mainTransactions.filter { it.fee > 0 && it.fulizaOutstandingBalance == 0.0 }.groupBy { it.type }.mapValues { entry -> entry.value.sumOf { it.fee } }
        val totalFees = feeBreakdown.values.sum() + fulizaFeeTotal

        if (totalFees > 500) {
            insights.add(
                Insight(
                    title = "High Fees",
                    description = "You've paid KES ${String.format(java.util.Locale.US, "%,.0f", totalFees)} in fees over $timeframeStr.",
                    type = InsightType.INFO
                )
            )
        }

        // Rule 3: Top Merchant
        val excludePayees = listOf("m-pesa", "fuliza", "safaricom", "mshwari", "kcb")
        val topMerchant = currentExpenses
            .filter { tx -> excludePayees.none { tx.payee.contains(it, ignoreCase = true) } }
            .groupBy { it.payee }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .maxByOrNull { it.value }

        if (topMerchant != null && topMerchant.value > 2000.0 && topMerchant.value > (totalCurrentExpense * 0.15)) {
            val shortPayee = if (topMerchant.key.length > 18) topMerchant.key.take(15) + "..." else topMerchant.key
            insights.add(
                Insight(
                    title = "Top Merchant",
                    description = "KES ${String.format(java.util.Locale.US, "%,.0f", topMerchant.value)} went to $shortPayee.",
                    type = InsightType.INFO,
                    category = "Shopping"
                )
            )
        }

        // Rule 4: Major Single Expense
        val majorExpense = currentExpenses.maxByOrNull { it.amount }
        if (majorExpense != null && totalCurrentExpense > 5000.0 && majorExpense.amount > (totalCurrentExpense * 0.35)) {
            val shortPayee = if (majorExpense.payee.length > 18) majorExpense.payee.take(15) + "..." else majorExpense.payee
            insights.add(
                Insight(
                    title = "Major Expense",
                    description = "A single payment of KES ${String.format(java.util.Locale.US, "%,.0f", majorExpense.amount)} to $shortPayee took up a large chunk of your budget.",
                    type = InsightType.WARNING,
                    category = majorExpense.category
                )
            )
        }

        // Rule 5: Velocity (Pace)
        if (totalPreviousExpense > 0) {
            val pace = totalCurrentExpense / totalPreviousExpense
            if (pace > 1.2 && (totalCurrentExpense - totalPreviousExpense) > 2000.0) {
                 insights.add(
                    Insight(
                        title = "Spending Fast",
                        description = "You're spending ${String.format(java.util.Locale.US, "%.0f", (pace - 1) * 100)}% faster than last period.",
                        type = InsightType.WARNING
                    )
                )
            } else if (pace < 0.8 && totalPreviousExpense > 5000.0) {
                 insights.add(
                    Insight(
                        title = "Great Saving!",
                        description = "You're spending ${String.format(java.util.Locale.US, "%.0f", (1 - pace) * 100)}% less than last period.",
                        type = InsightType.SUCCESS
                    )
                )
            }
        }

        // Return up to 4 insights, prioritizing Warnings, then Success, then Info
        return insights.sortedBy { 
            when(it.type) {
                InsightType.WARNING -> 0
                InsightType.SUCCESS -> 1
                InsightType.INFO -> 2
            }
        }.take(4)
    }
}
