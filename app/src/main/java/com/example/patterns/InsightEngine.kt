package com.pesalytics.patterns

import com.pesalytics.model.Bill
import com.pesalytics.model.Budget
import com.pesalytics.model.Transaction
import com.pesalytics.model.TransactionType
import java.util.Calendar
import java.util.Locale

data class Insight(
    val title: String,
    val description: String,
    val type: InsightType,
    val category: String? = null,
    val actionRoute: String? = null
)

enum class InsightType {
    WARNING, SUCCESS, INFO, BILL_DUE, BUDGET_ALERT
}

data class ScoredInsight(val insight: Insight, val score: Int)

data class PeriodComparison(
    val label: String,
    val currentAmount: Double,
    val previousAmount: Double,
    val topCategoryName: String?,
    val topCategoryCurrentAmount: Double = 0.0,
    val topCategoryPrevAmount: Double = 0.0
) {
    val ratio: Double get() = if (previousAmount > 0) currentAmount / previousAmount else 0.0
    val deltaAbs: Double get() = currentAmount - previousAmount
}

object InsightEngine {

    fun generateInsights(
        transactions: List<Transaction>,
        bills: List<Bill>,
        budgets: List<Budget>, // Unused for now, but in signature for future
        currentBudgetLimit: Double,
        monthlyExpense: Double,
        fulizaOutstanding: Double,
        fulizaDueDate: String,
        now: Long = System.currentTimeMillis()
    ): List<Insight> {
        val signals = mutableListOf<ScoredInsight>()
        
        signalYesterdaySpend(transactions, monthlyExpense, now)?.let { signals.add(it) }
        signalBudgetBurnRate(currentBudgetLimit, monthlyExpense, now)?.let { signals.add(it) }
        signalBillDueAlert(bills, now)?.let { signals.add(it) }
        signalTodaySpend(transactions, monthlyExpense, now)?.let { signals.add(it) }
        signalIncomeSaveNudge(transactions, now)?.let { signals.add(it) }
        signalUnusualSingleTransaction(transactions, monthlyExpense, now)?.let { signals.add(it) }
        signalSpendingSpike(transactions, now)?.let { signals.add(it) }
        signalWeekendBias(transactions, now)?.let { signals.add(it) }
        signalFrequentMerchantWeekly(transactions, now)?.let { signals.add(it) }
        signalCarrierFeesToll(transactions, now)?.let { signals.add(it) }
        signalSavingsGrowth(transactions, now)?.let { signals.add(it) }
        signalBudgetSafePace(currentBudgetLimit, monthlyExpense, now)?.let { signals.add(it) }
        signalMonthEndGauge(currentBudgetLimit, monthlyExpense, now)?.let { signals.add(it) }
        signalFulizaRisk(fulizaOutstanding, fulizaDueDate, now)?.let { signals.add(it) }
        signalSavingsWin(transactions, now)?.let { signals.add(it) }
        signalMonthEndWarning(transactions, currentBudgetLimit, now)?.let { signals.add(it) }
        signalCategoryUncertainty(transactions, now)?.let { signals.add(it) }
        signalMultiPeriodComparison(transactions, now)?.let { signals.add(it) }

        return signals.sortedByDescending { it.score }.take(5).map { it.insight }
    }

    private fun getCal(timeMs: Long): Calendar = Calendar.getInstance().apply { timeInMillis = timeMs }
    private fun formatAmount(amount: Double): String = String.format(java.util.Locale.US, "%,.0f", amount)

