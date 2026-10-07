package com.moasseum.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.auth.*
import com.moasseum.app.data.*
import com.moasseum.app.data.local.*
import com.moasseum.app.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class FinanceIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun requireIsolatedQaApp() { check(context.packageName.endsWith(".qa")) { "Tests must never run against the user's production ledger." } }
    @Test fun csvTimezoneAndEditedTransferDateArePreservedInRoom() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val dao = database.financeDao()
            val repo = FinanceRepository(dao)
            val timestamp = LocalDate.parse("2026-10-01").atStartOfDay(java.time.ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
            repo.importTransactions(listOf(ImportedTransaction(TransactionType.EXPENSE, 1000, timestamp, "FOOD", "QA", "", "카드", "IMPORT", timezone = "Asia/Seoul")))
            assertEquals(LocalDate.parse("2026-10-01"), repo.observeTransactions().first().single().occurredDate)
            assertEquals("Asia/Seoul", dao.allTransactionsForBackup().single().timezone)
            repo.saveAccount(null, "QA_A", 10000); repo.saveAccount(null, "QA_B", 0)
            val accounts = repo.observeAccounts().first()
            repo.saveTransfer(null, 1000, accounts[0].id, accounts[1].id, LocalDate.now(), "")
            val transfer = repo.observeTransactions().first().first { it.type == TransactionType.TRANSFER }
            val utc = LocalDate.parse("2026-10-02").atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
            dao.saveTransferEdit(transfer.id, 1000, utc, accounts[0].id, accounts[1].id, "", 10, "UTC")
            assertEquals("UTC", dao.getTransaction(transfer.id)!!.timezone)
            assertEquals(LocalDate.parse("2026-10-02"), repo.observeTransactions().first().first { it.id == transfer.id }.occurredDate)
        } finally { database.close() }
    }

    @Test fun missingRuleAndInvalidFinancialWritesAreRejected() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val repo = FinanceRepository(database.financeDao())
            assertTrue(runCatching { repo.editRecurringRule(999, 1000, TransactionType.EXPENSE, "QA", "OTHER", "", "카드", 1) }.isFailure)
            assertTrue(runCatching { repo.addRecurringRule(1_000_000_000_001L, TransactionType.EXPENSE, "QA", "OTHER", "", "카드", 1) }.isFailure)
            assertTrue(runCatching { repo.addRecurringRule(1000, TransactionType.TRANSFER, "QA", "OTHER", "", "카드", 1) }.isFailure)
            assertTrue(runCatching { repo.updateBudget("2026-10", 0) }.isFailure)
            assertFalse(repo.acceptNotificationCandidate(999))
            assertTrue(repo.observeTransactions().first().isEmpty())
        } finally { database.close() }
    }

    @Test fun cardMethodsAndRemovedCategoryBudgetsStayConsistent() = runBlocking {
        val prefs = UserPreferencesRepository(context)
        val before = prefs.backupSettings()
        try {
            val card = PaymentCard("CARD_QA", "QA 카드", 25, "QA 결제수단")
            prefs.savePaymentCards(listOf(card))
            assertTrue(prefs.paymentMethods.first().contains(card.paymentMethod))
            val key = "CUSTOM_ABCDEF123456"
            prefs.saveCategoryLabels(DEFAULT_CATEGORY_LABELS + (key to "QA 카테고리"))
            prefs.saveCategoryBudgets(mapOf(key to 10000L, "FOOD" to 5000L))
            prefs.saveCategoryLabels(DEFAULT_CATEGORY_LABELS)
            assertFalse(prefs.categoryBudgets.first().containsKey(key))
            assertEquals(5000L, prefs.categoryBudgets.first()["FOOD"])
        } finally { prefs.restoreSettings(before) }
    }

    @Test fun accountTransferEditingDeletionAndInvalidInputAreAtomic() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val repo = FinanceRepository(database.financeDao())
            repo.saveAccount(null, "QA_A", 10000); repo.saveAccount(null, "QA_B", 0)
            val accounts = repo.observeAccounts().first(); val a = accounts.first { it.name == "QA_A" }; val b = accounts.first { it.name == "QA_B" }
            repo.saveTransfer(null, 2000, a.id, b.id, LocalDate.now(), "QA")
            val transfer = repo.observeTransactions().first().single()
            assertEquals(8000, accountBalance(a, listOf(transfer))); assertEquals(2000, accountBalance(b, listOf(transfer)))
            assertTrue(runCatching { repo.saveTransfer(null, 1000, a.id, a.id, LocalDate.now(), "QA") }.isFailure)
            assertEquals(1, repo.observeTransactions().first().size)
            repo.saveTransfer(transfer.id, 3000, a.id, b.id, LocalDate.now(), "edited")
            assertEquals(7000, accountBalance(a, repo.observeTransactions().first()))
            repo.softDeleteTransaction(transfer.id); assertEquals(10000, accountBalance(a, repo.observeTransactions().first()))
            repo.restoreTransaction(transfer.id); assertEquals(7000, accountBalance(a, repo.observeTransactions().first()))
            repo.archiveAccount(b.id, true)
            assertTrue(runCatching { repo.saveTransfer(null, 1000, a.id, b.id, LocalDate.now(), "QA") }.isFailure)
        } finally { database.close() }
    }
    @Test fun failedRestoreRollsBackDatabaseAndBackupRoundTripRestoresLinkedBalances() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val repo = FinanceRepository(database.financeDao())
            repo.saveAccount(null, "QA_A", 10000)
            val account = repo.observeAccounts().first().single()
            repo.addTransaction(1000, TransactionType.EXPENSE, LocalDate.now(), "OTHER", "QA_Expense", "", accountId = account.id)
            repo.ensureBudget(YearMonth.now().toString())
            val before = repo.backupSnapshot()
            assertTrue(runCatching { repo.restoreBackup(before.copy(transactions = before.transactions + before.transactions)) }.isFailure)
            assertEquals(before, repo.backupSnapshot())
            repo.clearAllLocalRecords(); repo.restoreBackup(before)
            assertEquals(before, repo.backupSnapshot())
            assertEquals(9000, accountBalance(account, repo.observeTransactions().first()))
        } finally { database.close() }
    }
    @Test fun keystoreSessionPersistsCiphertextAndCanBeCleared() {
        val store = EncryptedSessionStore(context)
        store.clear()
        val session = AuthSession(SignedInUser("72a43f9d-00e0-44db-af34-4cf143314bb7", "qa@example.invalid"), "SYNTHETIC_SECRET_TOKEN", "SYNTHETIC_REFRESH", 99999999)
        try {
            store.write(session)
            val bytes = java.io.File(context.noBackupFilesDir, "auth-session.enc").readBytes()
            assertFalse(String(bytes, Charsets.ISO_8859_1).contains("SYNTHETIC_SECRET_TOKEN"))
            assertEquals(session.accessToken, store.read()!!.accessToken)
        } finally { store.clear() }
        assertNull(store.read())
    }
    @Test fun pendingEmailLinkSurvivesRestartWithoutPlaintextVerifier() {
        val store = EncryptedPendingAuthStore(context)
        store.clear()
        try {
            val flow = PendingAuthFlow.create("qa@example.invalid", true, 1000, "${context.packageName}://auth/callback")
            store.write(flow)
            val bytes = java.io.File(context.noBackupFilesDir, "auth-pending.enc").readBytes()
            assertFalse(String(bytes, Charsets.ISO_8859_1).contains(flow.verifier))
            assertEquals(flow.verifier, EncryptedPendingAuthStore(context).read()!!.verifier)
            assertTrue(EncryptedPendingAuthStore(context).read()!!.recovery)
            val google = PendingAuthFlow.create("", false, 1000, "${context.packageName}://auth/callback", "google")
            store.write(google)
            val returned = EncryptedPendingAuthStore(context).read()!!
            assertEquals("google", returned.provider); assertEquals(google.verifier, returned.verifier); assertFalse(returned.recovery)
        } finally { store.clear() }
        assertNull(store.read())
    }
    @Test fun fullRestoreKeepsNotificationDeduplicationAndConsentAndWritesSafetyCopy() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        val preferences = UserPreferencesRepository(context)
        val originalSettings = preferences.backupSettings()
        val manager = BackupManager(context, FinanceRepository(database.financeDao()), preferences)
        try {
            val repo = FinanceRepository(database.financeDao())
            repo.addTransaction(1234, TransactionType.EXPENSE, LocalDate.now(), "FOOD", "QA", "")
            val before = manager.snapshot()
            val id = database.financeDao().insertNotificationCandidate(NotificationCandidateEntity(
                packageName = "qa.synthetic.bank", title = "QA", preview = "QA 1234원", merchant = "QA", amount = 1234,
                type = "EXPENSE", categoryKey = "FOOD", postedAt = 1000, fingerprint = "QA-RESTORE", status = "ACCEPTED"))
            val consent = preferences.aiNotificationClassificationEnabled.first()
            val target = before.copy(transactions = emptyList(), settings = originalSettings.copy(reduceMotion = !originalSettings.reduceMotion))
            assertEquals(0, manager.restore(target))
            assertTrue(repo.observeTransactions().first().isEmpty())
            assertEquals(!originalSettings.reduceMotion, preferences.reduceMotion.first())
            assertEquals(consent, preferences.aiNotificationClassificationEnabled.first())
            assertEquals("ACCEPTED", database.financeDao().getNotificationCandidate(id)!!.status)
            val safety = FullBackupCodec.decode(manager.safetyFile.readText())
            assertEquals(before.transactions, safety.transactions)
            assertEquals(before.settings, safety.settings)
            manager.restore(safety)
            assertEquals(before.transactions, repo.backupSnapshot().transactions)
        } finally {
            preferences.restoreSettings(originalSettings)
            manager.safetyFile.delete()
            database.close()
        }
    }
    @Test fun pdfExportProducesRealPaginatedPdf() {
        val date = LocalDate.now()
        val rows = (1..120).map { Transaction(it.toLong(), TransactionType.EXPENSE, 1000, date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), "FOOD", "QA 한글 영수증 $it", "줄바꿈\n확인 및 긴 메모 반복 ".repeat(8), "카드", "MANUAL") }
        val file = java.io.File(context.cacheDir, "qa-monthly-report.pdf")
        file.outputStream().use { MonthlyPdfReport.write(LedgerUiState(YearMonth.now(), rows, 150000), DEFAULT_CATEGORY_LABELS, it) }
        val renderer = android.graphics.pdf.PdfRenderer(android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY))
        renderer.use {
            assertTrue(it.pageCount > 1)
            for (index in listOf(0, it.pageCount - 1)) it.openPage(index).use { page ->
                val bitmap = android.graphics.Bitmap.createBitmap(595, 842, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                var ink = 0
                for (y in 28..49) for (x in 36..520) if (bitmap.getPixel(x, y) != android.graphics.Color.WHITE) ink++
                assertTrue("Header must render on every page", ink > 100)
                java.io.File(context.cacheDir, "qa-pdf-page-$index.png").outputStream().use { out -> bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out) }
                bitmap.recycle()
            }
        }
        assertTrue(file.length() > 1000)
    }

    @Test fun csvImportFailureDoesNotLeavePartialRowsAndTransferDuplicateIsSkipped() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val repo = FinanceRepository(database.financeDao())
            val valid = ImportedTransaction(TransactionType.EXPENSE, 1000, 1000, "OTHER", "QA", "", "카드", "IMPORT")
            assertTrue(runCatching { repo.importTransactions(listOf(valid, valid.copy(accountId = "MISSING"))) }.isFailure)
            assertTrue(repo.observeTransactions().first().isEmpty())
            repo.saveAccount(null, "QA_A", 10000); repo.saveAccount(null, "QA_B", 0)
            val accounts = repo.observeAccounts().first()
            repo.saveTransfer(null, 1000, accounts[0].id, accounts[1].id, LocalDate.now(), "QA")
            val csv = CsvBackup.encode(repo.observeTransactions().first())
            assertEquals(0, repo.importTransactions(CsvBackup.decode(csv)))
            assertEquals(1, repo.observeTransactions().first().size)
            // Identical expense fields from different explicitly linked wallets are distinct.
            assertEquals(2, repo.importTransactions(listOf(valid.copy(accountId = accounts[0].id), valid.copy(accountId = accounts[1].id))))
            assertEquals(0, repo.importTransactions(listOf(valid))) // A legacy CSV without account links must not duplicate them.
            assertEquals(3, repo.observeTransactions().first().size)
        } finally { database.close() }
    }
}
