package com.moasseum.app

import com.moasseum.app.data.*
import com.moasseum.app.data.local.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FullBackupTest {
    private val account = AccountEntity("72a43f9d-00e0-44db-af34-4cf143314bb7", "계좌", 10000, false, 123)
    private fun backup() = FullBackup(
        listOf(TransactionEntity(id = 1, type = "EXPENSE", amount = 1000, occurredAt = 1234, timezone = "Asia/Seoul", categoryKey = "FOOD", merchant = "카페", createdAt = 123, updatedAt = 124, accountId = account.id, deletedAt = 125)),
        listOf(BudgetEntity("2026-10", 10000, true, 123)),
        listOf(RecurringTransactionEntity(1, "EXPENSE", 2000, "구독", 31, "2026-10-31", "OTHER", "메모", "카드", false, 123, 124)),
        listOf(account), BackupSettings(true, false, DEFAULT_PAYMENT_METHODS, emptyList(), DEFAULT_CATEGORY_LABELS, mapOf("FOOD" to 10000)),
    )
    @Test fun completeRoundTripPreservesAccountsRulesTombstonesAndSettings() {
        assertEquals(backup(), FullBackupCodec.decode(FullBackupCodec.encode(backup())))
    }
    @Test fun versionOneIsRecognizedForNonDestructiveMerge() {
        val json = JsonBackup.encode(listOf(com.moasseum.app.domain.Transaction(1, com.moasseum.app.domain.TransactionType.EXPENSE, 1000, 1234, "OTHER", "카페", "", "카드", "MANUAL")))
        assertTrue(FullBackupCodec.decode(json).legacy)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsUnknownApp() {
        FullBackupCodec.decode(JSONObject(FullBackupCodec.encode(backup())).put("app", "다른 앱").toString())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOrphanAccountBeforeMutation() {
        val root = JSONObject(FullBackupCodec.encode(backup())); root.getJSONArray("transactions").getJSONObject(0).put("accountId", "missing")
        FullBackupCodec.decode(root.toString())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidTransfersBeforeMutation() {
        val root = JSONObject(FullBackupCodec.encode(backup())); root.getJSONArray("transactions").getJSONObject(0).put("type", "TRANSFER").put("destinationAccountId", account.id)
        FullBackupCodec.decode(root.toString())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateIds() {
        val root = JSONObject(FullBackupCodec.encode(backup())); val rows = root.getJSONArray("transactions"); rows.put(rows.getJSONObject(0))
        FullBackupCodec.decode(root.toString())
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsOversizedStream() {
        FullBackupCodec.read(ByteArray(FullBackupCodec.MAX_BYTES + 1).inputStream())
    }
}
