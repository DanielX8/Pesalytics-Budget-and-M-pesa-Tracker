package com.pesalytics.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.pesalytics.model.Transaction
import com.pesalytics.model.TransactionType
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class TransactionDao_Impl(
  __db: RoomDatabase,
) : TransactionDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTransaction: EntityInsertAdapter<Transaction>
  init {
    this.__db = __db
    this.__insertAdapterOfTransaction = object : EntityInsertAdapter<Transaction>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `transactions` (`id`,`remoteRef`,`amount`,`type`,`payee`,`category`,`timestamp`,`accountRef`,`fee`,`balanceAfter`,`isFeeTransaction`,`usedFulizaAmount`,`originalSms`,`fulizaOutstandingBalance`,`fulizaDueDate`,`mshwariBalanceAfter`,`pochiBalanceAfter`,`fulizaLimitAfter`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Transaction) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.remoteRef)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, __TransactionType_enumToString(entity.type))
        statement.bindText(5, entity.payee)
        statement.bindText(6, entity.category)
        statement.bindLong(7, entity.timestamp)
        val _tmpAccountRef: String? = entity.accountRef
        if (_tmpAccountRef == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpAccountRef)
        }
        statement.bindDouble(9, entity.fee)
        statement.bindDouble(10, entity.balanceAfter)
        val _tmp: Int = if (entity.isFeeTransaction) 1 else 0
        statement.bindLong(11, _tmp.toLong())
        statement.bindDouble(12, entity.usedFulizaAmount)
        val _tmpOriginalSms: String? = entity.originalSms
        if (_tmpOriginalSms == null) {
          statement.bindNull(13)
        } else {
          statement.bindText(13, _tmpOriginalSms)
        }
        statement.bindDouble(14, entity.fulizaOutstandingBalance)
        val _tmpFulizaDueDate: String? = entity.fulizaDueDate
        if (_tmpFulizaDueDate == null) {
          statement.bindNull(15)
        } else {
          statement.bindText(15, _tmpFulizaDueDate)
        }
        statement.bindDouble(16, entity.mshwariBalanceAfter)
        statement.bindDouble(17, entity.pochiBalanceAfter)
        statement.bindDouble(18, entity.fulizaLimitAfter)
      }
    }
  }

  public override suspend fun insertTransaction(transaction: Transaction): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransaction.insert(_connection, transaction)
  }

  public override suspend fun insertTransactions(transactions: List<Transaction>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransaction.insert(_connection, transactions)
  }

  public override fun getAllTransactions(): Flow<List<Transaction>> {
    val _sql: String = "SELECT * FROM transactions ORDER BY timestamp DESC, id DESC"
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRemoteRef: Int = getColumnIndexOrThrow(_stmt, "remoteRef")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfPayee: Int = getColumnIndexOrThrow(_stmt, "payee")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfAccountRef: Int = getColumnIndexOrThrow(_stmt, "accountRef")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfBalanceAfter: Int = getColumnIndexOrThrow(_stmt, "balanceAfter")
        val _columnIndexOfIsFeeTransaction: Int = getColumnIndexOrThrow(_stmt, "isFeeTransaction")
        val _columnIndexOfUsedFulizaAmount: Int = getColumnIndexOrThrow(_stmt, "usedFulizaAmount")
        val _columnIndexOfOriginalSms: Int = getColumnIndexOrThrow(_stmt, "originalSms")
        val _columnIndexOfFulizaOutstandingBalance: Int = getColumnIndexOrThrow(_stmt,
            "fulizaOutstandingBalance")
        val _columnIndexOfFulizaDueDate: Int = getColumnIndexOrThrow(_stmt, "fulizaDueDate")
        val _columnIndexOfMshwariBalanceAfter: Int = getColumnIndexOrThrow(_stmt,
            "mshwariBalanceAfter")
        val _columnIndexOfPochiBalanceAfter: Int = getColumnIndexOrThrow(_stmt, "pochiBalanceAfter")
        val _columnIndexOfFulizaLimitAfter: Int = getColumnIndexOrThrow(_stmt, "fulizaLimitAfter")
        val _result: MutableList<Transaction> = mutableListOf()
        while (_stmt.step()) {
          val _item: Transaction
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpRemoteRef: String
          _tmpRemoteRef = _stmt.getText(_columnIndexOfRemoteRef)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpPayee: String
          _tmpPayee = _stmt.getText(_columnIndexOfPayee)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpAccountRef: String?
          if (_stmt.isNull(_columnIndexOfAccountRef)) {
            _tmpAccountRef = null
          } else {
            _tmpAccountRef = _stmt.getText(_columnIndexOfAccountRef)
          }
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpBalanceAfter: Double
          _tmpBalanceAfter = _stmt.getDouble(_columnIndexOfBalanceAfter)
          val _tmpIsFeeTransaction: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFeeTransaction).toInt()
          _tmpIsFeeTransaction = _tmp != 0
          val _tmpUsedFulizaAmount: Double
          _tmpUsedFulizaAmount = _stmt.getDouble(_columnIndexOfUsedFulizaAmount)
          val _tmpOriginalSms: String?
          if (_stmt.isNull(_columnIndexOfOriginalSms)) {
            _tmpOriginalSms = null
          } else {
            _tmpOriginalSms = _stmt.getText(_columnIndexOfOriginalSms)
          }
          val _tmpFulizaOutstandingBalance: Double
          _tmpFulizaOutstandingBalance = _stmt.getDouble(_columnIndexOfFulizaOutstandingBalance)
          val _tmpFulizaDueDate: String?
          if (_stmt.isNull(_columnIndexOfFulizaDueDate)) {
            _tmpFulizaDueDate = null
          } else {
            _tmpFulizaDueDate = _stmt.getText(_columnIndexOfFulizaDueDate)
          }
          val _tmpMshwariBalanceAfter: Double
          _tmpMshwariBalanceAfter = _stmt.getDouble(_columnIndexOfMshwariBalanceAfter)
          val _tmpPochiBalanceAfter: Double
          _tmpPochiBalanceAfter = _stmt.getDouble(_columnIndexOfPochiBalanceAfter)
          val _tmpFulizaLimitAfter: Double
          _tmpFulizaLimitAfter = _stmt.getDouble(_columnIndexOfFulizaLimitAfter)
          _item =
              Transaction(_tmpId,_tmpRemoteRef,_tmpAmount,_tmpType,_tmpPayee,_tmpCategory,_tmpTimestamp,_tmpAccountRef,_tmpFee,_tmpBalanceAfter,_tmpIsFeeTransaction,_tmpUsedFulizaAmount,_tmpOriginalSms,_tmpFulizaOutstandingBalance,_tmpFulizaDueDate,_tmpMshwariBalanceAfter,_tmpPochiBalanceAfter,_tmpFulizaLimitAfter)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTransactionByRef(ref: String): Transaction? {
    val _sql: String = "SELECT * FROM transactions WHERE remoteRef = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, ref)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRemoteRef: Int = getColumnIndexOrThrow(_stmt, "remoteRef")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfPayee: Int = getColumnIndexOrThrow(_stmt, "payee")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfAccountRef: Int = getColumnIndexOrThrow(_stmt, "accountRef")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfBalanceAfter: Int = getColumnIndexOrThrow(_stmt, "balanceAfter")
        val _columnIndexOfIsFeeTransaction: Int = getColumnIndexOrThrow(_stmt, "isFeeTransaction")
        val _columnIndexOfUsedFulizaAmount: Int = getColumnIndexOrThrow(_stmt, "usedFulizaAmount")
        val _columnIndexOfOriginalSms: Int = getColumnIndexOrThrow(_stmt, "originalSms")
        val _columnIndexOfFulizaOutstandingBalance: Int = getColumnIndexOrThrow(_stmt,
            "fulizaOutstandingBalance")
        val _columnIndexOfFulizaDueDate: Int = getColumnIndexOrThrow(_stmt, "fulizaDueDate")
        val _columnIndexOfMshwariBalanceAfter: Int = getColumnIndexOrThrow(_stmt,
            "mshwariBalanceAfter")
        val _columnIndexOfPochiBalanceAfter: Int = getColumnIndexOrThrow(_stmt, "pochiBalanceAfter")
        val _columnIndexOfFulizaLimitAfter: Int = getColumnIndexOrThrow(_stmt, "fulizaLimitAfter")
        val _result: Transaction?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpRemoteRef: String
          _tmpRemoteRef = _stmt.getText(_columnIndexOfRemoteRef)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpPayee: String
          _tmpPayee = _stmt.getText(_columnIndexOfPayee)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpAccountRef: String?
          if (_stmt.isNull(_columnIndexOfAccountRef)) {
            _tmpAccountRef = null
          } else {
            _tmpAccountRef = _stmt.getText(_columnIndexOfAccountRef)
          }
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpBalanceAfter: Double
          _tmpBalanceAfter = _stmt.getDouble(_columnIndexOfBalanceAfter)
          val _tmpIsFeeTransaction: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFeeTransaction).toInt()
          _tmpIsFeeTransaction = _tmp != 0
          val _tmpUsedFulizaAmount: Double
          _tmpUsedFulizaAmount = _stmt.getDouble(_columnIndexOfUsedFulizaAmount)
          val _tmpOriginalSms: String?
          if (_stmt.isNull(_columnIndexOfOriginalSms)) {
            _tmpOriginalSms = null
          } else {
            _tmpOriginalSms = _stmt.getText(_columnIndexOfOriginalSms)
          }
          val _tmpFulizaOutstandingBalance: Double
          _tmpFulizaOutstandingBalance = _stmt.getDouble(_columnIndexOfFulizaOutstandingBalance)
          val _tmpFulizaDueDate: String?
          if (_stmt.isNull(_columnIndexOfFulizaDueDate)) {
            _tmpFulizaDueDate = null
          } else {
            _tmpFulizaDueDate = _stmt.getText(_columnIndexOfFulizaDueDate)
          }
          val _tmpMshwariBalanceAfter: Double
          _tmpMshwariBalanceAfter = _stmt.getDouble(_columnIndexOfMshwariBalanceAfter)
          val _tmpPochiBalanceAfter: Double
          _tmpPochiBalanceAfter = _stmt.getDouble(_columnIndexOfPochiBalanceAfter)
          val _tmpFulizaLimitAfter: Double
          _tmpFulizaLimitAfter = _stmt.getDouble(_columnIndexOfFulizaLimitAfter)
          _result =
              Transaction(_tmpId,_tmpRemoteRef,_tmpAmount,_tmpType,_tmpPayee,_tmpCategory,_tmpTimestamp,_tmpAccountRef,_tmpFee,_tmpBalanceAfter,_tmpIsFeeTransaction,_tmpUsedFulizaAmount,_tmpOriginalSms,_tmpFulizaOutstandingBalance,_tmpFulizaDueDate,_tmpMshwariBalanceAfter,_tmpPochiBalanceAfter,_tmpFulizaLimitAfter)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getMonthlyIncome(startOfMonth: Long, endOfMonth: Long): Flow<Double?> {
    val _sql: String = """
        |
        |        SELECT SUM(amount) FROM transactions
        |        WHERE type IN ('RECEIVE_MONEY', 'MANUAL_INCOME')
        |        AND isFeeTransaction = 0
        |        AND timestamp >= ? AND timestamp < ?
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfMonth)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endOfMonth)
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getMonthlyExpense(startOfMonth: Long, endOfMonth: Long): Flow<Double?> {
    val _sql: String = """
        |
        |        SELECT SUM(amount) FROM transactions
        |        WHERE type NOT IN ('RECEIVE_MONEY', 'MANUAL_INCOME', 'MANUAL_TRANSFER',
        |                           'MSHWARI_TRANSFER', 'POCHI_TRANSFER', 'POCHI_RECEIVE', 'FULIZA')
        |        AND isFeeTransaction = 0
        |        AND timestamp >= ? AND timestamp < ?
        |    
        """.trimMargin()
    return createFlow(__db, false, arrayOf("transactions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfMonth)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endOfMonth)
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getDailyExpense(startOfDay: Long, endOfDay: Long): Double? {
    val _sql: String = """
        |
        |        SELECT SUM(amount) FROM transactions
        |        WHERE type NOT IN ('RECEIVE_MONEY', 'MANUAL_INCOME', 'MANUAL_TRANSFER',
        |                           'MSHWARI_TRANSFER', 'POCHI_TRANSFER', 'POCHI_RECEIVE', 'FULIZA')
        |        AND isFeeTransaction = 0
        |        AND timestamp >= ? AND timestamp < ?
        |    
        """.trimMargin()
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endOfDay)
        val _result: Double?
        if (_stmt.step()) {
          val _tmp: Double?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getDouble(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllTransactionsOnce(): List<Transaction> {
    val _sql: String = "SELECT * FROM transactions"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRemoteRef: Int = getColumnIndexOrThrow(_stmt, "remoteRef")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfPayee: Int = getColumnIndexOrThrow(_stmt, "payee")
        val _columnIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfAccountRef: Int = getColumnIndexOrThrow(_stmt, "accountRef")
        val _columnIndexOfFee: Int = getColumnIndexOrThrow(_stmt, "fee")
        val _columnIndexOfBalanceAfter: Int = getColumnIndexOrThrow(_stmt, "balanceAfter")
        val _columnIndexOfIsFeeTransaction: Int = getColumnIndexOrThrow(_stmt, "isFeeTransaction")
        val _columnIndexOfUsedFulizaAmount: Int = getColumnIndexOrThrow(_stmt, "usedFulizaAmount")
        val _columnIndexOfOriginalSms: Int = getColumnIndexOrThrow(_stmt, "originalSms")
        val _columnIndexOfFulizaOutstandingBalance: Int = getColumnIndexOrThrow(_stmt,
            "fulizaOutstandingBalance")
        val _columnIndexOfFulizaDueDate: Int = getColumnIndexOrThrow(_stmt, "fulizaDueDate")
        val _columnIndexOfMshwariBalanceAfter: Int = getColumnIndexOrThrow(_stmt,
            "mshwariBalanceAfter")
        val _columnIndexOfPochiBalanceAfter: Int = getColumnIndexOrThrow(_stmt, "pochiBalanceAfter")
        val _columnIndexOfFulizaLimitAfter: Int = getColumnIndexOrThrow(_stmt, "fulizaLimitAfter")
        val _result: MutableList<Transaction> = mutableListOf()
        while (_stmt.step()) {
          val _item: Transaction
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpRemoteRef: String
          _tmpRemoteRef = _stmt.getText(_columnIndexOfRemoteRef)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpType: TransactionType
          _tmpType = __TransactionType_stringToEnum(_stmt.getText(_columnIndexOfType))
          val _tmpPayee: String
          _tmpPayee = _stmt.getText(_columnIndexOfPayee)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_columnIndexOfCategory)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpAccountRef: String?
          if (_stmt.isNull(_columnIndexOfAccountRef)) {
            _tmpAccountRef = null
          } else {
            _tmpAccountRef = _stmt.getText(_columnIndexOfAccountRef)
          }
          val _tmpFee: Double
          _tmpFee = _stmt.getDouble(_columnIndexOfFee)
          val _tmpBalanceAfter: Double
          _tmpBalanceAfter = _stmt.getDouble(_columnIndexOfBalanceAfter)
          val _tmpIsFeeTransaction: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFeeTransaction).toInt()
          _tmpIsFeeTransaction = _tmp != 0
          val _tmpUsedFulizaAmount: Double
          _tmpUsedFulizaAmount = _stmt.getDouble(_columnIndexOfUsedFulizaAmount)
          val _tmpOriginalSms: String?
          if (_stmt.isNull(_columnIndexOfOriginalSms)) {
            _tmpOriginalSms = null
          } else {
            _tmpOriginalSms = _stmt.getText(_columnIndexOfOriginalSms)
          }
          val _tmpFulizaOutstandingBalance: Double
          _tmpFulizaOutstandingBalance = _stmt.getDouble(_columnIndexOfFulizaOutstandingBalance)
          val _tmpFulizaDueDate: String?
          if (_stmt.isNull(_columnIndexOfFulizaDueDate)) {
            _tmpFulizaDueDate = null
          } else {
            _tmpFulizaDueDate = _stmt.getText(_columnIndexOfFulizaDueDate)
          }
          val _tmpMshwariBalanceAfter: Double
          _tmpMshwariBalanceAfter = _stmt.getDouble(_columnIndexOfMshwariBalanceAfter)
          val _tmpPochiBalanceAfter: Double
          _tmpPochiBalanceAfter = _stmt.getDouble(_columnIndexOfPochiBalanceAfter)
          val _tmpFulizaLimitAfter: Double
          _tmpFulizaLimitAfter = _stmt.getDouble(_columnIndexOfFulizaLimitAfter)
          _item =
              Transaction(_tmpId,_tmpRemoteRef,_tmpAmount,_tmpType,_tmpPayee,_tmpCategory,_tmpTimestamp,_tmpAccountRef,_tmpFee,_tmpBalanceAfter,_tmpIsFeeTransaction,_tmpUsedFulizaAmount,_tmpOriginalSms,_tmpFulizaOutstandingBalance,_tmpFulizaDueDate,_tmpMshwariBalanceAfter,_tmpPochiBalanceAfter,_tmpFulizaLimitAfter)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteTransaction(id: Int) {
    val _sql: String = "DELETE FROM transactions WHERE id = ?"
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

  public override suspend fun updateTransactionCategory(id: Int, newCategory: String) {
    val _sql: String = "UPDATE transactions SET category = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, newCategory)
        _argIndex = 2
        _stmt.bindLong(_argIndex, id.toLong())
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateCategoryForPayee(payee: String, newCategory: String) {
    val _sql: String = "UPDATE transactions SET category = ? WHERE LOWER(payee) = LOWER(?)"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, newCategory)
        _argIndex = 2
        _stmt.bindText(_argIndex, payee)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun enrichFulizaTransaction(
    ref: String,
    outstandingBalance: Double,
    dueDate: String?,
    accessFee: Double,
  ) {
    val _sql: String =
        "UPDATE transactions SET fulizaOutstandingBalance = ?, fulizaDueDate = ?, fee = fee + ? WHERE remoteRef = ? AND isFeeTransaction = 0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindDouble(_argIndex, outstandingBalance)
        _argIndex = 2
        if (dueDate == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindText(_argIndex, dueDate)
        }
        _argIndex = 3
        _stmt.bindDouble(_argIndex, accessFee)
        _argIndex = 4
        _stmt.bindText(_argIndex, ref)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllTransactions() {
    val _sql: String = "DELETE FROM transactions"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __TransactionType_enumToString(_value: TransactionType): String = when (_value) {
    TransactionType.PAYBILL -> "PAYBILL"
    TransactionType.BUY_GOODS -> "BUY_GOODS"
    TransactionType.SEND_MONEY -> "SEND_MONEY"
    TransactionType.WITHDRAW -> "WITHDRAW"
    TransactionType.RECEIVE_MONEY -> "RECEIVE_MONEY"
    TransactionType.POCHI -> "POCHI"
    TransactionType.AIRTIME -> "AIRTIME"
    TransactionType.MANUAL_INCOME -> "MANUAL_INCOME"
    TransactionType.MANUAL_EXPENSE -> "MANUAL_EXPENSE"
    TransactionType.MANUAL_TRANSFER -> "MANUAL_TRANSFER"
    TransactionType.MSHWARI_TRANSFER -> "MSHWARI_TRANSFER"
    TransactionType.POCHI_RECEIVE -> "POCHI_RECEIVE"
    TransactionType.POCHI_TRANSFER -> "POCHI_TRANSFER"
    TransactionType.FULIZA -> "FULIZA"
  }

  private fun __TransactionType_stringToEnum(_value: String): TransactionType = when (_value) {
    "PAYBILL" -> TransactionType.PAYBILL
    "BUY_GOODS" -> TransactionType.BUY_GOODS
    "SEND_MONEY" -> TransactionType.SEND_MONEY
    "WITHDRAW" -> TransactionType.WITHDRAW
    "RECEIVE_MONEY" -> TransactionType.RECEIVE_MONEY
    "POCHI" -> TransactionType.POCHI
    "AIRTIME" -> TransactionType.AIRTIME
    "MANUAL_INCOME" -> TransactionType.MANUAL_INCOME
    "MANUAL_EXPENSE" -> TransactionType.MANUAL_EXPENSE
    "MANUAL_TRANSFER" -> TransactionType.MANUAL_TRANSFER
    "MSHWARI_TRANSFER" -> TransactionType.MSHWARI_TRANSFER
    "POCHI_RECEIVE" -> TransactionType.POCHI_RECEIVE
    "POCHI_TRANSFER" -> TransactionType.POCHI_TRANSFER
    "FULIZA" -> TransactionType.FULIZA
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
