package com.pesalytics.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pesalytics.MainActivity
import com.pesalytics.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val ALERTS_CHANNEL_ID  = "alerts_channel"
        const val REPORTS_CHANNEL_ID = "reports_channel"
    }

    // NOTE: channels are created lazily (right before the first notification is posted),
    // NOT at construction time. Creating them at app launch could surface the system
    // notification-permission prompt before onboarding. Posting only happens after
    // onboarding (workers / budget checks), by which point permission has been requested.

    private fun isMasterEnabled(): Boolean =
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
            .getBoolean("notif_master_enabled", true)

    private fun areNotificationsPermitted(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun isPrefEnabled(key: String, default: Boolean = true): Boolean =
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE)
            .getBoolean("notif_$key", default)

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(ALERTS_CHANNEL_ID, "Budget & Bill Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Immediate alerts for budget thresholds and upcoming bills"
                }
            )
            manager.createNotificationChannel(
                NotificationChannel(REPORTS_CHANNEL_ID, "Periodic Reports", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Daily, weekly and monthly spending summaries"
                }
            )
        }
    }

    // ── Gated notification methods ───────────────────────────────────────────

    /** Budget threshold crossed (80 % / 100 %) */
    fun showBudgetAlert(title: String, message: String) {
        if (!isPrefEnabled("budget_alerts")) return
        showNotification(ALERTS_CHANNEL_ID, 1001, title, message, "budget_planner")
    }

    /** Bill due within 3 days */
    fun showBillAlert(title: String, message: String, notifId: Int = 1002) {
        if (!isPrefEnabled("bill_alerts")) return
        showNotification(ALERTS_CHANNEL_ID, notifId, title, message, "bills")
    }

    /** Monthly goal contribution reminder */
    fun showGoalReminder(title: String, message: String, notifId: Int = 1003) {
        if (!isPrefEnabled("goal_reminders", default = false)) return
        showNotification(ALERTS_CHANNEL_ID, notifId, title, message, "goals")
    }

    /** Daily "yesterday you spent X" summary */
    fun showDailySpendSummary(title: String, message: String) {
        if (!isPrefEnabled("daily_summary")) return
        showNotification(REPORTS_CHANNEL_ID, 1004, title, message, "all_transactions")
    }

    /** Weekly spending wrap-up */
    fun showWeeklyReport(title: String, message: String) {
        if (!isPrefEnabled("weekly_report")) return
        showNotification(REPORTS_CHANNEL_ID, 2002, title, message, "all_transactions")
    }

    /** Monthly financial summary */
    fun showMonthlyReport(title: String, message: String) {
        if (!isPrefEnabled("monthly_report")) return
        showNotification(REPORTS_CHANNEL_ID, 2003, title, message, "all_transactions")
    }

    /** Subscription or trial expiry warning (3 days / 1 day / today) */
    fun showSubscriptionExpiryAlert(isTrial: Boolean, daysLeft: Int) {
        if (!isMasterEnabled() || !areNotificationsPermitted()) return
        val label = if (isTrial) "trial" else "Premium subscription"
        val message = when {
            daysLeft <= 0 -> "Your $label expires today. Renew to keep full access."
            daysLeft == 1 -> "Your $label expires tomorrow."
            else          -> "Your $label expires in $daysLeft days."
        }
        showNotification(ALERTS_CHANNEL_ID, 1005, "Premium Expiring Soon", message, "settings")
    }

    private fun showNotification(channelId: String, id: Int, title: String, message: String, deepLinkTarget: String = "") {
        if (!isMasterEnabled() || !areNotificationsPermitted()) return
        createNotificationChannels()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (deepLinkTarget.isNotEmpty()) putExtra("navigate_to", deepLinkTarget)
        }
        val piFlags = if (deepLinkTarget.isNotEmpty())
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_IMMUTABLE
        val pi = PendingIntent.getActivity(context, id, intent, piFlags)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title).setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (channelId == ALERTS_CHANNEL_ID) NotificationCompat.PRIORITY_HIGH
                         else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pi).setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, notification)
    }
}
