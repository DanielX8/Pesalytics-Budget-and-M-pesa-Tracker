package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PesaRepository
import com.example.model.Bill
import com.example.model.Transaction
import com.example.model.TransactionType
import com.example.model.BillCycle
import com.example.model.Budget
import com.example.model.Goal
import com.example.model.GoalType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.model.ThemeMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(), // Current month transactions
    val recentTransactions: List<Transaction> = emptyList(), // Last 5 transactions overall
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val isBalanceVisible: Boolean = true,
    val currentBudgetLimit: Double = 0.0,
    val hasBudget: Boolean = false,
    val budgets: List<com.example.model.Budget> = emptyList()
)

class PesaViewModel(private val repository: PesaRepository) : ViewModel() {

    private val _selectedMonthIndex = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    val selectedMonthIndex = _selectedMonthIndex.asStateFlow()

    fun setSelectedMonth(index: Int) {
        _selectedMonthIndex.value = index
    }

    val currentMonthStart: StateFlow<Long> = _selectedMonthIndex.map { monthIndex ->
        val calendar = Calendar.getInstance()
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

    val isBalanceVisible = MutableStateFlow(true)
    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    val userName = MutableStateFlow("User")
    val isFirstLaunch = MutableStateFlow(true)

    fun setUserName(name: String, context: android.content.Context) {
        userName.value = name
        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit().putString("user_name", name).apply()
    }

    fun loadUserNameAndFirstLaunch(context: android.content.Context) {
        val prefs = context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE)
        userName.value = prefs.getString("user_name", "User") ?: "User"
        isFirstLaunch.value = prefs.getBoolean("first_launch", true)
    }

