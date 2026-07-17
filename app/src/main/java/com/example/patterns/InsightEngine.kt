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

        // Rule 1: High category spend compared to last period
        val currentByCategory = currentTransactions.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val previousByCategory = previousTransactions.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        
        currentByCategory.forEach { (category, currentAmount) ->
            if (category.isBlank() || category == "Fuliza" || category == "Other") return@forEach
            val previousAmount = previousByCategory[category] ?: 0.0
            
            // If spent more than 30% more than last period, and amount is significant (at least 1000 more)
            if (currentAmount > previousAmount * 1.3 && (currentAmount - previousAmount) > 1000.0) {
                val multiplier = currentAmount / (if(previousAmount == 0.0) 1.0 else previousAmount)
                val descriptor = if (previousAmount == 0.0) "you usually spend" else "${String.format(java.util.Locale.US, "%.1f", multiplier)}x more than last period"
                insights.add(
                    Insight(
                        title = "Spending Spike",
                        description = "$category is $descriptor.",
                        type = InsightType.WARNING,
                        category = category
                    )
                )
            }
        }
        
        // Rule 2: High Fees
        val totalFees = currentTransactions.sumOf { it.fee }
        if (totalFees > 500) {
            insights.add(
                Insight(
                    title = "High Fees",
                    description = "You've paid KES ${String.format(java.util.Locale.US, "%,.0f", totalFees)} in fees.",
                    type = InsightType.INFO
                )
            )
        }

        // Rule 3: Velocity (Pace)
        val currentExpense = currentTransactions.filter { !it.isFeeTransaction && it.type != com.pesalytics.model.TransactionType.RECEIVE_MONEY && it.type != com.pesalytics.model.TransactionType.MANUAL_INCOME }.sumOf { it.amount }
        val previousExpense = previousTransactions.filter { !it.isFeeTransaction && it.type != com.pesalytics.model.TransactionType.RECEIVE_MONEY && it.type != com.pesalytics.model.TransactionType.MANUAL_INCOME }.sumOf { it.amount }
        
        if (previousExpense > 0) {
            val pace = currentExpense / previousExpense
            if (pace > 1.2 && (currentExpense - previousExpense) > 2000.0) {
                 insights.add(
                    Insight(
                        title = "Spending Fast",
                        description = "You're spending ${String.format(java.util.Locale.US, "%.0f", (pace - 1) * 100)}% faster than last period.",
                        type = InsightType.WARNING
                    )
                )
            } else if (pace < 0.8 && previousExpense > 5000.0) {
                 insights.add(
                    Insight(
                        title = "Great Saving!",
                        description = "You're spending ${String.format(java.util.Locale.US, "%.0f", (1 - pace) * 100)}% less than last period.",
                        type = InsightType.SUCCESS
                    )
                )
            }
        }

        return insights.sortedByDescending { it.type == InsightType.WARNING }
    }
}
