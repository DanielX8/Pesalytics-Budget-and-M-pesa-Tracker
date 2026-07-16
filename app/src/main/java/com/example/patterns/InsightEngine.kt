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
    fun generateInsights(transactions: List<Transaction>): List<Insight> {
        val insights = mutableListOf<Insight>()
        
        // Group by month
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        val thisMonthTransactions = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }
        
        val previousMonthTransactions = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            var prevMonth = currentMonth - 1
            var prevYear = currentYear
            if (prevMonth < 0) {
                prevMonth = 11
                prevYear -= 1
            }
            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear
        }

        // Rule 1: High category spend compared to last month
        val thisMonthByCategory = thisMonthTransactions.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val prevMonthByCategory = previousMonthTransactions.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        
        thisMonthByCategory.forEach { (category, currentAmount) ->
            if (category.isBlank() || category == "Fuliza" || category == "Other") return@forEach
            val previousAmount = prevMonthByCategory[category] ?: 0.0
            
            // If spent more than 50% more than last month, and amount is significant (> 1000)
            if (previousAmount > 1000.0 && currentAmount > previousAmount * 1.5) {
                val multiplier = currentAmount / previousAmount
                insights.add(
                    Insight(
                        title = "Spending Spike",
                        description = "$category is ${String.format(java.util.Locale.US, "%.1f", multiplier)}x more than your usual spend.",
                        type = InsightType.WARNING,
                        category = category
                    )
                )
            }
        }
        
        // Rule 2: High Fees
        val totalFees = thisMonthTransactions.sumOf { it.fee }
        if (totalFees > 500) {
            insights.add(
                Insight(
                    title = "High Fees",
                    description = "You've paid KES ${String.format(java.util.Locale.US, "%,.0f", totalFees)} in transaction fees this month.",
                    type = InsightType.INFO
                )
            )
        }

        return insights.sortedByDescending { it.type == InsightType.WARNING }
    }
}
