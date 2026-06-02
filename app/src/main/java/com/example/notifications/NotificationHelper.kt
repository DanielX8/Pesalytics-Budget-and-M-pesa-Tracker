package com.pesasense.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pesasense.MainActivity
import com.pesasense.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val ALERTS_CHANNEL_ID = "alerts_channel"
        const val REPORTS_CHANNEL_ID = "reports_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertsChannel = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "Budget & Bill Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate alerts for budget thresholds and upcoming bills"
            }

            val reportsChannel = NotificationChannel(
                REPORTS_CHANNEL_ID,
                "Periodic Reports",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic summary reports of your expenses"
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(alertsChannel)
            notificationManager.createNotificationChannel(reportsChannel)
        }
    }

    private fun isPrefEnabled(key: String, default: Boolean = true): Boolean =
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
            .getBoolean("notif_$key", default)

    fun showBudgetAlert(title: String, message: String) {
        if (!isPrefEnabled("budget_alerts")) return
        showNotification(ALERTS_CHANNEL_ID, 1001, title, message)
    }

    fun showBillAlert(title: String, message: String) {
        if (!isPrefEnabled("bill_alerts")) return
        showNotification(ALERTS_CHANNEL_ID, 1002, title, message)
    }

    fun showGoalReminder(title: String, message: String) {
        if (!isPrefEnabled("goal_reminders", default = false)) return
        showNotification(ALERTS_CHANNEL_ID, 1003, title, message)
    }

    fun showReportNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "report")
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, REPORTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2001, builder.build())
    }

    private fun showNotification(channelId: String, notificationId: Int, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
