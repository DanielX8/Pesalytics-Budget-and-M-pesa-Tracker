package com.pesalytics.data

import com.pesalytics.model.Bill
import com.pesalytics.model.Budget
import com.pesalytics.model.CustomRule
import com.pesalytics.model.Transaction
import com.pesalytics.model.Goal
import com.pesalytics.model.AppNotificationEntity
import kotlinx.coroutines.flow.Flow

import androidx.room.withTransaction

class PesaRepository(
    private val transactionDao: TransactionDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
    private val customRuleDao: CustomRuleDao,
    private val goalDao: GoalDao,
    private val db: AppDatabase
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBills: Flow<List<Bill>> = billDao.getAllBills()
    val allCustomRules: Flow<List<CustomRule>> = customRuleDao.getAllRules()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val allNotifications: Flow<List<AppNotificationEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()

    fun getMonthlyIncome(startOfMonth: Long, endOfMonth: Long) = transactionDao.getMonthlyIncome(startOfMonth, endOfMonth)
    fun getMonthlyExpense(startOfMonth: Long, endOfMonth: Long) = transactionDao.getMonthlyExpense(startOfMonth, endOfMonth)
    fun getBudgetsForMonth(monthYear: String) = budgetDao.getBudgetsForMonth(monthYear)

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun insertBill(bill: Bill) {
        billDao.insertBill(bill)
    }

    suspend fun deleteBill(bill: Bill) {
        billDao.deleteBill(bill.id)
    }
    
    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudgetByCategory(category: String, monthYear: String) {
        budgetDao.deleteBudgetByCategory(category, monthYear)
    }
    
    suspend fun insertCustomRule(rule: CustomRule) {
        customRuleDao.insertRule(rule)
    }
    
    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }

    suspend fun addGoalContribution(goalId: Int, amount: Double) {
        db.withTransaction {
            goalDao.addGoalContribution(goalId, amount)
            goalDao.insertGoalTransaction(com.pesalytics.model.GoalTransaction(
                goalId = goalId,
                amount = amount,
                timestamp = System.currentTimeMillis()
            ))
        }
    }

    fun getTransactionsForGoal(goalId: Int) = goalDao.getTransactionsForGoal(goalId)

    suspend fun deleteGoalTransaction(transactionId: Int, amountToRevert: Double, goalId: Int) {
        db.withTransaction {
            goalDao.deleteGoalTransaction(transactionId)
            goalDao.addGoalContribution(goalId, -amountToRevert)
        }
    }

    suspend fun deleteGoal(goalId: Int) {
        goalDao.deleteGoal(goalId)
    }

    suspend fun updateTransactionCategoryAndRetrain(transactionId: Int, payee: String, newCategory: String) {
        db.withTransaction {
            // 1. Update this specific transaction
            transactionDao.updateTransactionCategory(transactionId, newCategory)
            // 2. Retrain: create/update rule for future
            customRuleDao.insertRule(CustomRule(payeePattern = payee, mappedCategory = newCategory))
            // 3. Retroactively apply to other past transactions with same payee
            transactionDao.updateCategoryForPayee(payee, newCategory)
        }
    }
    
    suspend fun getTransactionByRef(ref: String): Transaction? {
        return transactionDao.getTransactionByRef(ref)
    }

    suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun enrichFulizaTransaction(ref: String, outstandingBalance: Double, dueDate: String?, accessFee: Double = 0.0) {
        transactionDao.enrichFulizaTransaction(ref, outstandingBalance, dueDate, accessFee)
    }

    suspend fun getDailyExpense(startOfDay: Long, endOfDay: Long): Double? {
        return transactionDao.getDailyExpense(startOfDay, endOfDay)
    }

    suspend fun updateBill(bill: Bill) {
        billDao.updateBill(bill)
    }

    suspend fun getCustomRulesOnce(): List<CustomRule> = customRuleDao.getCustomRulesOnce()

    suspend fun reanalyseMerchantCategories(customRules: List<CustomRule>) {
        val transactions = transactionDao.getAllTransactionsOnce()
        for (transaction in transactions) {
            if (transaction.isFeeTransaction) continue
            val newCategory = customRules.find {
                transaction.payee.contains(it.payeePattern, ignoreCase = true)
            }?.mappedCategory
                ?: MerchantCategoryEngine.categorize(transaction.payee)
                ?: continue
            if (newCategory != transaction.category) {
                transactionDao.updateTransactionCategory(transaction.id, newCategory)
            }
        }
    }

    suspend fun deleteAllData() {
        db.withTransaction {
            transactionDao.deleteAllTransactions()
            billDao.deleteAllBills()
            budgetDao.deleteAllBudgets()
            goalDao.deleteAllGoals()
            customRuleDao.deleteAllRules()
        }
    }

    suspend fun getTransactionsOnce(): List<Transaction> = transactionDao.getAllTransactionsOnce()
    suspend fun getBillsOnce(): List<Bill> = billDao.getBillsOnce()
    suspend fun getBudgetsOnce(): List<Budget> = budgetDao.getBudgetsOnce()
    suspend fun getGoalsOnce(): List<Goal> = goalDao.getGoalsOnce()
    suspend fun getGoalTransactionsOnce(): List<com.pesalytics.model.GoalTransaction> {
        return goalDao.getGoalTransactionsOnce()
    }

    // ── Notifications ──────────────────────────────────────────────────────────
    suspend fun markNotificationAsRead(id: Long) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        db.notificationDao().markAllAsRead()
    }

    suspend fun deleteNotification(id: Long) {
        db.notificationDao().deleteNotification(id)
    }

    suspend fun restoreBackup(schema: com.pesalytics.utils.BackupSchema): RestoreResult {
        db.withTransaction {
            transactionDao.insertTransactionsIgnore(schema.transactions)
            billDao.insertBills(schema.bills)
            budgetDao.insertBudgets(schema.budgets)
            goalDao.insertGoals(schema.goals)
            goalDao.insertGoalTransactions(schema.goal_transactions)
            customRuleDao.insertRules(schema.custom_rules)
        }

        return RestoreResult(
            transactionsAdded = schema.transactions.size,
            billsAdded = schema.bills.size,
            budgetsAdded = schema.budgets.size,
            goalsAdded = schema.goals.size,
            rulesAdded = schema.custom_rules.size
        )
    }
}

data class RestoreResult(
    val transactionsAdded: Int,
    val billsAdded: Int,
    val budgetsAdded: Int,
    val goalsAdded: Int,
    val rulesAdded: Int
)

    suspend fun insertNotification(notification: com.pesalytics.model.AppNotificationEntity) {
        db.notificationDao().insertNotification(notification)
    }
