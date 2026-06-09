package com.pesasense.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pesasense.data.PesaRepository
import com.pesasense.model.Bill
import com.pesasense.model.Transaction
import com.pesasense.model.TransactionType
import com.pesasense.model.BillCycle
import com.pesasense.model.Budget
import com.pesasense.model.Goal
import com.pesasense.model.GoalType
import com.pesasense.model.ThemeMode
import androidx.compose.ui.geometry.Offset
import com.pesasense.patterns.PatternEngine
import com.pesasense.patterns.PatternResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.pesasense.data.billing.SubscriptionManager
import com.pesasense.data.billing.PromoResult

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val isBalanceVisible: Boolean = true,
    val currentBudgetLimit: Double = 0.0,
    val hasBudget: Boolean = false,
    val budgets: List<com.pesasense.model.Budget> = emptyList()
)

class PesaViewModel(
    private val repository: PesaRepository,
    private val notificationHelper: com.pesasense.notifications.NotificationHelper? = null,
    val subscriptionManager: SubscriptionManager? = null
) : ViewModel() {

    // ── Month selection ──────────────────────────────────────────────────────
    private val _selectedMonthIndex = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonthIndex = _selectedMonthIndex.asStateFlow()

    fun setSelectedMonth(index: Int) {
        _selectedMonthIndex.value = index
    }

    val currentMonthStart: StateFlow<Long> = _selectedMonthIndex.map { monthIndex ->
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        // If the selected month is ahead of today's month it belongs to the previous year
        val targetYear = if (monthIndex > currentMonth) currentYear - 1 else currentYear
        calendar.set(Calendar.YEAR, targetYear)
        calendar.set(Calendar.MONTH, monthIndex)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0L)

    val currentMonthYearString: StateFlow<String> = currentMonthStart.map { start ->
        val format = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        format.format(Date(start))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    // ── General state ────────────────────────────────────────────────────────
    val isBalanceVisible = MutableStateFlow(true)
    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val revealOrigin = MutableStateFlow(Offset.Zero)
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    val userName = MutableStateFlow("User")
    val userAvatar = MutableStateFlow(0)
    val isFirstLaunch = MutableStateFlow(true)
    val isPremium: StateFlow<Boolean> = subscriptionManager?.state
        ?.map { it.isPremium }
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, false)
        ?: MutableStateFlow(false)

    // ── Subscription state ────────────────────────────────────────────────────
    val subscriptionState = subscriptionManager?.state
    val subscriptionStateFlow: StateFlow<com.pesasense.data.billing.SubscriptionState> =
        subscriptionManager?.state ?: MutableStateFlow(com.pesasense.data.billing.SubscriptionState())
    val trialDaysRemaining get() = subscriptionManager?.state?.value?.trialDaysRemaining ?: 0

    private val _promoMessage = MutableStateFlow<String?>(null)
    val promoMessage = _promoMessage.asStateFlow()

    // ── Notification preferences ─────────────────────────────────────────────
    val billAlertsEnabled = MutableStateFlow(true)
    val budgetAlertsEnabled = MutableStateFlow(true)
    val goalRemindersEnabled = MutableStateFlow(false)
    val highSpendingAlertsEnabled = MutableStateFlow(true)
    val smartAlertsEnabled = MutableStateFlow(false)

    // ── Pattern analysis ─────────────────────────────────────────────────────
    private val _patternResult = MutableStateFlow<PatternResult?>(null)
    val patternResult = _patternResult.asStateFlow()

    init {
        // Collect transactions and refresh patterns as soon as any data arrives.
        // Using collect instead of `first { isNotEmpty() }` prevents hanging forever
        // on a fresh install with an empty database.
        viewModelScope.launch {
            repository.allTransactions.collect { txns ->
                if (txns.isNotEmpty()) {
                    refreshPatterns()
                    return@collect
                }
            }
        }
    }

    // ── Profile / prefs ──────────────────────────────────────────────────────
    fun setProfileInfo(name: String, avatarIndex: Int, context: android.content.Context) {
        userName.value = name
        userAvatar.value = avatarIndex
        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit()
            .putString("user_name", name)
            .putInt("user_avatar", avatarIndex)
            .apply()
    }

    fun setUserName(name: String, context: android.content.Context) {
        userName.value = name
        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit()
            .putString("user_name", name)
            .apply()
    }

    fun loadUserNameAndFirstLaunch(context: android.content.Context) {
        val prefs = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        userName.value = prefs.getString("user_name", "User") ?: "User"
        userAvatar.value = prefs.getInt("user_avatar", 0)
        isFirstLaunch.value = !prefs.getBoolean("has_completed_onboarding", false)
        loadThemeMode(context)
        loadNotificationPrefs(context)
        checkUpcomingBills()
    }

    fun upgradeToPremium() {
        subscriptionManager?.grantFromPromo(com.pesasense.data.billing.PromoGrant.Lifetime)
    }

    fun completeOnboarding(name: String, avatarIndex: Int, context: android.content.Context) {
        setProfileInfo(name, avatarIndex, context)
        isFirstLaunch.value = false
        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit()
            .putBoolean("has_completed_onboarding", true)
            .apply()
    }

    // ── Theme ────────────────────────────────────────────────────────────────
    fun setThemeMode(mode: ThemeMode, context: android.content.Context, origin: Offset = Offset.Zero) {
        revealOrigin.value = origin
        themeMode.value = mode
        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit()
            .putString("theme_mode", mode.name)
            .apply()
    }

    private fun loadThemeMode(context: android.content.Context) {
        val prefs = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        themeMode.value = try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    // ── Notification preferences ─────────────────────────────────────────────
    fun setNotificationPref(key: String, enabled: Boolean, context: android.content.Context) {
        when (key) {
            "bill_alerts" -> billAlertsEnabled.value = enabled
            "budget_alerts" -> budgetAlertsEnabled.value = enabled
            "goal_reminders" -> goalRemindersEnabled.value = enabled
            "high_spending" -> highSpendingAlertsEnabled.value = enabled
            "smart_alerts" -> smartAlertsEnabled.value = enabled
        }
        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit()
            .putBoolean("notif_$key", enabled)
            .apply()
    }

    private fun loadNotificationPrefs(context: android.content.Context) {
        val prefs = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        billAlertsEnabled.value = prefs.getBoolean("notif_bill_alerts", true)
        budgetAlertsEnabled.value = prefs.getBoolean("notif_budget_alerts", true)
        goalRemindersEnabled.value = prefs.getBoolean("notif_goal_reminders", false)
        highSpendingAlertsEnabled.value = prefs.getBoolean("notif_high_spending", true)
        smartAlertsEnabled.value = prefs.getBoolean("notif_smart_alerts", false)
    }

    // ── Notifications (in-app) ───────────────────────────────────────────────
    fun addNotification(message: String) {
        _notifications.value = listOf(AppNotification(message = message)) + _notifications.value
    }

    fun dismissNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    // ── Pattern refresh ──────────────────────────────────────────────────────
    fun refreshPatterns() {
        viewModelScope.launch(Dispatchers.Default) {
            val transactions = repository.allTransactions.first()
            if (transactions.isNotEmpty()) {
                _patternResult.value = PatternEngine().compute(transactions)
            }
        }
    }

    // ── SMS sync ─────────────────────────────────────────────────────────────
    fun syncMpesaSms(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val transactionsList = mutableListOf<Transaction>()
            try {
                val existingTransactions = repository.allTransactions.first()
                val existingRefs = existingTransactions.map { "${it.remoteRef}_${it.isFeeTransaction}" }.toSet()
                val customRules = repository.allCustomRules.first()

                val cursor = context.contentResolver.query(
                    android.net.Uri.parse("content://sms/inbox"),
                    null,
                    "address = ?",
                    arrayOf("MPESA"),
                    "date DESC"
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val bodyIndex = cursor.getColumnIndex("body")
                    val dateIndex = cursor.getColumnIndex("date")
                    var scannedCount = 0
                    do {
                        scannedCount++
                        val body = cursor.getString(bodyIndex)
                        val timestamp = cursor.getLong(dateIndex)
                        val extractedTransactions = parseMpesaSms(body, timestamp, customRules)

                        for (transaction in extractedTransactions) {
                            val uniqueKey = "${transaction.remoteRef}_${transaction.isFeeTransaction}"
                            if (existingRefs.contains(uniqueKey)) continue
                            if (!transactionsList.any { "${it.remoteRef}_${it.isFeeTransaction}" == uniqueKey }) {
                                transactionsList.add(transaction)
                            }
                        }
                    } while (cursor.moveToNext() && transactionsList.size < 500 && scannedCount < 1000)
                    cursor.close()
                }

                if (transactionsList.isNotEmpty()) {
                    repository.insertTransactions(transactionsList)

                    // Auto-match newly synced transactions against tracked bills
                    val currentBills = repository.allBills.first()
                    for (transaction in transactionsList) {
                        val match = currentBills.find { bill ->
                            bill.payee.isNotBlank() &&
                            !bill.isPaid &&
                            transaction.payee.contains(bill.payee, ignoreCase = true)
                        }
                        if (match != null) {
                            val now = System.currentTimeMillis()
                            val updated = if (match.isAutoPay) {
                                match.copy(isPaid = true, lastPaidDate = now, nextDueDate = calculateNextDueDate(match.cycle, match.nextDueDate))
                            } else {
                                match.copy(isPaid = true, lastPaidDate = now)
                            }
                            repository.updateBill(updated)
                        }
                    }

                    _notifications.value = listOf(AppNotification(message = "Synced ${transactionsList.size} new MPESA records.")) + _notifications.value
                    checkBudgetThresholds()
                    refreshPatterns()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "M-Pesa messages synced successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "M-Pesa messages synced successfully (no new messages)", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _notifications.value = listOf(AppNotification(message = "Failed to sync SMS.")) + _notifications.value
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed to sync messages", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private data class SmsFields(
        val receipt: String,
        val amount: Double,
        val payee: String,
        val balance: Double,
        val fee: Double,
        val accountRef: String?
    )

    private suspend fun parseMpesaSms(body: String, timestamp: Long, customRules: List<com.pesasense.model.CustomRule>): List<Transaction> {
        val results = mutableListOf<Transaction>()

        // Two-pass Fuliza enrichment: detect outstanding-balance SMS and enrich the original transaction
        if (body.contains("Total Fuliza M-PESA outstanding amount is", ignoreCase = true)) {
            val outstandingRegex = Regex(
                """([A-Z0-9]+).*?Total Fuliza M-PESA outstanding amount is Ksh([\d,]+\.\d{2}).*?Due on (\d{1,2}/\d{1,2}/\d{4})""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            outstandingRegex.find(body)?.let { match ->
                val ref = match.groupValues[1]
                val outstandingBalance = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
                val dueDate = match.groupValues[3]
                repository.enrichFulizaTransaction(ref, outstandingBalance, dueDate)
            }
            return results // empty — don't create a duplicate transaction
        }

        // Secondary Fuliza Overdraft Match
        val fulizaRegex = Regex("Fuliza M-Pesa amount is Ksh([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
        val fulizaMatch = fulizaRegex.find(body)
        val usedFulizaAmount = fulizaMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        data class RuleEntry(val type: TransactionType, val regex: Regex, val extract: (MatchResult) -> SmsFields?)

        val rules = listOf(
            RuleEntry(TransactionType.BUY_GOODS, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+paid to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim().trimEnd('.'), m.groupValues[6].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(7)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            RuleEntry(TransactionType.PAYBILL, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+(?:sent to|paid to)\\s+(.+?)\\.?\\s*(?:for account|Account Number)\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim().trimEnd('.'), m.groupValues[7].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(8)?.replace(",","")?.toDoubleOrNull() ?: 0.0, m.groupValues[4].trim())
            },
            RuleEntry(TransactionType.SEND_MONEY, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+sent to\\s+(.+?)\\s+(\\d{7,15})\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim(), m.groupValues[7].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(8)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            RuleEntry(TransactionType.RECEIVE_MONEY, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*You have received Ksh([\\d,]+\\.\\d{2})\\s+from\\s+(.+?)\\s+(\\d{7,15})\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim(), m.groupValues[7].replace(",","").toDoubleOrNull() ?: 0.0, 0.0, null)
            },
            RuleEntry(TransactionType.WITHDRAW, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\s+Withdraw Ksh([\\d,]+\\.\\d{2})\\s+from\\s+(.+?)\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[4].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[5].trim().trimEnd('.'), m.groupValues[6].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(7)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            RuleEntry(TransactionType.AIRTIME, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*You bought Ksh([\\d,]+\\.\\d{2})\\s+of airtime on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, "Safaricom", m.groupValues[5].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(6)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            RuleEntry(TransactionType.MANUAL_EXPENSE, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+deducted.*?settle your Fuliza", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, "Fuliza", 0.0, 0.0, null)
            }
        )

        var fields: SmsFields? = null
        var matchedType: TransactionType? = null

        for (rule in rules) {
            val match = rule.regex.find(body)
            if (match != null) {
                fields = rule.extract(match)
                matchedType = rule.type
                break
            }
        }

        if (fields == null) {
            fields = fallbackParseSms(body, timestamp)
            if (fields != null) {
                matchedType = when {
                    body.contains("bought airtime", ignoreCase = true) -> TransactionType.AIRTIME
                    body.contains("Fuliza", ignoreCase = true) -> TransactionType.MANUAL_EXPENSE
                    body.contains("Withdraw", ignoreCase = true) -> TransactionType.WITHDRAW
                    body.contains("sent to", ignoreCase = true) -> TransactionType.SEND_MONEY
                    body.contains("received", ignoreCase = true) -> TransactionType.RECEIVE_MONEY
                    body.contains("paid to", ignoreCase = true) && body.contains("Account Number", ignoreCase = true) -> TransactionType.PAYBILL
                    body.contains("paid to", ignoreCase = true) -> TransactionType.BUY_GOODS
                    else -> TransactionType.MANUAL_EXPENSE
                }
            }
        }

        if (fields != null && matchedType != null && fields.amount > 0.0) {
            val payeeRaw = fields.payee.replace(Regex("\\s+\\d{4,}$"), "").trim()

            var category = customRules.find { payeeRaw.contains(it.payeePattern, ignoreCase = true) }?.mappedCategory
            if (category == null) {
                category = when (matchedType) {
                    TransactionType.BUY_GOODS -> "Shopping"
                    TransactionType.PAYBILL -> "Bills"
                    TransactionType.SEND_MONEY -> "Transfer"
                    TransactionType.RECEIVE_MONEY -> "Income"
                    TransactionType.WITHDRAW -> "Cash"
                    TransactionType.AIRTIME -> "Airtime"
                    TransactionType.MANUAL_EXPENSE -> if (body.contains("Fuliza", ignoreCase = true)) "Fuliza" else "Other"
                    else -> "Other"
                }
            }

            val effectiveFuliza = if (matchedType == TransactionType.MANUAL_EXPENSE && body.contains("Fuliza", ignoreCase = true)) {
                if (usedFulizaAmount > 0.0) usedFulizaAmount else fields.amount
            } else {
                usedFulizaAmount
            }

            results.add(
                Transaction(
                    amount = fields.amount,
                    payee = payeeRaw,
                    timestamp = timestamp,
                    type = matchedType,
                    remoteRef = fields.receipt,
                    category = category,
                    fee = fields.fee,
                    balanceAfter = fields.balance,
                    accountRef = fields.accountRef,
                    isFeeTransaction = false,
                    usedFulizaAmount = effectiveFuliza,
                    originalSms = body
                )
            )
        }

        return results
    }

    private fun fallbackParseSms(body: String, timestamp: Long): SmsFields? {
        val refRegex = Regex("^[A-Z0-9]+")
        val ref = refRegex.find(body)?.value ?: "SMS_$timestamp"

        val kshRegex = Regex("Ksh([\\d,]+\\.\\d{2})")
        val amountMatch = kshRegex.find(body)
        val amount = amountMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: return null

        var payee = "M-PESA"
        if (body.contains("sent to", ignoreCase = true)) {
            val toRegex = Regex("sent to (.*?)(?= on| for|\\.|$)")
            payee = toRegex.find(body)?.groupValues?.getOrNull(1)?.trim() ?: payee
        } else if (body.contains("paid to", ignoreCase = true)) {
            val toRegex = Regex("paid to (.*?)(?= on| for|\\.|$)")
            payee = toRegex.find(body)?.groupValues?.getOrNull(1)?.trim() ?: payee
        } else if (body.contains("from", ignoreCase = true)) {
            val fromRegex = Regex("from (.*?)(?= on| for|\\.|$)")
            payee = fromRegex.find(body)?.groupValues?.getOrNull(1)?.trim() ?: payee
        }

        var fee = 0.0
        val feeRegex = Regex("Transaction cost[s]?[^\\d]*([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
        feeRegex.find(body)?.let { fee = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0 }

        var balanceAfter = 0.0
        val balanceRegex = Regex("balance is Ksh([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
        balanceRegex.find(body)?.let { balanceAfter = it.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0 }

        var accountRef: String? = null
        val accountRegex = Regex("Account Number (.+?) on", RegexOption.IGNORE_CASE)
        accountRegex.find(body)?.let { accountRef = it.groupValues[1].trim() }

        return SmsFields(ref, amount, payee, balanceAfter, fee, accountRef)
    }

    // ── Balance visibility ───────────────────────────────────────────────────
    fun toggleBalanceVisibility() {
        isBalanceVisible.value = !isBalanceVisible.value
    }

    // ── DB-backed state ──────────────────────────────────────────────────────
    val bills = repository.allBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val goals = repository.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        repository.allTransactions,
        currentMonthStart.flatMapLatest { repository.getMonthlyIncome(it) },
        currentMonthStart.flatMapLatest { repository.getMonthlyExpense(it) },
        isBalanceVisible,
        currentMonthYearString.flatMapLatest { repository.getBudgetsForMonth(it) }
    ) { transactions, income, expense, isVisible, budgets ->
        val balance = transactions.maxByOrNull { it.timestamp }?.balanceAfter ?: 0.0
        val globalBudget = budgets.find { it.category == "Overall" }
        HomeUiState(
            transactions = transactions,
            recentTransactions = transactions.take(10),
            monthlyIncome = income ?: 0.0,
            monthlyExpense = expense ?: 0.0,
            currentBalance = balance,
            isBalanceVisible = isVisible,
            currentBudgetLimit = globalBudget?.limitAmount ?: 0.0,
            hasBudget = globalBudget != null,
            budgets = budgets
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // ── Bills ────────────────────────────────────────────────────────────────
    fun addBill(bill: Bill) {
        viewModelScope.launch { repository.insertBill(bill) }
    }

    fun updateBill(bill: Bill) {
        // Use updateBill (not insertBill) to preserve the existing row ID.
        // insertBill with REPLACE strategy would create a new row with a new ID,
        // silently resetting lastPaidDate and breaking any future ID-based lookups.
        viewModelScope.launch { repository.updateBill(bill) }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch { repository.deleteBill(bill) }
    }

    fun markBillAsPaid(bill: Bill) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = if (bill.isAutoPay) {
                bill.copy(isPaid = true, lastPaidDate = now, nextDueDate = calculateNextDueDate(bill.cycle, bill.nextDueDate))
            } else {
                bill.copy(isPaid = true, lastPaidDate = now)
            }
            repository.updateBill(updated)
        }
    }

    private fun calculateNextDueDate(cycle: BillCycle, currentDueDate: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDueDate }
        when (cycle) {
            BillCycle.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            BillCycle.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BillCycle.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BillCycle.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    // ── Goals ────────────────────────────────────────────────────────────────
    fun addGoal(goal: Goal) {
        viewModelScope.launch { repository.insertGoal(goal) }
    }

    fun checkUpcomingBills() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val threeDaysMs = 3L * 24 * 60 * 60 * 1000
            val dueSoon = repository.allBills.first()
                .filter { !it.isPaid && it.nextDueDate in now..(now + threeDaysMs) }
            dueSoon.forEach { bill ->
                val daysUntil = ((bill.nextDueDate - now) / (1000 * 60 * 60 * 24)).toInt()
                val timeLabel = when (daysUntil) {
                    0 -> "TODAY"
                    1 -> "TOMORROW"
                    else -> "in $daysUntil days"
                }
                val msg = "${bill.name} is due $timeLabel — KES ${"%.2f".format(bill.amount)}"
                addNotification(msg)
                notificationHelper?.showBillAlert("Upcoming Bill", msg)
            }
        }
    }

    fun addGoalContribution(goalId: Int, amount: Double) {
        viewModelScope.launch { repository.addGoalContribution(goalId, amount) }
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch { repository.deleteGoal(goalId) }
    }

    // ── Transactions ─────────────────────────────────────────────────────────
    fun deleteTransaction(id: Int) {
        viewModelScope.launch { repository.deleteTransaction(id) }
    }

    fun insertManualTransaction(amount: Double, payee: String, isIncome: Boolean) {
        viewModelScope.launch {
            val type = if (isIncome) TransactionType.MANUAL_INCOME else TransactionType.MANUAL_EXPENSE
            val category = if (isIncome) "Income" else "Other"
            repository.insertTransaction(
                Transaction(
                    remoteRef = "MANUAL_${System.currentTimeMillis()}",
                    amount = amount,
                    type = type,
                    payee = payee.trim(),
                    category = category,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateTransactionCategory(transaction: Transaction, newCategory: String) {
        viewModelScope.launch {
            repository.updateTransactionCategoryAndRetrain(transaction.id, transaction.payee, newCategory)
        }
    }

    // ── Budgets ──────────────────────────────────────────────────────────────
    fun setBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val monthYear = currentMonthYearString.value
            if (monthYear.isNotEmpty()) {
                repository.insertBudget(Budget(category = category, limitAmount = limit, monthYear = monthYear))
            }
        }
    }

    fun addOrUpdateBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val monthYear = currentMonthYearString.value
            if (monthYear.isNotEmpty()) {
                repository.insertBudget(Budget(category = category, limitAmount = limit, monthYear = monthYear))
            }
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            val monthYear = currentMonthYearString.value
            if (monthYear.isNotEmpty()) {
                repository.deleteBudget(Budget(category = category, limitAmount = 0.0, monthYear = monthYear))
            }
        }
    }

    // ── Budget threshold check ───────────────────────────────────────────────
    private fun checkBudgetThresholds() {
        if (notificationHelper == null) return
        viewModelScope.launch {
            val monthYear = currentMonthYearString.value
            val currentMonthStartMs = currentMonthStart.value
            val budgets = repository.getBudgetsForMonth(monthYear).firstOrNull() ?: emptyList()
            val expenses = repository.getMonthlyExpense(currentMonthStartMs).firstOrNull() ?: 0.0

            val globalBudget = budgets.find { it.category == "Overall" }
            if (globalBudget != null && globalBudget.limitAmount > 0) {
                val percentage = expenses / globalBudget.limitAmount
                when {
                    percentage >= 1.0 -> {
                        val msg = "You have exceeded your monthly budget!"
                        notificationHelper.showBudgetAlert("Budget Exceeded", msg)
                        addNotification(msg)
                    }
                    percentage >= 0.8 -> {
                        val msg = "You've used ${"%.0f".format(percentage * 100)}% of your monthly budget."
                        notificationHelper.showBudgetAlert("Budget Warning", msg)
                        addNotification(msg)
                    }
                }
            }
        }
    }

    fun redeemPromoCode(code: String) {
        val manager = subscriptionManager ?: return
        val result = manager.redeemPromoCode(code)
        val msg = when (result) {
            is PromoResult.EarlybirdLifetime  -> "🎉 EARLYBIRD accepted! Lifetime Premium unlocked."
            is PromoResult.EarlybirdSunset    -> "EARLYBIRD has ended — you've been given a 14-day free trial instead."
            is PromoResult.Success            -> when (result.grant) {
                is com.pesasense.data.billing.PromoGrant.Lifetime   -> "🎉 Lifetime Premium unlocked!"
                is com.pesasense.data.billing.PromoGrant.Monthly    -> "✅ 1 month Premium granted!"
                is com.pesasense.data.billing.PromoGrant.Quarterly  -> "✅ 3 months Premium granted!"
                is com.pesasense.data.billing.PromoGrant.Yearly     -> "✅ 1 year Premium granted!"
                is com.pesasense.data.billing.PromoGrant.Trial14Days -> "✅ 14-day trial extended!"
            }
            is PromoResult.AlreadyRedeemed    -> "This code has already been used on this device."
            is PromoResult.Invalid            -> "Invalid promo code. Check and try again."
            else                              -> "Unknown promo result."
        }
        _promoMessage.value = msg
    }

    fun clearPromoMessage() {
        _promoMessage.value = null
    }

    fun startTrial() {
        subscriptionManager?.startTrialIfNotStarted()
    }
}

class PesaViewModelFactory(
    private val repository: PesaRepository,
    private val notificationHelper: com.pesasense.notifications.NotificationHelper? = null,
    private val subscriptionManager: com.pesasense.data.billing.SubscriptionManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PesaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PesaViewModel(repository, notificationHelper, subscriptionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
