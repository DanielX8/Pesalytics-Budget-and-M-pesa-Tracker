package com.pesalytics.util

import android.content.Context
import com.pesalytics.data.AppDatabase
import com.pesalytics.model.AppNotificationEntity
import com.pesalytics.model.NotificationType
import com.pesalytics.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppNotificationDispatcher {
    suspend fun dispatch(
        context: Context,
        title: String,
        message: String,
        type: NotificationType,
        actionRoute: String? = null
    ) = withContext(Dispatchers.IO) {
        // 1. Insert into Room Database
        val db = AppDatabase.getDatabase(context)
        db.notificationDao().insertNotification(
            AppNotificationEntity(
                title = title,
                message = message,
                type = type,
                actionRoute = actionRoute
            )
        )

        // 2. Dispatch Android system notification
        val helper = NotificationHelper(context)
        when (type) {
            NotificationType.BUDGET_WARNING -> helper.showBudgetAlert(title, message)
            NotificationType.DAILY_BRIEF -> helper.showDailySpendSummary(title, message)
            NotificationType.MONTHLY_REPORT -> helper.showMonthlyReport(title, message)
            NotificationType.TARIFF_ALERT -> helper.showTariffSaverTip(title, message)
            NotificationType.MERCHANT_INSIGHT -> helper.showFrequentMerchantAlert(title, message)
            NotificationType.GOAL_REMINDER -> helper.showGoalReminder(title, message)
            NotificationType.SYSTEM -> helper.showInsightAlert(title, message, actionRoute)
        }
    }
}
