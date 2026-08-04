package com.pesalytics.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.pesalytics.model.Budget
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BudgetDao_Impl(
  __db: RoomDatabase,
) : BudgetDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBudget: EntityInsertAdapter<Budget>
  init {
    this.__db = __db
    this.__insertAdapterOfBudget = object : EntityInsertAdapter<Budget>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `budgets` (`id`,`category`,`limitAmount`,`spentAmount`,`monthYear`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Budget) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.category)
        statement.bindDouble(3, entity.limitAmount)
        statement.bindDouble(4, entity.spentAmount)
        statement.bindText(5, entity.monthYear)
      }
    }
  }

  public override suspend fun insertBudget(budget: Budget): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfBudget.insert(_connection, budget)
  }

  public override fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> {
    val _sql: String = "SELECT * FROM budgets WHERE monthYear = ?"
    return createFlow(__db, false, arrayOf("budgets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, monthYear)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfLimitAmount: Int = getColumnIndexOrThrow(_stmt, "limitAmount")
        val _columnIndexOfSpentAmount: Int = getColumnIndexOrThrow(_stmt, "spentAmount")
        val _columnIndexOfMonthYear: Int = getColumnIndexOrThrow(_stmt, "monthYear")
        val _result: MutableList<Budget> = mutableListOf()
        while (_stmt.step()) {
          val _item: Budget
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpLimitAmount: Double
          _tmpLimitAmount = _stmt.getDouble(_columnIndexOfLimitAmount)
          val _tmpSpentAmount: Double
          _tmpSpentAmount = _stmt.getDouble(_columnIndexOfSpentAmount)
          val _tmpMonthYear: String
          _tmpMonthYear = _stmt.getText(_columnIndexOfMonthYear)
          _item = Budget(_tmpId,_tmpCategory,_tmpLimitAmount,_tmpSpentAmount,_tmpMonthYear)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBudgetsOnce(): List<Budget> {
    val _sql: String = "SELECT * FROM budgets"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfLimitAmount: Int = getColumnIndexOrThrow(_stmt, "limitAmount")
        val _columnIndexOfSpentAmount: Int = getColumnIndexOrThrow(_stmt, "spentAmount")
        val _columnIndexOfMonthYear: Int = getColumnIndexOrThrow(_stmt, "monthYear")
        val _result: MutableList<Budget> = mutableListOf()
        while (_stmt.step()) {
          val _item: Budget
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpLimitAmount: Double
          _tmpLimitAmount = _stmt.getDouble(_columnIndexOfLimitAmount)
          val _tmpSpentAmount: Double
          _tmpSpentAmount = _stmt.getDouble(_columnIndexOfSpentAmount)
          val _tmpMonthYear: String
          _tmpMonthYear = _stmt.getText(_columnIndexOfMonthYear)
          _item = Budget(_tmpId,_tmpCategory,_tmpLimitAmount,_tmpSpentAmount,_tmpMonthYear)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBudgetByCategory(category: String, monthYear: String) {
    val _sql: String = "DELETE FROM budgets WHERE category = ? AND monthYear = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, category)
        _argIndex = 2
        _stmt.bindText(_argIndex, monthYear)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllBudgets() {
    val _sql: String = "DELETE FROM budgets"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
