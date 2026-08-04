package com.pesalytics.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _transactionDao: Lazy<TransactionDao> = lazy {
    TransactionDao_Impl(this)
  }

  private val _billDao: Lazy<BillDao> = lazy {
    BillDao_Impl(this)
  }

  private val _budgetDao: Lazy<BudgetDao> = lazy {
    BudgetDao_Impl(this)
  }

  private val _customRuleDao: Lazy<CustomRuleDao> = lazy {
    CustomRuleDao_Impl(this)
  }

  private val _goalDao: Lazy<GoalDao> = lazy {
    GoalDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(16,
        "0c17403292595793800fe5edf53c2efa", "4b9ac933372d3cb13f6bd1cff60ebde4") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteRef` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `payee` TEXT NOT NULL, `category` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `accountRef` TEXT, `fee` REAL NOT NULL, `balanceAfter` REAL NOT NULL, `isFeeTransaction` INTEGER NOT NULL, `usedFulizaAmount` REAL NOT NULL, `originalSms` TEXT, `fulizaOutstandingBalance` REAL NOT NULL, `fulizaDueDate` TEXT, `mshwariBalanceAfter` REAL NOT NULL, `pochiBalanceAfter` REAL NOT NULL, `fulizaLimitAfter` REAL NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_transactions_remoteRef_isFeeTransaction` ON `transactions` (`remoteRef`, `isFeeTransaction`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, `payee` TEXT NOT NULL, `cycle` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, `isAutoPay` INTEGER NOT NULL, `isPaid` INTEGER NOT NULL, `lastPaidDate` INTEGER, `isPaused` INTEGER NOT NULL, `pauseFreezeDueDate` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `limitAmount` REAL NOT NULL, `spentAmount` REAL NOT NULL, `monthYear` TEXT NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_category_monthYear` ON `budgets` (`category`, `monthYear`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `custom_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `payeePattern` TEXT NOT NULL, `mappedCategory` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `targetDate` INTEGER NOT NULL, `monthlyContribution` REAL NOT NULL, `color` INTEGER NOT NULL, `savedAmount` REAL NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `goal_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `goalId` INTEGER NOT NULL, `amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_transactions_goalId` ON `goal_transactions` (`goalId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0c17403292595793800fe5edf53c2efa')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `transactions`")
        connection.execSQL("DROP TABLE IF EXISTS `bills`")
        connection.execSQL("DROP TABLE IF EXISTS `budgets`")
        connection.execSQL("DROP TABLE IF EXISTS `custom_rules`")
        connection.execSQL("DROP TABLE IF EXISTS `goals`")
        connection.execSQL("DROP TABLE IF EXISTS `goal_transactions`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsTransactions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransactions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("remoteRef", TableInfo.Column("remoteRef", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("payee", TableInfo.Column("payee", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("accountRef", TableInfo.Column("accountRef", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("fee", TableInfo.Column("fee", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("balanceAfter", TableInfo.Column("balanceAfter", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("isFeeTransaction", TableInfo.Column("isFeeTransaction", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("usedFulizaAmount", TableInfo.Column("usedFulizaAmount", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("originalSms", TableInfo.Column("originalSms", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("fulizaOutstandingBalance",
            TableInfo.Column("fulizaOutstandingBalance", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("fulizaDueDate", TableInfo.Column("fulizaDueDate", "TEXT", false,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("mshwariBalanceAfter", TableInfo.Column("mshwariBalanceAfter",
            "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("pochiBalanceAfter", TableInfo.Column("pochiBalanceAfter", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransactions.put("fulizaLimitAfter", TableInfo.Column("fulizaLimitAfter", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransactions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTransactions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesTransactions.add(TableInfo.Index("index_transactions_remoteRef_isFeeTransaction",
            true, listOf("remoteRef", "isFeeTransaction"), listOf("ASC", "ASC")))
        val _infoTransactions: TableInfo = TableInfo("transactions", _columnsTransactions,
            _foreignKeysTransactions, _indicesTransactions)
        val _existingTransactions: TableInfo = read(connection, "transactions")
        if (!_infoTransactions.equals(_existingTransactions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transactions(com.pesalytics.model.Transaction).
              | Expected:
              |""".trimMargin() + _infoTransactions + """
              |
              | Found:
              |""".trimMargin() + _existingTransactions)
        }
        val _columnsBills: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBills.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("payee", TableInfo.Column("payee", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("cycle", TableInfo.Column("cycle", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("nextDueDate", TableInfo.Column("nextDueDate", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("isAutoPay", TableInfo.Column("isAutoPay", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("isPaid", TableInfo.Column("isPaid", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("lastPaidDate", TableInfo.Column("lastPaidDate", "INTEGER", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("isPaused", TableInfo.Column("isPaused", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBills.put("pauseFreezeDueDate", TableInfo.Column("pauseFreezeDueDate", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBills: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBills: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBills: TableInfo = TableInfo("bills", _columnsBills, _foreignKeysBills,
            _indicesBills)
        val _existingBills: TableInfo = read(connection, "bills")
        if (!_infoBills.equals(_existingBills)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |bills(com.pesalytics.model.Bill).
              | Expected:
              |""".trimMargin() + _infoBills + """
              |
              | Found:
              |""".trimMargin() + _existingBills)
        }
        val _columnsBudgets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBudgets.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("limitAmount", TableInfo.Column("limitAmount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("spentAmount", TableInfo.Column("spentAmount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBudgets.put("monthYear", TableInfo.Column("monthYear", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBudgets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBudgets: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesBudgets.add(TableInfo.Index("index_budgets_category_monthYear", true,
            listOf("category", "monthYear"), listOf("ASC", "ASC")))
        val _infoBudgets: TableInfo = TableInfo("budgets", _columnsBudgets, _foreignKeysBudgets,
            _indicesBudgets)
        val _existingBudgets: TableInfo = read(connection, "budgets")
        if (!_infoBudgets.equals(_existingBudgets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |budgets(com.pesalytics.model.Budget).
              | Expected:
              |""".trimMargin() + _infoBudgets + """
              |
              | Found:
              |""".trimMargin() + _existingBudgets)
        }
        val _columnsCustomRules: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsCustomRules.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomRules.put("payeePattern", TableInfo.Column("payeePattern", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsCustomRules.put("mappedCategory", TableInfo.Column("mappedCategory", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysCustomRules: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesCustomRules: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoCustomRules: TableInfo = TableInfo("custom_rules", _columnsCustomRules,
            _foreignKeysCustomRules, _indicesCustomRules)
        val _existingCustomRules: TableInfo = read(connection, "custom_rules")
        if (!_infoCustomRules.equals(_existingCustomRules)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |custom_rules(com.pesalytics.model.CustomRule).
              | Expected:
              |""".trimMargin() + _infoCustomRules + """
              |
              | Found:
              |""".trimMargin() + _existingCustomRules)
        }
        val _columnsGoals: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGoals.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("targetAmount", TableInfo.Column("targetAmount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("targetDate", TableInfo.Column("targetDate", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("monthlyContribution", TableInfo.Column("monthlyContribution", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("color", TableInfo.Column("color", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("savedAmount", TableInfo.Column("savedAmount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoals.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGoals: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesGoals: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoGoals: TableInfo = TableInfo("goals", _columnsGoals, _foreignKeysGoals,
            _indicesGoals)
        val _existingGoals: TableInfo = read(connection, "goals")
        if (!_infoGoals.equals(_existingGoals)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |goals(com.pesalytics.model.Goal).
              | Expected:
              |""".trimMargin() + _infoGoals + """
              |
              | Found:
              |""".trimMargin() + _existingGoals)
        }
        val _columnsGoalTransactions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsGoalTransactions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoalTransactions.put("goalId", TableInfo.Column("goalId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoalTransactions.put("amount", TableInfo.Column("amount", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsGoalTransactions.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysGoalTransactions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysGoalTransactions.add(TableInfo.ForeignKey("goals", "CASCADE", "NO ACTION",
            listOf("goalId"), listOf("id")))
        val _indicesGoalTransactions: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesGoalTransactions.add(TableInfo.Index("index_goal_transactions_goalId", false,
            listOf("goalId"), listOf("ASC")))
        val _infoGoalTransactions: TableInfo = TableInfo("goal_transactions",
            _columnsGoalTransactions, _foreignKeysGoalTransactions, _indicesGoalTransactions)
        val _existingGoalTransactions: TableInfo = read(connection, "goal_transactions")
        if (!_infoGoalTransactions.equals(_existingGoalTransactions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |goal_transactions(com.pesalytics.model.GoalTransaction).
              | Expected:
              |""".trimMargin() + _infoGoalTransactions + """
              |
              | Found:
              |""".trimMargin() + _existingGoalTransactions)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "transactions", "bills",
        "budgets", "custom_rules", "goals", "goal_transactions")
  }

  public override fun clearAllTables() {
    super.performClear(true, "transactions", "bills", "budgets", "custom_rules", "goals",
        "goal_transactions")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TransactionDao::class, TransactionDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BillDao::class, BillDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(BudgetDao::class, BudgetDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(CustomRuleDao::class, CustomRuleDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(GoalDao::class, GoalDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun transactionDao(): TransactionDao = _transactionDao.value

  public override fun billDao(): BillDao = _billDao.value

  public override fun budgetDao(): BudgetDao = _budgetDao.value

  public override fun customRuleDao(): CustomRuleDao = _customRuleDao.value

  public override fun goalDao(): GoalDao = _goalDao.value
}
