package com.pesalytics.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.pesalytics.model.Goal
import com.pesalytics.model.GoalTransaction
import com.pesalytics.model.GoalType
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.Long
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
public class GoalDao_Impl(
  __db: RoomDatabase,
) : GoalDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfGoal: EntityInsertAdapter<Goal>

  private val __insertAdapterOfGoalTransaction: EntityInsertAdapter<GoalTransaction>
  init {
    this.__db = __db
    this.__insertAdapterOfGoal = object : EntityInsertAdapter<Goal>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `goals` (`id`,`name`,`type`,`targetAmount`,`targetDate`,`monthlyContribution`,`color`,`savedAmount`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Goal) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, __GoalType_enumToString(entity.type))
        statement.bindDouble(4, entity.targetAmount)
        statement.bindLong(5, entity.targetDate)
        statement.bindDouble(6, entity.monthlyContribution)
        statement.bindLong(7, entity.color)
        statement.bindDouble(8, entity.savedAmount)
        statement.bindLong(9, entity.createdAt)
      }
    }
    this.__insertAdapterOfGoalTransaction = object : EntityInsertAdapter<GoalTransaction>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `goal_transactions` (`id`,`goalId`,`amount`,`timestamp`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GoalTransaction) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindLong(2, entity.goalId.toLong())
        statement.bindDouble(3, entity.amount)
        statement.bindLong(4, entity.timestamp)
      }
    }
  }

  public override suspend fun insertGoal(goal: Goal): Unit = performSuspending(__db, false, true) {
      _connection ->
    __insertAdapterOfGoal.insert(_connection, goal)
  }

  public override suspend fun insertGoalTransaction(transaction: GoalTransaction): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGoalTransaction.insert(_connection, transaction)
  }

  public override fun getAllGoals(): Flow<List<Goal>> {
    val _sql: String = "SELECT * FROM goals ORDER BY targetDate ASC"
    return createFlow(__db, false, arrayOf("goals")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTargetAmount: Int = getColumnIndexOrThrow(_stmt, "targetAmount")
        val _columnIndexOfTargetDate: Int = getColumnIndexOrThrow(_stmt, "targetDate")
        val _columnIndexOfMonthlyContribution: Int = getColumnIndexOrThrow(_stmt,
            "monthlyContribution")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfSavedAmount: Int = getColumnIndexOrThrow(_stmt, "savedAmount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<Goal> = mutableListOf()
        while (_stmt.step()) {
          val _item: Goal
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpType: GoalType
          _tmpType = __GoalType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpTargetAmount: Double
          _tmpTargetAmount = _stmt.getDouble(_columnIndexOfTargetAmount)
          val _tmpTargetDate: Long
          _tmpTargetDate = _stmt.getLong(_columnIndexOfTargetDate)
          val _tmpMonthlyContribution: Double
          _tmpMonthlyContribution = _stmt.getDouble(_columnIndexOfMonthlyContribution)
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpSavedAmount: Double
          _tmpSavedAmount = _stmt.getDouble(_columnIndexOfSavedAmount)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              Goal(_tmpId,_tmpName,_tmpType,_tmpTargetAmount,_tmpTargetDate,_tmpMonthlyContribution,_tmpColor,_tmpSavedAmount,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getGoalsOnce(): List<Goal> {
    val _sql: String = "SELECT * FROM goals"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTargetAmount: Int = getColumnIndexOrThrow(_stmt, "targetAmount")
        val _columnIndexOfTargetDate: Int = getColumnIndexOrThrow(_stmt, "targetDate")
        val _columnIndexOfMonthlyContribution: Int = getColumnIndexOrThrow(_stmt,
            "monthlyContribution")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfSavedAmount: Int = getColumnIndexOrThrow(_stmt, "savedAmount")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<Goal> = mutableListOf()
        while (_stmt.step()) {
          val _item: Goal
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpType: GoalType
          _tmpType = __GoalType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpTargetAmount: Double
          _tmpTargetAmount = _stmt.getDouble(_columnIndexOfTargetAmount)
          val _tmpTargetDate: Long
          _tmpTargetDate = _stmt.getLong(_columnIndexOfTargetDate)
          val _tmpMonthlyContribution: Double
          _tmpMonthlyContribution = _stmt.getDouble(_columnIndexOfMonthlyContribution)
          val _tmpColor: Long
          _tmpColor = _stmt.getLong(_columnIndexOfColor)
          val _tmpSavedAmount: Double
          _tmpSavedAmount = _stmt.getDouble(_columnIndexOfSavedAmount)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          _item =
              Goal(_tmpId,_tmpName,_tmpType,_tmpTargetAmount,_tmpTargetDate,_tmpMonthlyContribution,_tmpColor,_tmpSavedAmount,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getGoalTransactionsOnce(): List<GoalTransaction> {
    val _sql: String = "SELECT * FROM goal_transactions ORDER BY timestamp DESC, id DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfGoalId: Int = getColumnIndexOrThrow(_stmt, "goalId")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<GoalTransaction> = mutableListOf()
        while (_stmt.step()) {
          val _item: GoalTransaction
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpGoalId: Int
          _tmpGoalId = _stmt.getLong(_columnIndexOfGoalId).toInt()
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = GoalTransaction(_tmpId,_tmpGoalId,_tmpAmount,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTransactionsForGoal(goalId: Int): Flow<List<GoalTransaction>> {
    val _sql: String =
        "SELECT * FROM goal_transactions WHERE goalId = ? ORDER BY timestamp DESC, id DESC"
    return createFlow(__db, false, arrayOf("goal_transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, goalId.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfGoalId: Int = getColumnIndexOrThrow(_stmt, "goalId")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<GoalTransaction> = mutableListOf()
        while (_stmt.step()) {
          val _item: GoalTransaction
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpGoalId: Int
          _tmpGoalId = _stmt.getLong(_columnIndexOfGoalId).toInt()
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = GoalTransaction(_tmpId,_tmpGoalId,_tmpAmount,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun addGoalContribution(id: Int, amount: Double) {
    val _sql: String = "UPDATE goals SET savedAmount = savedAmount + ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindDouble(_argIndex, amount)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGoal(id: Int) {
    val _sql: String = "DELETE FROM goals WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllGoals() {
    val _sql: String = "DELETE FROM goals"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGoalTransaction(transactionId: Int) {
    val _sql: String = "DELETE FROM goal_transactions WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, transactionId.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __GoalType_enumToString(_value: GoalType): String = when (_value) {
    GoalType.SAVINGS -> "SAVINGS"
    GoalType.DEBT -> "DEBT"
  }

  private fun __GoalType_stringToEnum(_value: String): GoalType = when (_value) {
    "SAVINGS" -> GoalType.SAVINGS
    "DEBT" -> GoalType.DEBT
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
