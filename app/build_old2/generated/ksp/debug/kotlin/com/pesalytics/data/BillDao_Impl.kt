package com.pesalytics.`data`

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.pesalytics.model.Bill
import com.pesalytics.model.BillCycle
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
public class BillDao_Impl(
  __db: RoomDatabase,
) : BillDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBill: EntityInsertAdapter<Bill>

  private val __updateAdapterOfBill: EntityDeleteOrUpdateAdapter<Bill>
  init {
    this.__db = __db
    this.__insertAdapterOfBill = object : EntityInsertAdapter<Bill>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `bills` (`id`,`name`,`amount`,`payee`,`cycle`,`nextDueDate`,`isAutoPay`,`isPaid`,`lastPaidDate`,`isPaused`,`pauseFreezeDueDate`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Bill) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.payee)
        statement.bindText(5, __BillCycle_enumToString(entity.cycle))
        statement.bindLong(6, entity.nextDueDate)
        val _tmp: Int = if (entity.isAutoPay) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        val _tmp_1: Int = if (entity.isPaid) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        val _tmpLastPaidDate: Long? = entity.lastPaidDate
        if (_tmpLastPaidDate == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpLastPaidDate)
        }
        val _tmp_2: Int = if (entity.isPaused) 1 else 0
        statement.bindLong(10, _tmp_2.toLong())
        val _tmp_3: Int = if (entity.pauseFreezeDueDate) 1 else 0
        statement.bindLong(11, _tmp_3.toLong())
      }
    }
    this.__updateAdapterOfBill = object : EntityDeleteOrUpdateAdapter<Bill>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `bills` SET `id` = ?,`name` = ?,`amount` = ?,`payee` = ?,`cycle` = ?,`nextDueDate` = ?,`isAutoPay` = ?,`isPaid` = ?,`lastPaidDate` = ?,`isPaused` = ?,`pauseFreezeDueDate` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Bill) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindDouble(3, entity.amount)
        statement.bindText(4, entity.payee)
        statement.bindText(5, __BillCycle_enumToString(entity.cycle))
        statement.bindLong(6, entity.nextDueDate)
        val _tmp: Int = if (entity.isAutoPay) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        val _tmp_1: Int = if (entity.isPaid) 1 else 0
        statement.bindLong(8, _tmp_1.toLong())
        val _tmpLastPaidDate: Long? = entity.lastPaidDate
        if (_tmpLastPaidDate == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpLastPaidDate)
        }
        val _tmp_2: Int = if (entity.isPaused) 1 else 0
        statement.bindLong(10, _tmp_2.toLong())
        val _tmp_3: Int = if (entity.pauseFreezeDueDate) 1 else 0
        statement.bindLong(11, _tmp_3.toLong())
        statement.bindLong(12, entity.id.toLong())
      }
    }
  }

  public override suspend fun insertBill(bill: Bill): Unit = performSuspending(__db, false, true) {
      _connection ->
    __insertAdapterOfBill.insert(_connection, bill)
  }

  public override suspend fun updateBill(bill: Bill): Unit = performSuspending(__db, false, true) {
      _connection ->
    __updateAdapterOfBill.handle(_connection, bill)
  }

  public override fun getAllBills(): Flow<List<Bill>> {
    val _sql: String = "SELECT * FROM bills ORDER BY nextDueDate ASC"
    return createFlow(__db, false, arrayOf("bills")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfPayee: Int = getColumnIndexOrThrow(_stmt, "payee")
        val _columnIndexOfCycle: Int = getColumnIndexOrThrow(_stmt, "cycle")
        val _columnIndexOfNextDueDate: Int = getColumnIndexOrThrow(_stmt, "nextDueDate")
        val _columnIndexOfIsAutoPay: Int = getColumnIndexOrThrow(_stmt, "isAutoPay")
        val _columnIndexOfIsPaid: Int = getColumnIndexOrThrow(_stmt, "isPaid")
        val _columnIndexOfLastPaidDate: Int = getColumnIndexOrThrow(_stmt, "lastPaidDate")
        val _columnIndexOfIsPaused: Int = getColumnIndexOrThrow(_stmt, "isPaused")
        val _columnIndexOfPauseFreezeDueDate: Int = getColumnIndexOrThrow(_stmt,
            "pauseFreezeDueDate")
        val _result: MutableList<Bill> = mutableListOf()
        while (_stmt.step()) {
          val _item: Bill
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpPayee: String
          _tmpPayee = _stmt.getText(_columnIndexOfPayee)
          val _tmpCycle: BillCycle
          _tmpCycle = __BillCycle_stringToEnum(_stmt.getText(_columnIndexOfCycle))
          val _tmpNextDueDate: Long
          _tmpNextDueDate = _stmt.getLong(_columnIndexOfNextDueDate)
          val _tmpIsAutoPay: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAutoPay).toInt()
          _tmpIsAutoPay = _tmp != 0
          val _tmpIsPaid: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPaid).toInt()
          _tmpIsPaid = _tmp_1 != 0
          val _tmpLastPaidDate: Long?
          if (_stmt.isNull(_columnIndexOfLastPaidDate)) {
            _tmpLastPaidDate = null
          } else {
            _tmpLastPaidDate = _stmt.getLong(_columnIndexOfLastPaidDate)
          }
          val _tmpIsPaused: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsPaused).toInt()
          _tmpIsPaused = _tmp_2 != 0
          val _tmpPauseFreezeDueDate: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfPauseFreezeDueDate).toInt()
          _tmpPauseFreezeDueDate = _tmp_3 != 0
          _item =
              Bill(_tmpId,_tmpName,_tmpAmount,_tmpPayee,_tmpCycle,_tmpNextDueDate,_tmpIsAutoPay,_tmpIsPaid,_tmpLastPaidDate,_tmpIsPaused,_tmpPauseFreezeDueDate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBillsOnce(): List<Bill> {
    val _sql: String = "SELECT * FROM bills"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfPayee: Int = getColumnIndexOrThrow(_stmt, "payee")
        val _columnIndexOfCycle: Int = getColumnIndexOrThrow(_stmt, "cycle")
        val _columnIndexOfNextDueDate: Int = getColumnIndexOrThrow(_stmt, "nextDueDate")
        val _columnIndexOfIsAutoPay: Int = getColumnIndexOrThrow(_stmt, "isAutoPay")
        val _columnIndexOfIsPaid: Int = getColumnIndexOrThrow(_stmt, "isPaid")
        val _columnIndexOfLastPaidDate: Int = getColumnIndexOrThrow(_stmt, "lastPaidDate")
        val _columnIndexOfIsPaused: Int = getColumnIndexOrThrow(_stmt, "isPaused")
        val _columnIndexOfPauseFreezeDueDate: Int = getColumnIndexOrThrow(_stmt,
            "pauseFreezeDueDate")
        val _result: MutableList<Bill> = mutableListOf()
        while (_stmt.step()) {
          val _item: Bill
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAmount: Double
          _tmpAmount = _stmt.getDouble(_columnIndexOfAmount)
          val _tmpPayee: String
          _tmpPayee = _stmt.getText(_columnIndexOfPayee)
          val _tmpCycle: BillCycle
          _tmpCycle = __BillCycle_stringToEnum(_stmt.getText(_columnIndexOfCycle))
          val _tmpNextDueDate: Long
          _tmpNextDueDate = _stmt.getLong(_columnIndexOfNextDueDate)
          val _tmpIsAutoPay: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsAutoPay).toInt()
          _tmpIsAutoPay = _tmp != 0
          val _tmpIsPaid: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsPaid).toInt()
          _tmpIsPaid = _tmp_1 != 0
          val _tmpLastPaidDate: Long?
          if (_stmt.isNull(_columnIndexOfLastPaidDate)) {
            _tmpLastPaidDate = null
          } else {
            _tmpLastPaidDate = _stmt.getLong(_columnIndexOfLastPaidDate)
          }
          val _tmpIsPaused: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsPaused).toInt()
          _tmpIsPaused = _tmp_2 != 0
          val _tmpPauseFreezeDueDate: Boolean
          val _tmp_3: Int
          _tmp_3 = _stmt.getLong(_columnIndexOfPauseFreezeDueDate).toInt()
          _tmpPauseFreezeDueDate = _tmp_3 != 0
          _item =
              Bill(_tmpId,_tmpName,_tmpAmount,_tmpPayee,_tmpCycle,_tmpNextDueDate,_tmpIsAutoPay,_tmpIsPaid,_tmpLastPaidDate,_tmpIsPaused,_tmpPauseFreezeDueDate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBill(id: Int) {
    val _sql: String = "DELETE FROM bills WHERE id = ?"
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

  public override suspend fun deleteAllBills() {
    val _sql: String = "DELETE FROM bills"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __BillCycle_enumToString(_value: BillCycle): String = when (_value) {
    BillCycle.DAILY -> "DAILY"
    BillCycle.WEEKLY -> "WEEKLY"
    BillCycle.MONTHLY -> "MONTHLY"
    BillCycle.YEARLY -> "YEARLY"
  }

  private fun __BillCycle_stringToEnum(_value: String): BillCycle = when (_value) {
    "DAILY" -> BillCycle.DAILY
    "WEEKLY" -> BillCycle.WEEKLY
    "MONTHLY" -> BillCycle.MONTHLY
    "YEARLY" -> BillCycle.YEARLY
    else -> throw IllegalArgumentException("Can't convert value to enum, unknown value: " + _value)
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