    private fun getStartOfDay(now: Long): Long {
        val cal = getCal(now)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    private fun getStartOfMonth(now: Long): Long {
        val cal = getCal(now)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun isExpense(tx: Transaction): Boolean =
        !tx.isFeeTransaction && 
        tx.type != TransactionType.RECEIVE_MONEY && 
        tx.type != TransactionType.MANUAL_INCOME &&
        tx.type != TransactionType.MANUAL_TRANSFER &&
        tx.type != TransactionType.MSHWARI_TRANSFER &&
        tx.type != TransactionType.POCHI_TRANSFER &&
        tx.type != TransactionType.POCHI_RECEIVE &&
        tx.type != TransactionType.FULIZA

    // 1. Budget Burn Rate
    private fun signalBudgetBurnRate(limit: Double, expense: Double, now: Long): ScoredInsight? {
        if (limit <= 0) return null
        val cal = getCal(now)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        if (dayOfMonth < 5) return null
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - dayOfMonth + 1
        val pct = expense / limit
        
        if (pct >= 0.9) {
            return ScoredInsight(
                Insight(
                    "Budget Critical",
                    "You've used ${(pct*100).toInt()}% of your KES ${formatAmount(limit)} budget — $daysLeft days left this month.",
                    InsightType.BUDGET_ALERT,
                    actionRoute = "budget_planner"
                ), 85
            )
        } else if (pct >= 0.7) {
            return ScoredInsight(
                Insight(
                    "Budget Warning",
                    "You've used ${(pct*100).toInt()}% of your monthly budget. Watch your spending.",
                    InsightType.BUDGET_ALERT,
                    actionRoute = "budget_planner"
                ), 60
            )
        }
        return null
    }

    // 2. Bill Due Alert
    private fun signalBillDueAlert(bills: List<Bill>, now: Long): ScoredInsight? {
        val activeBills = bills.filter { !it.isPaid && !it.isPaused }
        val startOfToday = getStartOfDay(now)
        val msPerDay = 1000L * 60 * 60 * 24
        
        var topBill: Bill? = null
        var minDays = Int.MAX_VALUE
        var multipleCount = 0
        var totalAmount = 0.0

        for (bill in activeBills) {
            val daysDue = ((bill.nextDueDate - startOfToday) / msPerDay).toInt()
            if (daysDue in 0..5) {
                multipleCount++
                totalAmount += bill.amount
                if (daysDue < minDays) {
                    minDays = daysDue
                    topBill = bill
                }
            }
        }
        if (multipleCount == 0 || topBill == null) return null

        val score = if (minDays <= 2) 95 else 75
        
        val desc = if (multipleCount == 1) {
            val whenDue = if (minDays == 0) "today" else if (minDays == 1) "tomorrow" else "in $minDays days"
            "${topBill.name} bill of KES ${formatAmount(topBill.amount)} is due $whenDue."
        } else {
            "$multipleCount bills coming up in the next 5 days — KES ${formatAmount(totalAmount)} total."
        }
        return ScoredInsight(Insight("Bill Due Soon", desc, InsightType.BILL_DUE, actionRoute = "bills"), score)
    }

    // 3. Today's Spend vs Daily Average
    private fun signalTodaySpend(transactions: List<Transaction>, monthlyExpense: Double, now: Long): ScoredInsight? {
        val cal = getCal(now)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val startOfToday = getStartOfDay(now)
        
        val todayTxns = transactions.filter { it.timestamp >= startOfToday && isExpense(it) }
        val todaySpend = todayTxns.sumOf { it.amount }
        if (todaySpend == 0.0) return null
        
        val dailyAvg = if (dayOfMonth > 1) monthlyExpense / dayOfMonth else monthlyExpense
        if (dailyAvg < 500.0) return null // Skip if avg is too small
        
        if (todaySpend > dailyAvg * 2.0) {
            return ScoredInsight(
                Insight(
                    "High Daily Spend",
                    "You've spent KES ${formatAmount(todaySpend)} today — ${String.format(java.util.Locale.US, "%.1f", todaySpend/dailyAvg)}x your KES ${formatAmount(dailyAvg)} daily average.",
                    InsightType.WARNING,
                    actionRoute = "all_transactions?filter=Today"
                ), 70
            )
        }
        return ScoredInsight(
            Insight(
                "Today's Spending",
                "KES ${formatAmount(todaySpend)} spent today. Well within your usual daily pace.",
                InsightType.INFO,
                actionRoute = "all_transactions?filter=Today"
            ), 40
        )
    }

    // 4. Income → Save Nudge
    private fun signalIncomeSaveNudge(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val oneDayAgo = now - 24L * 60 * 60 * 1000L
        val recentIncomes = transactions.filter { 
            it.timestamp >= oneDayAgo && 
            (it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME)
        }
        val totalRecentIncome = recentIncomes.sumOf { it.amount }
        if (totalRecentIncome > 1000.0) {
            val saveAmount = totalRecentIncome * 0.20
            return ScoredInsight(
                Insight(
                    "Savings Nudge",
                    "You received KES ${formatAmount(totalRecentIncome)} recently. Consider moving KES ${formatAmount(saveAmount)} to savings (20% rule).",
                    InsightType.SUCCESS
                ), 80
            )
        }
        return null
    }

    // 5. Unusual Single Transaction
    private fun signalUnusualSingleTransaction(transactions: List<Transaction>, monthlyExpense: Double, now: Long): ScoredInsight? {
        val cal = getCal(now)
        val dayOfMonth = Math.max(1, cal.get(Calendar.DAY_OF_MONTH))
        val dailyAvg = monthlyExpense / dayOfMonth
        if (dailyAvg < 500) return null
        
        val sixHoursAgo = now - 6L * 60 * 60 * 1000L
        val recentSpikeTx = transactions.filter { it.timestamp >= sixHoursAgo && isExpense(it) }
            .maxByOrNull { it.amount }
            
        if (recentSpikeTx != null && recentSpikeTx.amount > dailyAvg * 3.0 && recentSpikeTx.amount > 2000.0) {
            val payeeName = if (recentSpikeTx.payee.length > 15) recentSpikeTx.payee.take(12) + "..." else recentSpikeTx.payee
            return ScoredInsight(
                Insight(
                    "Large Payment",
                    "KES ${formatAmount(recentSpikeTx.amount)} to $payeeName — much bigger than your usual daily spend.",
                    InsightType.WARNING,
                    category = recentSpikeTx.category
                ), 65
            )
        }
        return null
    }

    // 6. Spending Spike
    private fun signalSpendingSpike(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val currentMonthStart = getStartOfMonth(now)
        val cal = getCal(currentMonthStart)
        cal.add(Calendar.MONTH, -1)
        val prevMonthStart = cal.timeInMillis
        
        val currTxns = transactions.filter { it.timestamp >= currentMonthStart && isExpense(it) }
        val prevTxns = transactions.filter { it.timestamp in prevMonthStart until currentMonthStart && isExpense(it) }
        
        val currByCat = currTxns.groupBy { it.category ?: "Other" }.mapValues { it.value.sumOf { t -> t.amount } }
        val prevByCat = prevTxns.groupBy { it.category ?: "Other" }.mapValues { it.value.sumOf { t -> t.amount } }
        
        var topSpike: String? = null
        var maxDelta = 0.0
        var topMult = 1.0
        
        currByCat.forEach { (cat, currAmt) ->
            if (cat.isBlank() || cat.contains("Fuliza", true) || cat == "Other" || cat == "Transfer") return@forEach
            val prevAmt = prevByCat[cat] ?: 0.0
            val delta = currAmt - prevAmt
            if (currAmt > prevAmt * 1.3 && delta > 1000.0 && delta > maxDelta) {
                maxDelta = delta
                topSpike = cat
                topMult = currAmt / if (prevAmt == 0.0) 1.0 else prevAmt
            }
        }
        if (topSpike != null) {
            val multStr = String.format(java.util.Locale.US, "%.1f", topMult)
            val desc = if (topMult > 5.0 || prevByCat[topSpike] == 0.0) {
                "$topSpike spending is unusually high — KES ${formatAmount(currByCat[topSpike]!!)} this month."
            } else {
                "$topSpike is up ${multStr}x compared to last month — KES ${formatAmount(maxDelta)} more."
            }
            return ScoredInsight(
                Insight("Spending Spike", desc, InsightType.WARNING, topSpike, "analytics"), 55
            )
        }
        return null
    }

    // 7. Fuliza Risk
    private fun signalFulizaRisk(fulizaOutstanding: Double, fulizaDueDate: String, now: Long): ScoredInsight? {
        if (fulizaOutstanding <= 0.0) return null
        
        if (fulizaDueDate.isBlank()) {
            return ScoredInsight(
                Insight("Fuliza Active", "You have KES ${formatAmount(fulizaOutstanding)} outstanding on Fuliza.", InsightType.WARNING, actionRoute = "all_transactions?filter=Fuliza"), 50
            )
        }
        
        // Parse due date if possible (e.g., "15.08.2023")
        var daysDue = 5 // default fallback
        try {
            val parts = fulizaDueDate.split(".")
            if (parts.size == 3) {
                val d = parts[0].toInt()
                val m = parts[1].toInt() - 1
                val y = parts[2].toInt()
                val dueCal = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                val startOfToday = getStartOfDay(now)
                daysDue = ((dueCal.timeInMillis - startOfToday) / (1000L * 60 * 60 * 24)).toInt()
            }
        } catch (e: Exception) { }
        
        val score = if (daysDue <= 2) 90 else if (daysDue <= 5) 70 else 50
        val whenStr = if (daysDue <= 0) "today" else if (daysDue == 1) "tomorrow" else "in $daysDue days"
        
        return ScoredInsight(
            Insight(
                "Fuliza Risk",
                "Fuliza balance of KES ${formatAmount(fulizaOutstanding)} is due $whenStr. Avoid new charges to reduce interest.",
                InsightType.WARNING,
                actionRoute = "all_transactions?filter=Fuliza"
            ), score
        )
    }

    // 8. Savings Win
    private fun signalSavingsWin(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val cal = getCal(now)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        if (dayOfMonth < 10) return null
        
        val currentMonthStart = getStartOfMonth(now)
        
        val prevMonthCal = getCal(currentMonthStart).apply { add(Calendar.MONTH, -1) }
        val prevMonthStart = prevMonthCal.timeInMillis
        val prevMonthSameDay = getCal(now).apply { add(Calendar.MONTH, -1) }.timeInMillis
        
        val currExpense = transactions.filter { it.timestamp >= currentMonthStart && isExpense(it) }.sumOf { it.amount }
        val prevExpenseToSameDay = transactions.filter { it.timestamp in prevMonthStart..prevMonthSameDay && isExpense(it) }.sumOf { it.amount }
        
        if (prevExpenseToSameDay > 2000.0 && currExpense < prevExpenseToSameDay * 0.8) {
            val saved = prevExpenseToSameDay - currExpense
            val pct = (saved / prevExpenseToSameDay) * 100
            return ScoredInsight(
                Insight(
                    "Savings Track",
                    "You're spending ${pct.toInt()}% less than this time last month. On track to save KES ${formatAmount(saved)} more.",
                    InsightType.SUCCESS
                ), 45
            )
        }
        return null
    }

    // 9. Month-End Warning
    private fun signalMonthEndWarning(transactions: List<Transaction>, currentBudgetLimit: Double, now: Long): ScoredInsight? {
        val cal = getCal(now)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        if (dayOfMonth < 25) return null
        
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - dayOfMonth
        if (daysLeft <= 0) return null
        
        val currentMonthStart = getStartOfMonth(now)
        val prevMonthCal = getCal(currentMonthStart).apply { add(Calendar.MONTH, -1) }
        val prevMonthStart = prevMonthCal.timeInMillis
        
        val currExpense = transactions.filter { it.timestamp >= currentMonthStart && isExpense(it) }.sumOf { it.amount }
        val prevMonthExpense = transactions.filter { it.timestamp in prevMonthStart until currentMonthStart && isExpense(it) }.sumOf { it.amount }
        
        val dailyPace = currExpense / dayOfMonth
        val projEnd = currExpense + (dailyPace * daysLeft)
        
        if (currentBudgetLimit > 0 && projEnd > currentBudgetLimit * 1.05) {
            val over = projEnd - currentBudgetLimit
            return ScoredInsight(
                Insight("Month-End Warning", "$daysLeft days left. At your current pace, you'll exceed your budget by KES ${formatAmount(over)}.", InsightType.WARNING), 75
            )
        } else if (prevMonthExpense > 0 && projEnd > prevMonthExpense * 1.15) {
            val over = projEnd - prevMonthExpense
            return ScoredInsight(
                Insight("Month-End Warning", "$daysLeft days left. At your current pace, you'll spend KES ${formatAmount(over)} more than last month.", InsightType.WARNING), 75
            )
        }
        return null
    }

    // 10. Category Uncertainty
    private fun signalCategoryUncertainty(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
        val uncategorizedCount = transactions.count { it.timestamp >= thirtyDaysAgo && it.isUncategorized() && isExpense(it) }
        
        if (uncategorizedCount >= 3) {
            val extraScore = Math.min(20, (uncategorizedCount / 5) * 5)
            return ScoredInsight(
                Insight(
                    "Categorize Transactions",
                    "$uncategorizedCount uncategorized payments are affecting your analytics accuracy. Tap to review.",
                    InsightType.INFO,
                    actionRoute = "all_transactions?filter=uncategorized"
                ), 60 + extraScore
            )
        }
        return null
    }

    // 11. Multi-Period Spend Comparison
    private fun signalMultiPeriodComparison(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val comps = mutableListOf<PeriodComparison>()
        val cal = getCal(now)
        
        // --- Daily (Today vs Yesterday) ---
        val todayStart = getStartOfDay(now)
        val yesterdayStart = todayStart - 24L * 60 * 60 * 1000L
        val todayTxns = transactions.filter { it.timestamp >= todayStart && isExpense(it) }
        val yestTxns = transactions.filter { it.timestamp in yesterdayStart until todayStart && isExpense(it) }
        
        if (todayTxns.size >= 2) {
            comps.add(createComparison("today vs yesterday", "today", "yesterday", todayTxns, yestTxns))
        }
        
        // --- Weekly (This Mon vs Last Mon-Sun) ---
        // Android Calendar: Sunday=1, Monday=2. We want Mon=first day.
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Adjust so Monday is 0, Sunday is 6
        val adjustedDow = (dow + 5) % 7 
        
        if (adjustedDow >= 2) { // Wednesday or later
            val thisWeekStart = todayStart - (adjustedDow * 24L * 60 * 60 * 1000L)
            val lastWeekStart = thisWeekStart - 7L * 24 * 60 * 60 * 1000L
            val lastWeekEnd = thisWeekStart - 1 // End of Sunday
            
            val thisWeekTxns = transactions.filter { it.timestamp >= thisWeekStart && isExpense(it) }
            val lastWeekTxns = transactions.filter { it.timestamp in lastWeekStart..lastWeekEnd && isExpense(it) }
            comps.add(createComparison("this week vs last", "this week", "last week", thisWeekTxns, lastWeekTxns))
        }
        
        // --- Monthly (This Month vs Same Point Last Month) ---
        val dom = cal.get(Calendar.DAY_OF_MONTH)
        if (dom >= 7) {
            val thisMonthStart = getStartOfMonth(now)
            val lastMonthStart = getCal(thisMonthStart).apply { add(Calendar.MONTH, -1) }.timeInMillis
            val lastMonthSamePoint = getCal(now).apply { add(Calendar.MONTH, -1) }.timeInMillis
            
            val thisMonthTxns = transactions.filter { it.timestamp >= thisMonthStart && isExpense(it) }
            val lastMonthTxns = transactions.filter { it.timestamp in lastMonthStart..lastMonthSamePoint && isExpense(it) }
            comps.add(createComparison("this month vs last", "this month", "last month", thisMonthTxns, lastMonthTxns))
        }
        
        // Find most striking comparison
        var bestComp: PeriodComparison? = null
        var maxDiffRatio = 0.0
        
        for (comp in comps) {
            if (Math.abs(comp.deltaAbs) > 500.0) {
                val diffRatio = Math.abs(comp.ratio - 1.0)
                if (diffRatio > maxDiffRatio) {
                    maxDiffRatio = diffRatio
                    bestComp = comp
                }
            }
        }
        
        if (bestComp == null) return null
        
        if (bestComp.ratio > 1.25) { // Overspending
            val score = if (bestComp.ratio > 1.5) 72 else 55
            val desc = if (bestComp.topCategoryName != null && bestComp.topCategoryCurrentAmount > bestComp.topCategoryPrevAmount * 1.5) {
                "${bestComp.topCategoryName} is ${String.format(java.util.Locale.US, "%.1f", bestComp.topCategoryCurrentAmount / (if(bestComp.topCategoryPrevAmount==0.0) 1.0 else bestComp.topCategoryPrevAmount))}x higher ${bestComp.label.split(" vs ")[0]} — KES ${formatAmount(bestComp.topCategoryCurrentAmount)} vs KES ${formatAmount(bestComp.topCategoryPrevAmount)} ${bestComp.label.split(" vs ")[1]}."
            } else {
                "You've spent ${String.format(java.util.Locale.US, "%.1f", bestComp.ratio)}x more ${bestComp.label.split(" vs ")[0]} than ${bestComp.label.split(" vs ")[1]} — KES ${formatAmount(bestComp.currentAmount)} vs KES ${formatAmount(bestComp.previousAmount)}."
            }
            return ScoredInsight(Insight("Spend Comparison", desc, InsightType.WARNING, actionRoute = "analytics"), score)
        } else if (bestComp.ratio < 0.8) { // Underspending
            val desc = if (bestComp.topCategoryName != null && bestComp.topCategoryCurrentAmount < bestComp.topCategoryPrevAmount * 0.5) {
                "${bestComp.topCategoryName} spend is down ${((1-bestComp.topCategoryCurrentAmount/(if(bestComp.topCategoryPrevAmount==0.0) 1.0 else bestComp.topCategoryPrevAmount))*100).toInt()}% ${bestComp.label.split(" vs ")[0]} compared to ${bestComp.label.split(" vs ")[1]}."
            } else {
                "Quieter ${bestComp.label.split(" vs ")[0].replace("this ", "").replace("today", "day")} — KES ${formatAmount(bestComp.currentAmount)} spent vs KES ${formatAmount(bestComp.previousAmount)} ${bestComp.label.split(" vs ")[1]}."
            }
            return ScoredInsight(Insight("Spend Comparison", desc, InsightType.INFO, actionRoute = "analytics"), 40)
        }
        return null
    }
    
    private fun createComparison(label: String, curLabel: String, prevLabel: String, curTxns: List<Transaction>, prevTxns: List<Transaction>): PeriodComparison {
        val curAmt = curTxns.sumOf { it.amount }
        val prevAmt = prevTxns.sumOf { it.amount }
        
        val curByCat = curTxns.groupBy { it.category ?: "Other" }.mapValues { it.value.sumOf { t -> t.amount } }
        val prevByCat = prevTxns.groupBy { it.category ?: "Other" }.mapValues { it.value.sumOf { t -> t.amount } }
        
        var topCat: String? = null
        var maxDelta = 0.0
        
        curByCat.forEach { (cat, cAmt) ->
            if (cat.isBlank() || cat.contains("Fuliza", true) || cat == "Other" || cat == "Transfer" || cat == "Send Money" || cat == "Paybill" || cat == "Buy Goods") return@forEach
            val pAmt = prevByCat[cat] ?: 0.0
            val deltaAbs = Math.abs(cAmt - pAmt)
            // If category drives >60% of total absolute delta
            if (deltaAbs > Math.abs(curAmt - prevAmt) * 0.6 && deltaAbs > maxDelta) {
                maxDelta = deltaAbs
                topCat = cat
            }
        }
        
        return PeriodComparison(
            label = label,
            currentAmount = curAmt,
            previousAmount = prevAmt,
            topCategoryName = topCat,
            topCategoryCurrentAmount = if (topCat != null) curByCat[topCat]!! else 0.0,
            topCategoryPrevAmount = if (topCat != null) prevByCat[topCat] ?: 0.0 else 0.0
        )
    }

    // 12. Yesterday's Spend
    fun signalYesterdaySpend(transactions: List<Transaction>, monthlyExpense: Double, now: Long): ScoredInsight? {
        val todayStart = getStartOfDay(now)
        val yesterdayStart = todayStart - 24L * 60 * 60 * 1000L
        val yestTxns = transactions.filter { it.timestamp in yesterdayStart until todayStart && isExpense(it) }
        val yestSpend = yestTxns.sumOf { it.amount }

        val cal = getCal(now)
        val dayOfMonth = Math.max(1, cal.get(Calendar.DAY_OF_MONTH))
        val dailyAvg = if (monthlyExpense > 0 && dayOfMonth > 1) monthlyExpense / dayOfMonth else 1200.0

        if (yestTxns.isEmpty() || yestSpend == 0.0) {
            // Check if user has active transaction history in the past 30 days
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
            val hasRecentHistory = transactions.any { it.timestamp >= thirtyDaysAgo }
            if (hasRecentHistory) {
                return ScoredInsight(
                    Insight(
                        title = "Zero-Spend Day 🎉",
                        description = "You spent KES 0 yesterday — excellent financial discipline keeping expenses down!",
                        type = InsightType.SUCCESS,
                        actionRoute = "all_transactions?filter=Yesterday"
                    ), 78
                )
            }
            return null
        }

        val topCategory = yestTxns
            .filter { !it.category.isNullOrBlank() && it.category != "Other" }
            .groupBy { it.category!! }
            .maxByOrNull { it.value.sumOf { t -> t.amount } }

        val topCatStr = if (topCategory != null) " (Top: ${topCategory.key})" else ""
        val txCountStr = "${yestTxns.size} transaction${if (yestTxns.size > 1) "s" else ""}"

        if (yestSpend > dailyAvg * 2.2 && yestSpend >= 2000.0) {
            val mult = String.format(Locale.US, "%.1f", yestSpend / dailyAvg)
            return ScoredInsight(
                Insight(
                    title = "High Yesterday Spend",
                    description = "You spent KES ${formatAmount(yestSpend)} yesterday${topCatStr} — ${mult}x your daily average.",
                    type = InsightType.WARNING,
                    actionRoute = "all_transactions?filter=Yesterday"
                ), 88
            )
        }

        return ScoredInsight(
            Insight(
                title = "Yesterday's Spend",
                description = "You spent KES ${formatAmount(yestSpend)} yesterday across $txCountStr$topCatStr.",
                type = InsightType.INFO,
                actionRoute = "all_transactions?filter=Yesterday"
            ), 76
        )
    }

    // 13. Weekend Bias & Weekend Wrap
    fun signalWeekendBias(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val cal = getCal(now)
        val dow = cal.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2, ... Saturday=7
        val isMonday = (dow == Calendar.MONDAY)

        val todayStart = getStartOfDay(now)

        if (isMonday) {
            // Sunday (yesterday) + Saturday (2 days ago) + Friday (3 days ago)
            val fridayStart = todayStart - (3L * 24 * 60 * 60 * 1000L)
            val weekendTxns = transactions.filter { it.timestamp in fridayStart until todayStart && isExpense(it) }
            val weekendSpend = weekendTxns.sumOf { it.amount }
            if (weekendSpend >= 1000.0) {
                return ScoredInsight(
                    Insight(
                        title = "Weekend Wrap-Up",
                        description = "You spent KES ${formatAmount(weekendSpend)} over the weekend across ${weekendTxns.size} transactions.",
                        type = InsightType.INFO,
                        actionRoute = "analytics"
                    ), 82
                )
            }
        }

        // Rolling 30-day weekend vs weekday ratio
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
        val recentTxns = transactions.filter { it.timestamp >= thirtyDaysAgo && isExpense(it) }
        if (recentTxns.size < 8) return null

        var weekendSum = 0.0
        var totalSum = 0.0
        for (tx in recentTxns) {
            val tCal = getCal(tx.timestamp)
            val day = tCal.get(Calendar.DAY_OF_WEEK)
            totalSum += tx.amount
            if (day == Calendar.FRIDAY || day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
                weekendSum += tx.amount
            }
        }

        if (totalSum >= 5000.0) {
            val weekendRatio = weekendSum / totalSum
            if (weekendRatio >= 0.58) {
                val pct = (weekendRatio * 100).toInt()
                return ScoredInsight(
                    Insight(
                        title = "Weekend Pattern",
                        description = "$pct% of your spending occurs on weekends (Fri-Sun). Plan ahead for weekend entertainment & dining.",
                        type = InsightType.INFO,
                        actionRoute = "analytics"
                    ), 66
                )
            }
        }
        return null
    }

    // 14. Frequent Merchant Weekly (Top merchant in past 7 days)
    fun signalFrequentMerchantWeekly(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000L
        val weekTxns = transactions.filter { 
            it.timestamp >= sevenDaysAgo && 
            isExpense(it) && 
            !it.payee.isNullOrBlank() &&
            !it.payee.equals("M-PESA", ignoreCase = true) &&
            !it.payee.equals("M-Shwari", ignoreCase = true) &&
            !it.payee.equals("Fuliza", ignoreCase = true) &&
            !it.payee.equals("Airtime", ignoreCase = true)
        }

        if (weekTxns.isEmpty()) return null

        val merchantGroups = weekTxns.groupBy { it.payee.trim() }
        val topMerchant = merchantGroups.maxByOrNull { it.value.size } ?: return null

        if (topMerchant.value.size >= 3) {
            val totalSpent = topMerchant.value.sumOf { it.amount }
            val count = topMerchant.value.size
            return ScoredInsight(
                Insight(
                    title = "Frequent Merchant",
                    description = "You paid ${topMerchant.key} $count times this week totaling KES ${formatAmount(totalSpent)}.",
                    type = InsightType.INFO,
                    actionRoute = "payee_history?payee=${topMerchant.key}"
                ), 68
            )
        }
        return null
    }

    // 15. Frequent Merchant Daily (Multiple visits to same payee today)
    fun signalFrequentMerchantDaily(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val todayStart = getStartOfDay(now)
        val todayTxns = transactions.filter {
            it.timestamp >= todayStart && 
            isExpense(it) && 
            !it.payee.isNullOrBlank() &&
            !it.payee.equals("M-PESA", ignoreCase = true) &&
            !it.payee.equals("M-Shwari", ignoreCase = true)
        }

        val topToday = todayTxns.groupBy { it.payee.trim() }.maxByOrNull { it.value.size } ?: return null
        if (topToday.value.size >= 2) {
            val total = topToday.value.sumOf { it.amount }
            return ScoredInsight(
                Insight(
                    title = "Multiple Visits Today",
                    description = "You paid ${topToday.key} ${topToday.value.size} times today (KES ${formatAmount(total)}).",
                    type = InsightType.INFO,
                    actionRoute = "payee_history?payee=${topToday.key}"
                ), 52
            )
        }
        return null
    }

    // 16. M-PESA Carrier Fees Toll
    fun signalCarrierFeesToll(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val monthStart = getStartOfMonth(now)
        val monthlyFees = transactions.filter { it.timestamp >= monthStart }.sumOf { it.fee }
        if (monthlyFees >= 200.0) {
            return ScoredInsight(
                Insight(
                    title = "M-PESA Tariff Check",
                    description = "You've paid KES ${formatAmount(monthlyFees)} in carrier fees this month. Tip: Pay via Buy Goods / Till (0 charges) to save.",
                    type = InsightType.INFO,
                    actionRoute = "analytics"
                ), 64
            )
        }
        return null
    }

    // 17. Savings Growth & Momentum
    fun signalSavingsGrowth(transactions: List<Transaction>, now: Long): ScoredInsight? {
        val currentMonthStart = getStartOfMonth(now)
        val prevMonthCal = getCal(currentMonthStart).apply { add(Calendar.MONTH, -1) }
        val prevMonthStart = prevMonthCal.timeInMillis

        val currSavings = transactions.filter { 
            it.timestamp >= currentMonthStart && 
            (it.type == TransactionType.MSHWARI_TRANSFER || it.category.equals("Savings", ignoreCase = true))
        }.sumOf { it.amount }

        val prevSavings = transactions.filter { 
            it.timestamp in prevMonthStart until currentMonthStart && 
            (it.type == TransactionType.MSHWARI_TRANSFER || it.category.equals("Savings", ignoreCase = true))
        }.sumOf { it.amount }

        if (currSavings >= 2000.0 && currSavings > prevSavings && prevSavings > 0) {
            val growthPct = (((currSavings - prevSavings) / prevSavings) * 100).toInt()
            return ScoredInsight(
                Insight(
                    title = "Savings Growth 🎉",
                    description = "You've saved KES ${formatAmount(currSavings)} this month — up $growthPct% compared to last month!",
                    type = InsightType.SUCCESS,
                    actionRoute = "all_transactions"
                ), 84
            )
        }
        return null
    }

    // 18. Budget Safe Pace
    fun signalBudgetSafePace(limit: Double, expense: Double, now: Long): ScoredInsight? {
        if (limit <= 0) return null
        val cal = getCal(now)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        if (dayOfMonth < 20) return null
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - dayOfMonth + 1

        val pct = expense / limit
        if (pct <= 0.60 && daysLeft in 1..11) {
            val projectedSaved = limit - expense
            return ScoredInsight(
                Insight(
                    title = "Budget on Track 🎯",
                    description = "$daysLeft days left and you've used only ${(pct * 100).toInt()}% of your budget. You're set to save KES ${formatAmount(projectedSaved)}!",
                    type = InsightType.SUCCESS,
                    actionRoute = "budget_planner"
                ), 82
            )
        }
        return null
    }

    // 19. Month-End Survival Gauge
    fun signalMonthEndGauge(limit: Double, expense: Double, now: Long): ScoredInsight? {
        if (limit <= 0) return null
        val cal = getCal(now)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        if (dayOfMonth < 25) return null
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - dayOfMonth + 1
        if (daysLeft <= 0) return null

        val remaining = limit - expense
        if (remaining > 0) {
            val dailyAllowance = remaining / daysLeft
            return ScoredInsight(
                Insight(
                    title = "Month-End Daily Budget",
                    description = "$daysLeft days left. Your safe daily spending allowance is KES ${formatAmount(dailyAllowance)} to stay within budget.",
                    type = InsightType.INFO,
                    actionRoute = "budget_planner"
                ), 74
            )
        }
        return null
    }

    // ── Dedicated Helper Extractors for Notification Workers ──────────────────

    fun getYesterdaySpendInsight(transactions: List<Transaction>, monthlyExpense: Double, now: Long = System.currentTimeMillis()): Insight? =
        signalYesterdaySpend(transactions, monthlyExpense, now)?.insight

    fun getWeekendWrapInsight(transactions: List<Transaction>, now: Long = System.currentTimeMillis()): Insight? =
        signalWeekendBias(transactions, now)?.insight

    fun getFrequentMerchantWeeklyInsight(transactions: List<Transaction>, now: Long = System.currentTimeMillis()): Insight? =
        signalFrequentMerchantWeekly(transactions, now)?.insight

    fun getWeeklyBudgetOutlookInsight(budgets: List<Budget>, monthlyExpense: Double, now: Long = System.currentTimeMillis()): Insight? {
        val globalBudget = budgets.find { it.category == "Overall" }
        val limit = globalBudget?.limitAmount ?: 0.0
        if (limit <= 0.0) return null

        val cal = getCal(now)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = daysInMonth - dayOfMonth + 1
        val remaining = limit - monthlyExpense
        if (remaining > 0 && daysLeft > 0) {
            val weeklyPace = (remaining / daysLeft) * 7.coerceAtMost(daysLeft)
            return Insight(
                title = "Weekly Budget Outlook",
                description = "You have KES ${formatAmount(remaining)} remaining in your budget for the next $daysLeft days (~KES ${formatAmount(weeklyPace)} for this week).",
                type = InsightType.INFO,
                actionRoute = "budget_planner"
            )
        }
        return null
    }

    fun getMpesaTariffSaverInsight(transactions: List<Transaction>, now: Long = System.currentTimeMillis()): Insight? =
        signalCarrierFeesToll(transactions, now)?.insight
}


