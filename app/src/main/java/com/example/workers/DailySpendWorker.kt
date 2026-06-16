package com.pesalytics.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pesalytics.PesalyticsApplication
import com.pesalytics.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar

class DailySpendWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as PesalyticsApplication).repository
        val notif = NotificationHelper(applicationContext)
        val prefs = applicationContext.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
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
                notif.showDailySpendSummary(
                    "Yesterday's Spending",
                    "You spent KES ${"%.2f".format(expense)} yesterday."
                )
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
            notif.showBillAlert(
                if (dueSoon.size == 1) "Bill Due Soon" else "${dueSoon.size} Bills Due Soon",
                summary
            )
        }

        // ── Budget threshold check (only fires when the user has set a budget) ──
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthStr = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(monthStart))
        val budgets = repository.getBudgetsForMonth(monthStr).first()
        val globalBudget = budgets.find { it.category == "Overall" }

        // Only alert if the user has actually configured a budget
        if (globalBudget != null && globalBudget.limitAmount > 0) {
            // Use getMonthlyExpense() — not getDailyExpense() — to get the full
            // month's spending so the threshold percentage is calculated correctly.
            val monthExpense = repository.getMonthlyExpense(monthStart).first() ?: 0.0
            val pct = monthExpense / globalBudget.limitAmount
            when {
                pct >= 1.0 -> notif.showBudgetAlert("Budget Exceeded",
                    "You've exceeded your KES ${"%.0f".format(globalBudget.limitAmount)} monthly budget.")
                pct >= 0.8 -> notif.showBudgetAlert("Budget Warning",
                    "You've used ${"%.0f".format(pct * 100)}% of your monthly budget.")
            }
        }

        return Result.success()
    }
}
