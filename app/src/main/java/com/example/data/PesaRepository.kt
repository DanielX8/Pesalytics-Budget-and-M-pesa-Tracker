package com.pesalytics.data

import com.pesalytics.model.Bill
import com.pesalytics.model.Budget
import com.pesalytics.model.CustomRule
import com.pesalytics.model.Transaction
import com.pesalytics.model.Goal
import kotlinx.coroutines.flow.Flow

class PesaRepository(
    private val transactionDao: TransactionDao,
    private val billDao: BillDao,
    private val budgetDao: BudgetDao,
    private val customRuleDao: CustomRuleDao,
    private val goalDao: GoalDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allBills: Flow<List<Bill>> = billDao.getAllBills()
    val allCustomRules: Flow<List<CustomRule>> = customRuleDao.getAllRules()
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()

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

    suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budget)
    }
    
    suspend fun insertCustomRule(rule: CustomRule) {
        customRuleDao.insertRule(rule)
    }
    
    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }

    suspend fun addGoalContribution(goalId: Int, amount: Double) {
        goalDao.addGoalContribution(goalId, amount)
    }

    suspend fun deleteGoal(goalId: Int) {
        goalDao.deleteGoal(goalId)
    }

    suspend fun updateTransactionCategoryAndRetrain(transactionId: Int, payee: String, newCategory: String) {
        // 1. Update this specific transaction
        transactionDao.updateTransactionCategory(transactionId, newCategory)
        // 2. Retrain: create/update rule for future
        customRuleDao.insertRule(CustomRule(payeePattern = payee, mappedCategory = newCategory))
        // 3. Retroactively apply to other past transactions with same payee
        transactionDao.updateCategoryForPayee(payee, newCategory)
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

    /** Wipes every table — used by the "Delete All Data" action. */
    suspend fun deleteAllData() {
        transactionDao.deleteAllTransactions()
        billDao.deleteAllBills()
        budgetDao.deleteAllBudgets()
        goalDao.deleteAllGoals()
        customRuleDao.deleteAllRules()
    }
}
