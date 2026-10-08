package com.moasseum.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, BudgetEntity::class, NotificationCandidateEntity::class, RecurringTransactionEntity::class, AccountEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        fun create(context: Context, name: String = "moasseum.db"): FinanceDatabase =
            Room.databaseBuilder(
                context,
                FinanceDatabase::class.java,
                name,
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN accountId TEXT")
        database.execSQL("ALTER TABLE transactions ADD COLUMN destinationAccountId TEXT")
        database.execSQL("CREATE TABLE IF NOT EXISTS accounts (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, openingBalance INTEGER NOT NULL, archived INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN cloudId TEXT")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_cloudId ON transactions(cloudId)")
    }
}

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions ADD COLUMN installmentGroupId TEXT")
        database.execSQL("ALTER TABLE transactions ADD COLUMN installmentNumber INTEGER")
        database.execSQL("ALTER TABLE transactions ADD COLUMN installmentCount INTEGER")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_installmentGroupId ON transactions(installmentGroupId)")
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS notification_candidates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                packageName TEXT NOT NULL,
                title TEXT NOT NULL,
                preview TEXT NOT NULL,
                merchant TEXT NOT NULL,
                amount INTEGER NOT NULL,
                type TEXT NOT NULL,
                categoryKey TEXT NOT NULL,
                postedAt INTEGER NOT NULL,
                fingerprint TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING'
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_notification_candidates_fingerprint ON notification_candidates(fingerprint)",
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recurring_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                merchant TEXT NOT NULL,
                dayOfMonth INTEGER NOT NULL,
                nextOccurrenceDate TEXT NOT NULL,
                categoryKey TEXT NOT NULL,
                memo TEXT NOT NULL,
                paymentMethod TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_recurring_transactions_isActive_nextOccurrenceDate ON recurring_transactions(isActive, nextOccurrenceDate)",
        )
    }
}
