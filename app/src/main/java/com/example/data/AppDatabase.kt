package com.pesasense.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pesasense.model.Bill
import com.pesasense.model.Budget
import com.pesasense.model.CustomRule
import com.pesasense.model.Transaction
import com.pesasense.model.Goal

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

@Database(entities = [Transaction::class, Bill::class, Budget::class, CustomRule::class, Goal::class], version = 11, exportSchema = false)
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
                    "pesasense_database"
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
