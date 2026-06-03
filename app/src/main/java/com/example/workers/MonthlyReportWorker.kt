package com.pesasense.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pesasense.PesaSenseApplication
import com.pesasense.notifications.NotificationHelper
import com.pesasense.model.TransactionType
import kotlinx.coroutines.flow.first
import java.util.Calendar

class MonthlyReportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as PesaSenseApplication).repository
        val notif = NotificationHelper(applicationContext)

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
            .filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
            .sumOf { it.amount }
        val savings = income - expense

        val cal = Calendar.getInstance().apply { timeInMillis = lastMonthStart }
        val monthName = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(cal.time)

        notif.showMonthlyReport(
            "$monthName Financial Summary",
            "Income KES ${"%.0f".format(income)} · Expenses KES ${"%.0f".format(expense)} · " +
            "${if (savings >= 0) "Saved" else "Over budget by"} KES ${"%.0f".format(Math.abs(savings))}."
        )

        // ── Goal progress reminders ─────────────────────────────────────────
        val goals = repository.allGoals.first()
        goals.forEach { goal ->
            val pct = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount * 100).toInt() else 0
            val remaining = goal.targetAmount - goal.savedAmount
            notif.showGoalReminder(
                "Goal Update: ${goal.name}",
                "$pct% complete. KES ${"%.0f".format(remaining)} remaining. " +
                "Keep contributing KES ${"%.0f".format(goal.monthlyContribution)}/month."
            )
        }

        // ── Self-reschedule for the 1st of next month at 9:00 AM ───────────
        // Using a OneTimeWorkRequest chain instead of a 30-day periodic job
        // ensures the report always fires on the correct calendar date regardless
        // of month length (28 / 29 / 30 / 31 days).
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
