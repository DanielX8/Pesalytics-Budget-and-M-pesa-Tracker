package com.pesasense.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pesasense.PesaSenseApplication
import com.pesasense.notifications.NotificationHelper
import com.pesasense.model.TransactionType
import kotlinx.coroutines.flow.first
import java.util.Calendar

class WeeklyReportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getString("report_frequency", "Daily") == "Monthly") return Result.success()

        val repository = (applicationContext as PesaSenseApplication).repository
        val notif = NotificationHelper(applicationContext)

        // ── This week's spending (Mon 00:00 → now) ──────────────────────────
        val weekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            // If today is before Monday (i.e., Sunday), go back one week
            if (timeInMillis > System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, -1)
        }.timeInMillis

        val weekExpense = repository.getDailyExpense(weekStart, System.currentTimeMillis()) ?: 0.0

        val allTransactions = repository.allTransactions.first()
        val weekIncome = allTransactions
            .filter { it.timestamp >= weekStart &&
                (it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME) }
            .sumOf { it.amount }

        notif.showWeeklyReport(
            "Weekly Spending Recap",
            "This week: KES ${"%.0f".format(weekExpense)} spent, KES ${"%.0f".format(weekIncome)} received." +
            if (weekExpense > weekIncome && weekIncome > 0)
                " Spending exceeded income by KES ${"%.0f".format(weekExpense - weekIncome)}."
            else ""
        )

        // ── Bills due in the next 7 days ────────────────────────────────────
        val now = System.currentTimeMillis()
        val nextWeek = now + 7L * 24 * 60 * 60 * 1000
        val dueSoon = repository.allBills.first()
            .filter { !it.isPaid && it.nextDueDate in now..nextWeek }

        if (dueSoon.isNotEmpty()) {
            val total = dueSoon.sumOf { it.amount }
            val names = dueSoon.joinToString(", ") { it.name }
            notif.showBillAlert(
                "Bills Due This Week",
                "$names — total KES ${"%.2f".format(total)}"
            )
        }

        return Result.success()
    }
}
