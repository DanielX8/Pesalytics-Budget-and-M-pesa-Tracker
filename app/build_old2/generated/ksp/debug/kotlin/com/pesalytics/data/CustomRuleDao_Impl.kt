package com.pesalytics.`data`

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.pesalytics.model.CustomRule
import javax.`annotation`.processing.Generated
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
public class CustomRuleDao_Impl(
  __db: RoomDatabase,
) : CustomRuleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfCustomRule: EntityInsertAdapter<CustomRule>
  init {
    this.__db = __db
    this.__insertAdapterOfCustomRule = object : EntityInsertAdapter<CustomRule>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `custom_rules` (`id`,`payeePattern`,`mappedCategory`) VALUES (nullif(?, 0),?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CustomRule) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.payeePattern)
        statement.bindText(3, entity.mappedCategory)
      }
    }
  }

  public override suspend fun insertRule(rule: CustomRule): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfCustomRule.insert(_connection, rule)
  }

  public override fun getAllRules(): Flow<List<CustomRule>> {
    val _sql: String = "SELECT * FROM custom_rules"
    return createFlow(__db, false, arrayOf("custom_rules")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPayeePattern: Int = getColumnIndexOrThrow(_stmt, "payeePattern")
        val _columnIndexOfMappedCategory: Int = getColumnIndexOrThrow(_stmt, "mappedCategory")
        val _result: MutableList<CustomRule> = mutableListOf()
        while (_stmt.step()) {
          val _item: CustomRule
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpPayeePattern: String
          _tmpPayeePattern = _stmt.getText(_columnIndexOfPayeePattern)
          val _tmpMappedCategory: String
          _tmpMappedCategory = _stmt.getText(_columnIndexOfMappedCategory)
          _item = CustomRule(_tmpId,_tmpPayeePattern,_tmpMappedCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCustomRulesOnce(): List<CustomRule> {
    val _sql: String = "SELECT * FROM custom_rules"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPayeePattern: Int = getColumnIndexOrThrow(_stmt, "payeePattern")
        val _columnIndexOfMappedCategory: Int = getColumnIndexOrThrow(_stmt, "mappedCategory")
        val _result: MutableList<CustomRule> = mutableListOf()
        while (_stmt.step()) {
          val _item: CustomRule
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpPayeePattern: String
          _tmpPayeePattern = _stmt.getText(_columnIndexOfPayeePattern)
          val _tmpMappedCategory: String
          _tmpMappedCategory = _stmt.getText(_columnIndexOfMappedCategory)
          _item = CustomRule(_tmpId,_tmpPayeePattern,_tmpMappedCategory)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllRules() {
    val _sql: String = "DELETE FROM custom_rules"
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
