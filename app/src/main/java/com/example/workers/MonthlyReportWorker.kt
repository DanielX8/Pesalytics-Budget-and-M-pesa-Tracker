package com.pesalytics.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pesalytics.PesalyticsApplication
import com.pesalytics.model.TransactionType
import com.pesalytics.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar

class MonthlyReportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as PesalyticsApplication).repository
        val notif = NotificationHelper(applicationContext)
        val prefs = applicationContext.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)

        // ── Last month's financial summary ───────────────────────────────────
        val lastMonthStart = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val lastMonthEnd = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis - 1

        val allTransactions = repository.allTransactions.first()
        val monthTxns = allTransactions.filter {
            it.timestamp in lastMonthStart..lastMonthEnd && !it.isFeeTransaction
        }

        val income = monthTxns
            .filter { it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME }
            .sumOf { it.amount }
        val expense = monthTxns
            .filter {
                it.type != TransactionType.RECEIVE_MONEY &&
                it.type != TransactionType.MANUAL_INCOME &&
                it.type != TransactionType.MANUAL_TRANSFER &&
                it.type != TransactionType.MSHWARI_TRANSFER &&
                it.type != TransactionType.POCHI_TRANSFER &&
                it.type != TransactionType.POCHI_RECEIVE &&
                it.type != TransactionType.FULIZA
            }
            .sumOf { it.amount }
        val savings = income - expense
        val txnCount = monthTxns.size
        val topCat = monthTxns
            .filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
            .groupBy { it.category ?: "Other" }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .maxByOrNull { it.value }?.key

        val cal = Calendar.getInstance().apply { timeInMillis = lastMonthStart }
        val monthName = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(cal.time)

        val body = buildString {
            append("Income KES ${"%.0f".format(income)} · Expenses KES ${"%.0f".format(expense)} · ")
            append(if (savings >= 0) "Saved KES ${"%.0f".format(savings)}." else "Over budget by KES ${"%.0f".format(-savings)}.")
            if (topCat != null) append(" Top spend: $topCat.")
        }

        notif.showMonthlyReport("$monthName Financial Summary", body)
        appendInAppNotification(
            prefs,
            "$monthName summary: KES ${"%.0f".format(income)} in, KES ${"%.0f".format(expense)} out across $txnCount transactions. " +
            if (savings >= 0) "Saved KES ${"%.0f".format(savings)}." else "Over by KES ${"%.0f".format(-savings)}."
        )

        // ── Goal progress reminders ─────────────────────────────────────────
        val goals = repository.allGoals.first()
        goals.forEachIndexed { index, goal ->
            val pct = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount * 100).toInt() else 0
            val remaining = goal.targetAmount - goal.savedAmount
            val goalMsg = "$pct% complete. KES ${"%.0f".format(remaining)} remaining. Keep contributing KES ${"%.0f".format(goal.monthlyContribution)}/month."
            notif.showGoalReminder("Goal Update: ${goal.name}", goalMsg, notifId = 1003 + index)
            appendInAppNotification(prefs, "Goal '${goal.name}': $goalMsg")
        }

        // ── Self-reschedule for the 1st of next month at 9:00 AM ───────────
        val nextMonthDelay = delayUntilFirstOfNextMonth(9, 0)
        androidx.work.WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "monthly_report",
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<MonthlyReportWorker>()
                .setInitialDelay(nextMonthDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .addTag("monthly_report")
                .build()
        )

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

    private fun delayUntilFirstOfNextMonth(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
