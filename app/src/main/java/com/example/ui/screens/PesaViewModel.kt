package com.pesalytics.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pesalytics.data.PesaRepository
import com.pesalytics.model.Bill
import com.pesalytics.model.Transaction
import com.pesalytics.model.TransactionType
import com.pesalytics.model.BillCycle
import com.pesalytics.model.Budget
import com.pesalytics.model.Goal
import com.pesalytics.model.GoalType
import com.pesalytics.model.ThemeMode
import androidx.compose.ui.geometry.Offset
import com.pesalytics.patterns.PatternEngine
import com.pesalytics.patterns.PatternResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import com.pesalytics.data.billing.SubscriptionManager
import com.pesalytics.data.billing.PromoResult
import androidx.compose.runtime.Immutable

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val isBalanceVisible: Boolean = true,
    val currentBudgetLimit: Double = 0.0,
    val hasBudget: Boolean = false,
    val budgets: List<com.pesalytics.model.Budget> = emptyList(),
    val categorySpent: Map<String, Double> = emptyMap()
)

data class BudgetInsights(
    val bestMonthLabel: String? = null,
    val bestMonthSaved: Double = 0.0,
    val avgSaved: Double = 0.0,
    val hasData: Boolean = false
)

class PesaViewModel(
    private val repository: PesaRepository,
    private val notificationHelper: com.pesalytics.notifications.NotificationHelper? = null,
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
    // Default hidden — user must explicitly reveal. Auto-hides 5 s after reveal.
    val isBalanceVisible = MutableStateFlow(false)
    private var balanceHideJob: Job? = null
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
    val subscriptionStateFlow: StateFlow<com.pesalytics.data.billing.SubscriptionState> =
        subscriptionManager?.state ?: MutableStateFlow(com.pesalytics.data.billing.SubscriptionState())
    val trialDaysRemaining get() = subscriptionManager?.state?.value?.trialDaysRemaining ?: 0

    private val _promoMessage = MutableStateFlow<String?>(null)
    val promoMessage = _promoMessage.asStateFlow()

    // ── Notification preferences ─────────────────────────────────────────────
    val billAlertsEnabled = MutableStateFlow(true)
    val budgetAlertsEnabled = MutableStateFlow(true)
    val goalRemindersEnabled = MutableStateFlow(false)
    val highSpendingAlertsEnabled = MutableStateFlow(true)
    val smartAlertsEnabled = MutableStateFlow(false)

    // ── SMS sync progress ────────────────────────────────────────────────────
    val isSyncing = MutableStateFlow(false)
    val syncProgress = MutableStateFlow(0)
    // Total M-PESA SMS count — only populated during the first-ever sync for the % display.
    // Zero means indeterminate (subsequent syncs show the thin indeterminate bar instead).
    val syncTotalMessages = MutableStateFlow(0)
    // True only during the very first sync so the UI knows to show the percentage label.
    val isFirstSync = MutableStateFlow(true)

    // ── Pattern analysis ─────────────────────────────────────────────────────
    private val _patternResult = MutableStateFlow<PatternResult?>(null)
    val patternResult = _patternResult.asStateFlow()
    private val patternEngine = PatternEngine()

    init {
        // Refresh patterns on every transaction change. Using collect (not first{}) so
        // patterns stay live after the initial load. No return@collect — that would only
        // skip the current emission, not stop the flow.
        viewModelScope.launch {
            repository.allTransactions.collect { txns ->
                if (txns.isNotEmpty()) refreshPatterns()
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
        isFirstSync.value = !prefs.getBoolean("has_synced_before", false)
        loadThemeMode(context)
        loadNotificationPrefs(context)
        loadNeedsWants(context)
        checkUpcomingBills()
        drainWorkerNotifications(prefs)
    }

    // Workers (DailySpendWorker, WeeklyReportWorker, MonthlyReportWorker) write in-app
    // notification messages to "pending_in_app_notifs" in SharedPreferences because they
    // can't reach this ViewModel directly. We drain and clear that queue on app launch.
    private fun drainWorkerNotifications(prefs: android.content.SharedPreferences) {
        val raw = prefs.getString("pending_in_app_notifs", "") ?: ""
        if (raw.isBlank()) return
        val incoming = raw.split("\n")
            .filter { it.isNotBlank() }
            .map { AppNotification(message = it) }
        _notifications.value = incoming + _notifications.value
        prefs.edit().remove("pending_in_app_notifs").apply()
    }

    fun upgradeToPremium() {
        subscriptionManager?.grantFromPromo(com.pesalytics.data.billing.PromoGrant.Lifetime)
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

    // ── Needs vs Wants classification ─────────────────────────────────────────
    // category -> true (Need) / false (Want). Unclassified categories fall back to
    // DEFAULT_NEED_KEYWORDS so existing users see sensible defaults until they customise.
    val needsWantsClassification = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    val knownCategories: StateFlow<List<String>> = repository.allTransactions.map { txns ->
        val defaults = listOf(
            "Groceries", "Transport", "Utilities", "Rent", "Bills", "Health",
            "Entertainment", "Airtime", "Shopping", "Food", "Savings", "Other"
        )
        (txns.map { it.category }.filter { it.isNotBlank() } + defaults).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadNeedsWants(context: android.content.Context) {
        val prefs = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        val needs = prefs.getStringSet("nw_needs", emptySet()) ?: emptySet()
        val wants = prefs.getStringSet("nw_wants", emptySet()) ?: emptySet()
        needsWantsClassification.value =
            needs.associateWith { true } + wants.associateWith { false }
    }

    fun setCategoryClassification(category: String, isNeed: Boolean, context: android.content.Context) {
        val updated = needsWantsClassification.value.toMutableMap()
        updated[category] = isNeed
        needsWantsClassification.value = updated
        val prefs = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("nw_needs", updated.filterValues { it }.keys)
            .putStringSet("nw_wants", updated.filterValues { !it }.keys)
            .apply()
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
                _patternResult.value = patternEngine.compute(transactions)
            }
        }
    }

    // ── SMS sync ─────────────────────────────────────────────────────────────
    fun syncMpesaSms(context: android.content.Context) {
        // Guard against overlapping syncs. Set the flag synchronously on Main before launching
        // so two rapid calls can't both pass the check.
        if (isSyncing.value) return
        isSyncing.value = true
        syncProgress.value = 0
        syncTotalMessages.value = 0

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
            val hasSyncedBefore = prefs.getBoolean("has_synced_before", false)
            val lastSyncTimestamp = prefs.getLong("last_sync_timestamp_ms", 0L)
            isFirstSync.value = !hasSyncedBefore

            val transactionsList = mutableListOf<Transaction>()
            try {
                val existingTransactions = repository.allTransactions.first()
                val existingRefs = existingTransactions.map { "${it.remoteRef}_${it.isFeeTransaction}" }.toSet()
                val customRules = repository.allCustomRules.first()

                // First sync: count all M-PESA messages upfront so the UI can show a real %.
                // Subsequent syncs: skip the count query — the bar stays indeterminate.
                if (!hasSyncedBefore) {
                    val countCursor = context.contentResolver.query(
                        android.net.Uri.parse("content://sms/inbox"),
                        arrayOf("_id"), "address = ?", arrayOf("MPESA"), null
                    )
                    syncTotalMessages.value = countCursor?.count ?: 0
                    countCursor?.close()
                }

                // Subsequent syncs query ONLY messages newer than the last processed timestamp,
                // cutting the cursor to a handful of rows instead of the entire inbox.
                val selection: String
                val selectionArgs: Array<String>
                if (hasSyncedBefore && lastSyncTimestamp > 0L) {
                    selection = "address = ? AND date > ?"
                    selectionArgs = arrayOf("MPESA", lastSyncTimestamp.toString())
                } else {
                    selection = "address = ?"
                    selectionArgs = arrayOf("MPESA")
                }

                val cursor = context.contentResolver.query(
                    android.net.Uri.parse("content://sms/inbox"),
                    null,
                    selection,
                    selectionArgs,
                    "date DESC"
                )

                var latestTimestamp = lastSyncTimestamp
                var processedMessageCount = 0

                if (cursor != null && cursor.moveToFirst()) {
                    val bodyIndex = cursor.getColumnIndex("body")
                    val dateIndex = cursor.getColumnIndex("date")
                    if (bodyIndex == -1 || dateIndex == -1) {
                        cursor.close()
                    } else {
                        do {
                            val body = cursor.getString(bodyIndex)
                            val timestamp = cursor.getLong(dateIndex)
                            if (timestamp > latestTimestamp) latestTimestamp = timestamp

                            val extractedTransactions = parseMpesaSms(body, timestamp, customRules)
                            for (transaction in extractedTransactions) {
                                val uniqueKey = "${transaction.remoteRef}_${transaction.isFeeTransaction}"
                                if (existingRefs.contains(uniqueKey)) continue
                                if (!transactionsList.any { "${it.remoteRef}_${it.isFeeTransaction}" == uniqueKey }) {
                                    transactionsList.add(transaction)
                                }
                            }

                            processedMessageCount++
                            // Only update the progress counter during the first sync (percentage mode).
                            if (!hasSyncedBefore) syncProgress.value = processedMessageCount

                        } while (cursor.moveToNext())
                        cursor.close()
                    }
                }

                // Persist the high-water-mark so the next sync starts from here.
                prefs.edit().apply {
                    if (!hasSyncedBefore) putBoolean("has_synced_before", true)
                    if (latestTimestamp > lastSyncTimestamp) putLong("last_sync_timestamp_ms", latestTimestamp)
                    apply()
                }
                // Update the flag so a repeat call in the same session is treated as subsequent.
                isFirstSync.value = false

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

                    _notifications.value = listOf(AppNotification(message = "Synced ${transactionsList.size} new M-PESA records.")) + _notifications.value
                    checkBudgetThresholds()
                    refreshPatterns()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "M-Pesa messages synced successfully", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Already up to date", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _notifications.value = listOf(AppNotification(message = "Failed to sync SMS.")) + _notifications.value
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed to sync messages", android.widget.Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSyncing.value = false
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

    private suspend fun parseMpesaSms(body: String, timestamp: Long, customRules: List<com.pesalytics.model.CustomRule>): List<Transaction> {
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
            },
            // Pochi la Biashara — payment to a business wallet (no account number, "sent to" phrasing)
            RuleEntry(TransactionType.POCHI, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+sent to\\s+(.+?)\\s+Pochi la Biashara\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim(), m.groupValues[6].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(7)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
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
        balanceHideJob?.cancel()
        val nowVisible = !isBalanceVisible.value
        isBalanceVisible.value = nowVisible
        if (nowVisible) {
            // Auto-hide after 5 seconds; cancelled immediately if the user hides manually
            balanceHideJob = viewModelScope.launch {
                delay(5_000L)
                isBalanceVisible.value = false
            }
        }
    }

    // ── DB-backed state ──────────────────────────────────────────────────────
    val bills = repository.allBills.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = repository.allGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Bundles the month window + aggregates from the DAO so the uiState combine has no
    // side-channel reads and always uses the correct upper bound for monthly queries.
    private data class MonthlyStats(val start: Long, val end: Long, val income: Double, val expense: Double)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val monthlyStats: kotlinx.coroutines.flow.Flow<MonthlyStats> =
        currentMonthStart.flatMapLatest { start ->
            val end = Calendar.getInstance().apply {
                timeInMillis = start; add(Calendar.MONTH, 1)
            }.timeInMillis
            combine(
                repository.getMonthlyIncome(start, end),
                repository.getMonthlyExpense(start, end)
            ) { income, expense -> MonthlyStats(start, end, income ?: 0.0, expense ?: 0.0) }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = combine(
        repository.allTransactions,
        monthlyStats,
        isBalanceVisible,
        currentMonthYearString.flatMapLatest { repository.getBudgetsForMonth(it) }
    ) { transactions, stats, isVisible, budgets ->
        val balance = transactions.maxByOrNull { it.timestamp }?.balanceAfter ?: 0.0
        val globalBudget = budgets.find { it.category == "Overall" }

        val categorySpent = transactions
            .filter {
                it.timestamp in stats.start until stats.end &&
                    it.type != TransactionType.RECEIVE_MONEY &&
                    it.type != TransactionType.MANUAL_INCOME &&
                    it.type != TransactionType.MANUAL_TRANSFER &&
                    !it.isFeeTransaction
            }
            .groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }

        HomeUiState(
            transactions = transactions,
            recentTransactions = transactions.take(10),
            monthlyIncome = stats.income,
            monthlyExpense = stats.expense,
            currentBalance = balance,
            isBalanceVisible = isVisible,
            currentBudgetLimit = globalBudget?.limitAmount ?: 0.0,
            hasBudget = globalBudget != null,
            budgets = budgets,
            categorySpent = categorySpent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    // ── Budget insights (real 6-month savings stats) ──────────────────────────
    val budgetInsights: StateFlow<BudgetInsights> = repository.allTransactions
        .map { computeBudgetInsights(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetInsights())

    private fun computeBudgetInsights(transactions: List<Transaction>): BudgetInsights {
        if (transactions.isEmpty()) return BudgetInsights()
        val labelFmt = SimpleDateFormat("MMM ''yy", Locale.getDefault())
        val savingsByMonth = mutableListOf<Pair<String, Double>>()
        for (monthsAgo in 0 until 6) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -monthsAgo)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            val end = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, 1) }.timeInMillis
            val monthTxns = transactions.filter { it.timestamp in start until end && !it.isFeeTransaction }
            if (monthTxns.isEmpty()) continue
            val income = monthTxns.filter {
                it.type == TransactionType.RECEIVE_MONEY || it.type == TransactionType.MANUAL_INCOME
            }.sumOf { it.amount }
            val expense = monthTxns.filter {
                it.type != TransactionType.RECEIVE_MONEY &&
                    it.type != TransactionType.MANUAL_INCOME &&
                    it.type != TransactionType.MANUAL_TRANSFER
            }.sumOf { it.amount }
            savingsByMonth.add(labelFmt.format(Date(start)) to (income - expense))
        }
        if (savingsByMonth.isEmpty()) return BudgetInsights()
        val best = savingsByMonth.maxByOrNull { it.second }
        val avg = savingsByMonth.map { it.second }.average()
        return BudgetInsights(
            bestMonthLabel = best?.first,
            bestMonthSaved = best?.second ?: 0.0,
            avgSaved = avg,
            hasData = true
        )
    }

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
    fun addOrUpdateBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val monthYear = currentMonthYearString.value
            if (monthYear.isNotEmpty()) {
                // Reuse the existing row's id so this is a true update, not a duplicate insert.
                val existing = repository.getBudgetsForMonth(monthYear).first()
                    .find { it.category == category }
                val budget = existing?.copy(limitAmount = limit)
                    ?: Budget(category = category, limitAmount = limit, monthYear = monthYear)
                repository.insertBudget(budget)
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
            val currentMonthEndMs = Calendar.getInstance().apply {
                timeInMillis = currentMonthStartMs; add(Calendar.MONTH, 1)
            }.timeInMillis
            val expenses = repository.getMonthlyExpense(currentMonthStartMs, currentMonthEndMs).firstOrNull() ?: 0.0

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

    // ── Data management ────────────────────────────────────────────────────────
    fun deleteAllData(context: android.content.Context) {
        viewModelScope.launch {
            repository.deleteAllData()
            clearNotifications()
            needsWantsClassification.value = emptyMap()
            context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit()
                .remove("nw_needs").remove("nw_wants").apply()
            _patternResult.value = null
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(context, "All data deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Restore purchases ──────────────────────────────────────────────────────
    fun restorePurchases() {
        val manager = subscriptionManager
        if (manager == null) {
            _promoMessage.value = "Billing unavailable"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                manager.syncPurchases()
                _promoMessage.value = "Purchases restored"
            } catch (e: Exception) {
                _promoMessage.value = "Could not restore purchases"
            }
        }
    }

    fun redeemPromoCode(code: String) {
        val manager = subscriptionManager ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val result = manager.redeemPromoCode(code)
            val msg = when (result) {
                is PromoResult.EarlybirdLifetime  -> "🎉 EARLYBIRD accepted! Lifetime Premium unlocked."
                is PromoResult.EarlybirdSunset    -> "EARLYBIRD has ended — you've been given a 14-day free trial instead."
                is PromoResult.Success            -> when (result.grant) {
                    is com.pesalytics.data.billing.PromoGrant.Lifetime    -> "🎉 Lifetime Premium unlocked!"
                    is com.pesalytics.data.billing.PromoGrant.Monthly     -> "✅ 1 month Premium granted!"
                    is com.pesalytics.data.billing.PromoGrant.Quarterly   -> "✅ 3 months Premium granted!"
                    is com.pesalytics.data.billing.PromoGrant.Yearly      -> "✅ 1 year Premium granted!"
                    is com.pesalytics.data.billing.PromoGrant.Trial14Days -> "✅ 14-day trial extended!"
                }
                is PromoResult.AlreadyRedeemed    -> "This code has already been used on this device."
                is PromoResult.Invalid            -> "Invalid promo code. Check and try again."
            }
            _promoMessage.value = msg
        }
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
    private val notificationHelper: com.pesalytics.notifications.NotificationHelper? = null,
    private val subscriptionManager: com.pesalytics.data.billing.SubscriptionManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PesaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PesaViewModel(repository, notificationHelper, subscriptionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
