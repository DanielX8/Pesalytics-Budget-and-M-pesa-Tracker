package com.pesalytics.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pesalytics.PesalyticsApplication
import com.pesalytics.model.TransactionType
import com.pesalytics.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailySpendWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as PesalyticsApplication).repository
        val notif = NotificationHelper(applicationContext)
        val prefs = applicationContext.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthEnd = Calendar.getInstance().apply {
            timeInMillis = monthStart; add(Calendar.MONTH, 1)
        }.timeInMillis
        val monthStr = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(monthStart))

        val transactions = repository.allTransactions.first()
        val bills = repository.allBills.first()
        val budgets = repository.getBudgetsForMonth(monthStr).first()
        val globalBudget = budgets.find { it.category == "Overall" }
        val monthlyExpense = repository.getMonthlyExpense(monthStart, monthEnd).first() ?: 0.0

        val fulizaUsageTxns = transactions.filter { it.fulizaOutstandingBalance > 0 }
        val allFulizaTxns = transactions.filter { it.type == TransactionType.FULIZA }
        val allRepayments = allFulizaTxns.filter {
            it.category == "Fuliza Full Repayment" ||
            it.category == "Fuliza Partial Repayment" ||
            it.category == "Fuliza Repayment"
        }
        val fullRepayments = allFulizaTxns.filter { it.category == "Fuliza Full Repayment" }
        val fulizaTotalLimit = fullRepayments.maxByOrNull { it.timestamp }?.fulizaLimitAfter
            ?: allFulizaTxns.filter { it.category == "Fuliza Repayment" }.maxByOrNull { it.timestamp }?.fulizaLimitAfter
            ?: 0.0

        val latestRepayment = allRepayments.maxByOrNull { it.timestamp }
        val latestUsageTxn = fulizaUsageTxns.maxByOrNull { it.timestamp }
        val latestRepaymentTime = latestRepayment?.timestamp ?: 0L
        val latestUsageTime = latestUsageTxn?.timestamp ?: 0L
        
        val fulizaOutstanding = when {
            latestUsageTime > latestRepaymentTime -> latestUsageTxn?.fulizaOutstandingBalance ?: 0.0
            latestRepayment?.category == "Fuliza Full Repayment" ||
            (latestRepayment?.category == "Fuliza Repayment" && fulizaTotalLimit > 0 &&
             latestRepayment.fulizaLimitAfter >= fulizaTotalLimit) -> 0.0
            fulizaTotalLimit > 0 -> (fulizaTotalLimit - (latestRepayment?.fulizaLimitAfter ?: 0.0)).coerceAtLeast(0.0)
            else -> latestUsageTxn?.fulizaOutstandingBalance ?: 0.0
        }
        val fulizaDueDate = latestUsageTxn?.fulizaDueDate ?: ""

        val isMorningRun = currentHour < 12

        if (isMorningRun) {
            // ── 🌅 Morning Briefing (8:00 AM - 8:30 AM) ───────────────────────
            if (dayOfWeek == Calendar.MONDAY) {
                // Monday Morning: Check Weekend Wrap first
                val weekendInsight = com.pesalytics.patterns.InsightEngine.getWeekendWrapInsight(transactions, now)
                if (weekendInsight != null) {
                    notif.showWeekendWrap(weekendInsight.title, weekendInsight.description)
                    appendInAppNotification(prefs, "${weekendInsight.title}: ${weekendInsight.description}")
                } else {
                    // Fallback to yesterday spend
                    val yestInsight = com.pesalytics.patterns.InsightEngine.getYesterdaySpendInsight(transactions, monthlyExpense, now)
                    yestInsight?.let {
                        notif.showDailySpendSummary(it.title, it.description)
                        appendInAppNotification(prefs, "${it.title}: ${it.description}")
                    }
                }
            } else {
                // Regular Morning: Yesterday's Spend / Zero Spend Pulse
                val yestInsight = com.pesalytics.patterns.InsightEngine.getYesterdaySpendInsight(transactions, monthlyExpense, now)
                if (yestInsight != null) {
                    notif.showDailySpendSummary(yestInsight.title, yestInsight.description)
                    appendInAppNotification(prefs, "${yestInsight.title}: ${yestInsight.description}")
                }
            }
        } else {
            // ── 🌆 Evening Pulse (7:30 PM) ──────────────────────────────────
            val contextualInsight = when {
                // Sunday Evening: Weekly Budget Outlook
                dayOfWeek == Calendar.SUNDAY -> {
                    com.pesalytics.patterns.InsightEngine.getWeeklyBudgetOutlookInsight(budgets, monthlyExpense, now)
                }
                // Friday Evening: M-PESA Tariff Saver Tip
                dayOfWeek == Calendar.FRIDAY -> {
                    com.pesalytics.patterns.InsightEngine.getMpesaTariffSaverInsight(transactions, now)
                }
                // Wednesday or Saturday: Frequent Merchant Tracker
                dayOfWeek == Calendar.WEDNESDAY || dayOfWeek == Calendar.SATURDAY -> {
                    com.pesalytics.patterns.InsightEngine.getFrequentMerchantWeeklyInsight(transactions, now)
                }
                else -> null
            }

            if (contextualInsight != null) {
                notif.showInsightAlert(contextualInsight.title, contextualInsight.description, contextualInsight.actionRoute)
                appendInAppNotification(prefs, "${contextualInsight.title}: ${contextualInsight.description}")
            } else {
                // General Scored Top Insight
                val insights = com.pesalytics.patterns.InsightEngine.generateInsights(
                    transactions = transactions,
                    bills = bills,
                    budgets = budgets,
                    currentBudgetLimit = globalBudget?.limitAmount ?: 0.0,
                    monthlyExpense = monthlyExpense,
                    fulizaOutstanding = fulizaOutstanding,
                    fulizaDueDate = fulizaDueDate,
                    now = now
                )

                insights.firstOrNull()?.let { topInsight ->
                    notif.showInsightAlert(topInsight.title, topInsight.description, topInsight.actionRoute)
                    appendInAppNotification(prefs, "${topInsight.title}: ${topInsight.description}")
                }
            }
        }

        // ── Subscription / trial expiry warning ─────────────────────────────
        val subPrefs = applicationContext.getSharedPreferences("pesa_subscription", android.content.Context.MODE_PRIVATE)
        val tierName = subPrefs.getString("tier", "FREE") ?: "FREE"
        val trialStartMs = subPrefs.getLong("trial_start_ms", 0L)
        val paymentExpiryMs = subPrefs.getLong("expiry_ms", 0L)
        val effectiveExpiryMs = when (tierName) {
            "TRIAL" -> if (trialStartMs > 0L) trialStartMs + TimeUnit.DAYS.toMillis(14) else 0L
            "PREMIUM_MONTHLY", "PREMIUM_QUARTERLY", "PREMIUM_YEARLY" -> paymentExpiryMs
            else -> 0L
        }
        if (effectiveExpiryMs > 0L) {
            val daysLeft = ((effectiveExpiryMs - now) / (1000 * 60 * 60 * 24)).toInt()
            if (daysLeft in 0..3) {
                notif.showSubscriptionExpiryAlert(isTrial = tierName == "TRIAL", daysLeft = daysLeft)
            }
        }

        // ── Reschedule for next slot (Alternates between 8:00 AM and 7:30 PM) ──
        val nextTargetHour = if (isMorningRun) 19 else 8
        val nextTargetMinute = if (isMorningRun) 30 else 0
        val nextDelay = delayUntilTime(nextTargetHour, nextTargetMinute)

        val workRequest = androidx.work.OneTimeWorkRequestBuilder<DailySpendWorker>()
            .setInitialDelay(nextDelay, TimeUnit.MILLISECONDS)
            .addTag("daily_spend_notification")
            .build()
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "daily_spend_notification",
            androidx.work.ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return Result.success()
    }

    private fun delayUntilTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    private fun appendInAppNotification(
        prefs: android.content.SharedPreferences,
        message: String
    ) {
        val existing = prefs.getString("pending_in_app_notifs", "") ?: ""
        val updated = if (existing.isBlank()) message else "$existing\n$message"
        prefs.edit().putString("pending_in_app_notifs", updated).apply()
    }
}
