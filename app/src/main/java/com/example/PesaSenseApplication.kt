package com.pesasense

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pesasense.data.AppDatabase
import com.pesasense.data.PesaRepository
import com.pesasense.workers.DailySpendWorker
import com.pesasense.workers.WeeklyReportWorker
import com.pesasense.workers.MonthlyReportWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class PesaSenseApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        PesaRepository(
            database.transactionDao(),
            database.billDao(),
            database.budgetDao(),
            database.customRuleDao(),
            database.goalDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()
    }

    private fun scheduleWorkers() {
        val wm = WorkManager.getInstance(this)

        // Daily at 7:30 PM — spend summary + bill alerts + budget check
        wm.enqueueUniquePeriodicWork(
            "daily_spend_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailySpendWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayUntilTime(19, 30), TimeUnit.MILLISECONDS)
                .addTag("daily_spend_notification")
                .build()
        )

        // Weekly on Sunday at 8:00 PM — weekly recap + upcoming bills
        wm.enqueueUniquePeriodicWork(
            "weekly_report",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeeklyReportWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(delayUntilWeekday(Calendar.SUNDAY, 20, 0), TimeUnit.MILLISECONDS)
                .addTag("weekly_report")
                .build()
        )

        // Monthly on the 1st at 9:00 AM — monthly summary + goal progress
        wm.enqueueUniquePeriodicWork(
            "monthly_report",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MonthlyReportWorker>(30, TimeUnit.DAYS)
                .setInitialDelay(delayUntilFirstOfNextMonth(9, 0), TimeUnit.MILLISECONDS)
                .addTag("monthly_report")
                .build()
        )
    }

    /** Delay in ms until the next occurrence of [hour]:[minute] today or tomorrow. */
    private fun delayUntilTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    /** Delay in ms until the next [dayOfWeek] at [hour]:[minute]. */
    private fun delayUntilWeekday(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.WEEK_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    /** Delay in ms until the 1st of next month at [hour]:[minute]. */
    private fun delayUntilFirstOfNextMonth(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
