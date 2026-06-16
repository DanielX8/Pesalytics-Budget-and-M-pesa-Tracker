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

@Database(entities = [Transaction::class, Bill::class, Budget::class, CustomRule::class, Goal::class], version = 10, exportSchema = false)
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
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
