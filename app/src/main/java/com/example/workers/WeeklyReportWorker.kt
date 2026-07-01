package com.pesalytics.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pesalytics.PesalyticsApplication
import com.pesalytics.model.TransactionType
import com.pesalytics.notifications.NotificationHelper
import kotlinx.coroutines.flow.first
import java.util.Calendar

class WeeklyReportWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)

        val repository = (applicationContext as PesalyticsApplication).repository
        val notif = NotificationHelper(applicationContext)

        // ── Previous complete week (Mon 00:00 → Sun 23:59:59) ───────────────
        val now = Calendar.getInstance()

        val currentWeekStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis > now.timeInMillis) add(Calendar.WEEK_OF_YEAR, -1)
        }.timeInMillis

        val prevWeekStart = currentWeekStart - 7L * 24 * 60 * 60 * 1000
        val prevWeekEnd   = currentWeekStart - 1L

        val allTransactions = repository.allTransactions.first()
        val weekTxns = allTransactions.filter {
            it.timestamp in prevWeekStart..prevWeekEnd && !it.isFeeTransaction
        }
        val weekExpense = weekTxns
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
        val weekIncome = weekTxns
            .filter { it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME }
            .sumOf { it.amount }
        val txnCount = weekTxns.size
        val topCat = weekTxns
            .filter { it.type != TransactionType.RECEIVE_MONEY && it.type != TransactionType.MANUAL_INCOME }
            .groupBy { it.category ?: "Other" }
            .mapValues { e -> e.value.sumOf { it.amount } }
            .maxByOrNull { it.value }?.key

        val dateFmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
        val weekLabel = "${dateFmt.format(java.util.Date(prevWeekStart))} – ${dateFmt.format(java.util.Date(prevWeekEnd))}"

        val body = buildString {
            append("$weekLabel: KES ${"%.0f".format(weekExpense)} spent, KES ${"%.0f".format(weekIncome)} received.")
            if (topCat != null) append(" Top category: $topCat.")
            if (weekExpense > weekIncome && weekIncome > 0)
                append(" Spent KES ${"%.0f".format(weekExpense - weekIncome)} more than received.")
            else if (weekIncome > weekExpense && weekExpense > 0)
                append(" Saved KES ${"%.0f".format(weekIncome - weekExpense)} — great week!")
        }

        notif.showWeeklyReport("Last Week's Spending Report", body)
        appendInAppNotification(prefs, "Weekly report ($weekLabel): KES ${"%.0f".format(weekExpense)} spent across $txnCount transactions.")

        // ── Bills due in the next 7 days ────────────────────────────────────
        val nowMs = System.currentTimeMillis()
        val nextWeek = nowMs + 7L * 24 * 60 * 60 * 1000
        val dueSoon = repository.allBills.first()
            .filter { !it.isPaid && it.nextDueDate in nowMs..nextWeek }

        if (dueSoon.isNotEmpty()) {
            val total = dueSoon.sumOf { it.amount }
            val names = dueSoon.joinToString(", ") { it.name }
            notif.showBillAlert("Bills Due This Week", "$names — total KES ${"%.2f".format(total)}", notifId = 1007)
            appendInAppNotification(prefs, "Bills due this week: $names (KES ${"%.0f".format(total)} total)")
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
