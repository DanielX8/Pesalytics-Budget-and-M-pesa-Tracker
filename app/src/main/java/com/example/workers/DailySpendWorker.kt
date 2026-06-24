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
        val frequency = prefs.getString("report_frequency", "Daily") ?: "Daily"

        // ── Yesterday's spending summary (only for Daily frequency) ─────────
        if (frequency == "Daily") {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val startYesterday = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            val endYesterday = cal.timeInMillis

            val expense = repository.getDailyExpense(startYesterday, endYesterday) ?: 0.0
            if (expense > 0) {
                // Enrich with transaction count and top category
                val yesterdayTxns = repository.allTransactions.first()
                    .filter { it.timestamp in startYesterday..endYesterday && !it.isFeeTransaction }
                val txnCount = yesterdayTxns.size
                val topCat = yesterdayTxns
                    .filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
                    .groupBy { it.category ?: "Other" }
                    .mapValues { e -> e.value.sumOf { it.amount } }
                    .maxByOrNull { it.value }?.key

                val body = buildString {
                    append("You spent KES ${"%.0f".format(expense)} across $txnCount transaction${if (txnCount == 1) "" else "s"}.")
                    if (topCat != null) append(" Top category: $topCat.")
                }

                notif.showDailySpendSummary("Yesterday's Spending", body)
                appendInAppNotification(prefs, "Yesterday's spending: KES ${"%.0f".format(expense)} — $body")
            }
        }

        // ── Bills due within 3 days (always fires) ──────────────────────────
        val now = System.currentTimeMillis()
        val threeDays = 3L * 24 * 60 * 60 * 1000
        val dueSoon = repository.allBills.first()
            .filter { !it.isPaid && it.nextDueDate in now..(now + threeDays) }

        if (dueSoon.isNotEmpty()) {
            val summary = dueSoon.joinToString("\n") { bill ->
                val days = ((bill.nextDueDate - now) / (1000 * 60 * 60 * 24)).toInt()
                val when_ = if (days == 0) "TODAY" else if (days == 1) "TOMORROW" else "in $days days"
                "${bill.name} due $when_ — KES ${"%.2f".format(bill.amount)}"
            }
            val title = if (dueSoon.size == 1) "Bill Due Soon" else "${dueSoon.size} Bills Due Soon"
            notif.showBillAlert(title, summary)
            appendInAppNotification(prefs, "$title: $summary")
        }

        // ── Budget threshold check ───────────────────────────────────────────
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
        val budgets = repository.getBudgetsForMonth(monthStr).first()
        val globalBudget = budgets.find { it.category == "Overall" }

        if (globalBudget != null && globalBudget.limitAmount > 0) {
            val monthExpense = repository.getMonthlyExpense(monthStart, monthEnd).first() ?: 0.0
            val pct = monthExpense / globalBudget.limitAmount
            when {
                pct >= 1.0 -> {
                    val msg = "You've exceeded your KES ${"%.0f".format(globalBudget.limitAmount)} monthly budget."
                    notif.showBudgetAlert("Budget Exceeded", msg)
                    appendInAppNotification(prefs, "Budget Exceeded: $msg")
                }
                pct >= 0.8 -> {
                    val msg = "You've used ${"%.0f".format(pct * 100)}% of your monthly budget."
                    notif.showBudgetAlert("Budget Warning", msg)
                    appendInAppNotification(prefs, "Budget Warning: $msg")
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

        return Result.success()
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
