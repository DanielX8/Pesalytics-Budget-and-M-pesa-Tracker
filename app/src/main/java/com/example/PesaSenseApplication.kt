package com.pesasense

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pesasense.data.AppDatabase
import com.pesasense.data.PesaRepository
import com.pesasense.workers.DailySpendWorker
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
        scheduleDailySpendWorker()
    }

    private fun scheduleDailySpendWorker() {
        val now = Calendar.getInstance()

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<DailySpendWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag("daily_spend_notification")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_spend_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