    fun completeOnboarding(name: String, context: android.content.Context) {
        setUserName(name, context)
        isFirstLaunch.value = false
        context.getSharedPreferences("pesa_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("first_launch", false).apply()
    }

    fun addNotification(message: String) {
        _notifications.value = listOf(AppNotification(message = message)) + _notifications.value
    }

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
                            if (existingRefs.contains(uniqueKey)) {
                                continue 
                            }
                            if (!transactionsList.any { "${it.remoteRef}_${it.isFeeTransaction}" == uniqueKey }) {
                                transactionsList.add(transaction)
                            }
                        }
                    } while (cursor.moveToNext() && transactionsList.size < 500 && scannedCount < 1000)
                    cursor.close()
                }

                if (transactionsList.isNotEmpty()) {
                    repository.insertTransactions(transactionsList)
                    _notifications.value = listOf(AppNotification(message = "Synced ${transactionsList.size} new MPESA records.")) + _notifications.value
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _notifications.value = listOf(AppNotification(message = "Failed to sync SMS.")) + _notifications.value
            }
        }
    }

    // Data class to hold extracted fields from any matched SMS regex
    private data class SmsFields(
        val receipt: String,
        val amount: Double,
        val payee: String,
        val balance: Double,
        val fee: Double,
        val accountRef: String?
    )

    private fun parseMpesaSms(body: String, timestamp: Long, customRules: List<com.example.model.CustomRule>): List<Transaction> {
        val results = mutableListOf<Transaction>()
        
        // Secondary Fuliza Overdraft Match
        val fulizaRegex = Regex("Fuliza M-Pesa amount is Ksh([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
        val fulizaMatch = fulizaRegex.find(body)
        val usedFulizaAmount = fulizaMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        // All regexes use positional groups (no named groups — API 24 safe)
        // Relaxed with \\s+ for whitespace and optional punctuation
        data class RuleEntry(val type: TransactionType, val regex: Regex, val extract: (MatchResult) -> SmsFields?)

        val rules = listOf(
            // BUY_GOODS: receipt, amount, payee, date, time, balance, fee?
            RuleEntry(TransactionType.BUY_GOODS, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+paid to\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim().trimEnd('.'), m.groupValues[6].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(7)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            // PAYBILL: receipt, amount, payee, account, date, time, balance, fee?
            RuleEntry(TransactionType.PAYBILL, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+(?:sent to|paid to)\\s+(.+?)\\.?\\s*(?:for account|Account Number)\\s+(.+?)\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim().trimEnd('.'), m.groupValues[7].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(8)?.replace(",","")?.toDoubleOrNull() ?: 0.0, m.groupValues[4].trim())
            },
            // SEND_MONEY: receipt, amount, payee, phone, date, time, balance, fee?
            RuleEntry(TransactionType.SEND_MONEY, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*Ksh([\\d,]+\\.\\d{2})\\s+sent to\\s+(.+?)\\s+(\\d{7,15})\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim(), m.groupValues[7].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(8)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            // RECEIVE_MONEY: receipt, amount, payee, phone, date, time, balance
            RuleEntry(TransactionType.RECEIVE_MONEY, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*You have received Ksh([\\d,]+\\.\\d{2})\\s+from\\s+(.+?)\\s+(\\d{7,15})\\s+on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[3].trim(), m.groupValues[7].replace(",","").toDoubleOrNull() ?: 0.0, 0.0, null)
            },
            // WITHDRAW: receipt, date, time, amount, payee, balance, fee?
            RuleEntry(TransactionType.WITHDRAW, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\s+Withdraw Ksh([\\d,]+\\.\\d{2})\\s+from\\s+(.+?)\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[4].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues[5].trim().trimEnd('.'), m.groupValues[6].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(7)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            // AIRTIME: receipt, amount, date, time, balance, fee?
            RuleEntry(TransactionType.AIRTIME, Regex("([A-Z0-9]+)\\s+Confirmed\\.?\\s*You bought Ksh([\\d,]+\\.\\d{2})\\s+of airtime on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[APM]{2})\\.?\\s*New M-PESA balance is Ksh([\\d,]+\\.\\d{2})\\.(?:\\s*Transaction cost,?\\s*Ksh([\\d,]+\\.\\d{2})\\.?)?", RegexOption.IGNORE_CASE)) { m ->
                SmsFields(m.groupValues[1], m.groupValues[2].replace(",","").toDoubleOrNull() ?: 0.0, "Safaricom", m.groupValues[5].replace(",","").toDoubleOrNull() ?: 0.0, m.groupValues.getOrNull(6)?.replace(",","")?.toDoubleOrNull() ?: 0.0, null)
            },
            // FULIZA DEDUCT: receipt, amount
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

        // Fallback: keyword-based parsing for non-standard SMS formats
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

            // Tier 2 Custom Checking
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

            // For Fuliza deductions, set the usedFulizaAmount to the transaction amount itself
            val effectiveFuliza = if (matchedType == TransactionType.MANUAL_EXPENSE && body.contains("Fuliza", ignoreCase = true)) {
                if (usedFulizaAmount > 0.0) usedFulizaAmount else fields.amount
            } else {
                usedFulizaAmount
            }

            val mainTransaction = Transaction(
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
            results.add(mainTransaction)

            // NO MORE separate fee transaction splitting.
            // Fees are stored on the main transaction's `fee` field and displayed inline.
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
        val feeMatch = feeRegex.find(body)
        if (feeMatch != null) {
            fee = feeMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
        }

        var balanceAfter = 0.0
        val balanceRegex = Regex("balance is Ksh([\\d,]+\\.\\d{2})", RegexOption.IGNORE_CASE)
        val balanceMatch = balanceRegex.find(body)
        if (balanceMatch != null) {
            balanceAfter = balanceMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: 0.0
        }

        var accountRef: String? = null
        val accountRegex = Regex("Account Number (.+?) on", RegexOption.IGNORE_CASE)
        val accountMatch = accountRegex.find(body)
        if (accountMatch != null) {
            accountRef = accountMatch.groupValues[1].trim()
        }

        return SmsFields(ref, amount, payee, balanceAfter, fee, accountRef)
    }

    fun dismissNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }
    
    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    fun toggleBalanceVisibility() {
        isBalanceVisible.value = !isBalanceVisible.value
    }

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
        val globalBudget = budgets.find { it.category == "GLOBAL" }
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

    fun addBill(bill: Bill) {
        viewModelScope.launch {
            repository.insertBill(bill)
        }
    }

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            repository.insertBill(bill) // Room REPLACE will update it
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
        }
    }

    fun addGoal(goal: Goal) {
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

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

    fun updateTransactionCategory(transaction: Transaction, newCategory: String) {
        viewModelScope.launch {
            repository.updateTransactionCategoryAndRetrain(transaction.id, transaction.payee, newCategory)
        }
    }
}

class PesaViewModelFactory(private val repository: PesaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PesaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PesaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
