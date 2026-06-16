package com.pesalytics.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable

@Serializable
enum class TransactionType {
    PAYBILL,
    BUY_GOODS,
    SEND_MONEY,
    WITHDRAW,
    RECEIVE_MONEY,
    POCHI,
    AIRTIME,
    MANUAL_INCOME,
    MANUAL_EXPENSE,
    MANUAL_TRANSFER
}

@Immutable
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["remoteRef", "isFeeTransaction"], unique = true)]
)
@Serializable
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val remoteRef: String, // MPESA reference
    val amount: Double,
    val type: TransactionType,
    val payee: String, // Receiver or Sender
    val category: String, // Tier 2 category
    val timestamp: Long,
    val accountRef: String? = null, // Paybill account number
    val fee: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val isFeeTransaction: Boolean = false, // If this is a split off fee
    val usedFulizaAmount: Double = 0.0, // Tracks overdraft debt components locally
    val originalSms: String? = null,
    val fulizaOutstandingBalance: Double = 0.0,
    val fulizaDueDate: String? = null
)

@Entity(tableName = "custom_rules")
@Serializable
data class CustomRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val payeePattern: String,
    val mappedCategory: String
)

@Serializable
enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Serializable
enum class BillCycle {
    DAILY, WEEKLY, MONTHLY, YEARLY
}

@Entity(tableName = "bills")
@Serializable
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val payee: String,
    val cycle: BillCycle,
    val nextDueDate: Long,
    val isAutoPay: Boolean = false,
    val isPaid: Boolean = false,
    val lastPaidDate: Long? = null
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["category", "monthYear"], unique = true)]
)
@Serializable
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // Or "Overall" for the global cap
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val monthYear: String // e.g. "2026-05"
)

@Serializable
enum class GoalType {
    SAVINGS, DEBT
}

@Entity(tableName = "goals")
@Serializable
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: GoalType,
    val targetAmount: Double,
    val targetDate: Long,
    val monthlyContribution: Double,
    val color: Long = 0xFF4CAF50,
    val savedAmount: Double = 0.0
)
