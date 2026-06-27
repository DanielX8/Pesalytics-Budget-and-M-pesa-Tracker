package com.pesalytics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pesalytics.model.Bill
import com.pesalytics.model.Budget
import com.pesalytics.model.CustomRule
import com.pesalytics.model.Transaction
import com.pesalytics.model.Goal

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN fulizaOutstandingBalance REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN fulizaDueDate TEXT")
        db.execSQL("ALTER TABLE bills ADD COLUMN isPaid INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bills ADD COLUMN lastPaidDate INTEGER")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE goals ADD COLUMN savedAmount REAL NOT NULL DEFAULT 0")
    }
}

// Budgets previously had no uniqueness on (category, monthYear), so editing a limit
// inserted a brand-new row instead of replacing it (the old buggy behaviour). De-duplicate
// existing rows (keep the newest per category+month) then add a unique index so that
// OnConflictStrategy.REPLACE upserts correctly from here on.
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM budgets WHERE id NOT IN " +
                "(SELECT MAX(id) FROM budgets GROUP BY category, monthYear)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_category_monthYear " +
                "ON budgets(category, monthYear)"
        )
    }
}

// Transactions had no uniqueness, so overlapping SMS syncs could double-insert the same
// receipt. De-duplicate existing rows (keep the earliest per receipt) then add a unique
// index so OnConflictStrategy.REPLACE upserts and duplicates become impossible.
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM transactions WHERE id NOT IN " +
                "(SELECT MIN(id) FROM transactions GROUP BY remoteRef, isFeeTransaction)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_remoteRef_isFeeTransaction " +
                "ON transactions(remoteRef, isFeeTransaction)"
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN mshwariBalanceAfter REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN pochiBalanceAfter REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN fulizaLimitAfter REAL NOT NULL DEFAULT 0.0")
    }
}

@Database(entities = [Transaction::class, Bill::class, Budget::class, CustomRule::class, Goal::class], version = 13, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun billDao(): BillDao
    abstract fun budgetDao(): BudgetDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pesalytics_database"
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
