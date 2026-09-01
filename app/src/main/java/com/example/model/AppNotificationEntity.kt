package com.pesalytics.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class NotificationType {
    DAILY_BRIEF, TARIFF_ALERT, BUDGET_WARNING, MERCHANT_INSIGHT, MONTHLY_REPORT, SYSTEM, GOAL_REMINDER
}

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val actionRoute: String? = null
)
