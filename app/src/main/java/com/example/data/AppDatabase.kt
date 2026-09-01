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
import com.pesalytics.model.AppNotificationEntity

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

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE bills ADD COLUMN isPaused INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE bills ADD COLUMN pauseFreezeDueDate INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `goal_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `goalId` INTEGER NOT NULL, `amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_transactions_goalId` ON `goal_transactions` (`goalId`)")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val currentTime = System.currentTimeMillis()
        database.execSQL("ALTER TABLE goals ADD COLUMN createdAt INTEGER NOT NULL DEFAULT $currentTime")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_notifications` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isRead` INTEGER NOT NULL, `actionRoute` TEXT)"
        )
    }
}
}

@Database(entities = [Transaction::class, Bill::class, Budget::class, CustomRule::class, Goal::class, com.pesalytics.model.GoalTransaction::class, AppNotificationEntity::class], version = 17, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
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
                ).addMigrations(MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17).build()
                INSTANCE = instance
                instance
            }
        }
    }
}





