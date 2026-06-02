package com.pesasense.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pesasense.PesaSenseApplication
import com.pesasense.notifications.NotificationHelper
import java.util.Calendar

class DailySpendWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as PesaSenseApplication).repository

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfYesterday = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfYesterday = cal.timeInMillis

        val totalExpense = repository.getDailyExpense(startOfYesterday, endOfYesterday) ?: 0.0

        if (totalExpense > 0) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.showBudgetAlert(
                "Yesterday's Spending Summary",
                "You spent KES ${"%.2f".format(totalExpense)} yesterday."
            )
        }

        return Result.success()
    }
}
