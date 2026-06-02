package com.pesasense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import com.pesasense.model.Bill
import com.pesasense.model.Budget
import com.pesasense.model.CustomRule
import com.pesasense.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Query("SELECT * FROM transactions WHERE remoteRef = :ref LIMIT 1")
    suspend fun getTransactionByRef(ref: String): Transaction?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Int)

    @Query("SELECT SUM(amount) FROM transactions WHERE type IN ('RECEIVE_MONEY', 'MANUAL_INCOME') AND timestamp >= :startOfMonth")
    fun getMonthlyIncome(startOfMonth: Long): Flow<Double?>
    
    @Query("SELECT SUM(amount) FROM transactions WHERE type NOT IN ('RECEIVE_MONEY', 'MANUAL_INCOME', 'MANUAL_TRANSFER') AND timestamp >= :startOfMonth")
    fun getMonthlyExpense(startOfMonth: Long): Flow<Double?>

    @Query("UPDATE transactions SET category = :newCategory WHERE id = :id")
    suspend fun updateTransactionCategory(id: Int, newCategory: String)

    @Query("UPDATE transactions SET category = :newCategory WHERE LOWER(payee) = LOWER(:payee)")
    suspend fun updateCategoryForPayee(payee: String, newCategory: String)

    @Query("UPDATE transactions SET fulizaOutstandingBalance = :outstandingBalance, fulizaDueDate = :dueDate WHERE remoteRef = :ref")
    suspend fun enrichFulizaTransaction(ref: String, outstandingBalance: Double, dueDate: String?)

    @Query("SELECT SUM(amount) FROM transactions WHERE type NOT IN ('RECEIVE_MONEY', 'MANUAL_INCOME', 'MANUAL_TRANSFER') AND timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun getDailyExpense(startOfDay: Long, endOfDay: Long): Double?
}

@Dao
interface CustomRuleDao {
    @Query("SELECT * FROM custom_rules")
    fun getAllRules(): Flow<List<CustomRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CustomRule)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY nextDueDate ASC")
    fun getAllBills(): Flow<List<Bill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBill(id: Int)

    @Update
    suspend fun updateBill(bill: Bill)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY targetDate ASC")
    fun getAllGoals(): Flow<List<com.pesasense.model.Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: com.pesasense.model.Goal)

    @Query("UPDATE goals SET savedAmount = savedAmount + :amount WHERE id = :id")
    suspend fun addGoalContribution(id: Int, amount: Double)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Int)
}
