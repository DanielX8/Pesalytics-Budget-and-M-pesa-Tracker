package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.Bill
import com.example.model.Budget
import com.example.model.CustomRule
import com.example.model.Transaction
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
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY targetDate ASC")
    fun getAllGoals(): Flow<List<com.example.model.Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: com.example.model.Goal)
}
