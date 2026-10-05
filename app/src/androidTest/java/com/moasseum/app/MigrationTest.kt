package com.moasseum.app

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.data.local.FinanceDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test fun versionThreeLedgerSurvivesVersionFourSchemaValidation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".qa"))
        val name = "qa-migration-v3.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { database ->
            database.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ownerId TEXT NOT NULL, ledgerId TEXT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, currency TEXT NOT NULL, occurredAt INTEGER NOT NULL, timezone TEXT NOT NULL, categoryKey TEXT NOT NULL, merchant TEXT NOT NULL, memo TEXT NOT NULL, paymentMethod TEXT NOT NULL, source TEXT NOT NULL, sharingScope TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, deletedAt INTEGER)")
            database.execSQL("CREATE INDEX index_transactions_occurredAt ON transactions(occurredAt)")
            database.execSQL("CREATE INDEX index_transactions_deletedAt ON transactions(deletedAt)")
            database.execSQL("CREATE TABLE budgets (monthKey TEXT PRIMARY KEY NOT NULL, amount INTEGER NOT NULL, rollover INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            database.execSQL("CREATE TABLE notification_candidates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, packageName TEXT NOT NULL, title TEXT NOT NULL, preview TEXT NOT NULL, merchant TEXT NOT NULL, amount INTEGER NOT NULL, type TEXT NOT NULL, categoryKey TEXT NOT NULL, postedAt INTEGER NOT NULL, fingerprint TEXT NOT NULL, status TEXT NOT NULL)")
            database.execSQL("CREATE UNIQUE INDEX index_notification_candidates_fingerprint ON notification_candidates(fingerprint)")
            database.execSQL("CREATE TABLE recurring_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount INTEGER NOT NULL, merchant TEXT NOT NULL, dayOfMonth INTEGER NOT NULL, nextOccurrenceDate TEXT NOT NULL, categoryKey TEXT NOT NULL, memo TEXT NOT NULL, paymentMethod TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
            database.execSQL("CREATE INDEX index_recurring_transactions_isActive_nextOccurrenceDate ON recurring_transactions(isActive, nextOccurrenceDate)")
            database.execSQL("INSERT INTO transactions VALUES (1, 'local-user', 'personal', 'EXPENSE', 1234, 'KRW', 1234, 'Asia/Seoul', 'FOOD', 'QA_PRESERVE', '', '카드', 'MANUAL', 'PRIVATE', 1234, 1234, NULL)")
            database.execSQL("INSERT INTO budgets VALUES ('2026-10', 1500000, 0, 1234)")
            database.version = 3
        }
        val migrated = FinanceDatabase.create(context, name)
        try {
            val rows = migrated.financeDao().allTransactionsForBackup()
            assertEquals(1, rows.size); assertEquals(1234L, rows.single().amount)
            assertNull(rows.single().accountId); assertNull(rows.single().destinationAccountId)
            assertEquals(1500000L, migrated.financeDao().getBudget("2026-10")!!.amount)
            assertTrue(migrated.financeDao().allAccountsForBackup().isEmpty())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }
}
