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
        listOf(account), BackupSettings(
            true, false, DEFAULT_PAYMENT_METHODS, emptyList(), DEFAULT_CATEGORY_LABELS, mapOf("FOOD" to 10000),
            DEFAULT_CATEGORY_LABELS.keys.toList(),
            mapOf("2026-10" to 250000L),
            setOf(com.moasseum.app.domain.HomeDashboardCards.TODAY_WEEK, com.moasseum.app.domain.HomeDashboardCards.CATEGORIES),
            14,
            java.time.LocalDate.of(2026, 10, 2),
        ),
    )
    @Test fun completeRoundTripPreservesAccountsRulesTombstonesAndSettings() {
        assertEquals(backup(), FullBackupCodec.decode(FullBackupCodec.encode(backup())))
    }
    @Test fun installmentGroupSurvivesFullBackupRoundTrip() {
        val groupId = "971a8c7e-7f32-4e6b-9a74-82c22e218671"
        val rows = (1..3).map { number ->
            TransactionEntity(
                id = number.toLong(), type = "EXPENSE", amount = 10_000, occurredAt = number.toLong(),
                timezone = "Asia/Seoul", categoryKey = "SHOPPING", merchant = "가전", memo = "할부 $number/3",
                paymentMethod = "카드", source = "INSTALLMENT", createdAt = 1, updatedAt = 1,
                installmentGroupId = groupId, installmentNumber = number, installmentCount = 3,
            )
        }
        val original = backup().copy(transactions = rows)
        assertEquals(original, FullBackupCodec.decode(FullBackupCodec.encode(original)))
    }
    @Test fun oldSettingsBackupWithoutCategoryOrderUsesSafeDefault() {
        val root = JSONObject(FullBackupCodec.encode(backup()))
        root.getJSONObject("settings").remove("categoryOrder")
        root.getJSONObject("settings").remove("monthlyIncomeTargets")
        root.getJSONObject("settings").remove("homeDashboardCards")
        root.getJSONObject("settings").remove("noSpendChallengeGoalDays")
        root.getJSONObject("settings").remove("noSpendChallengeStartDate")
        val restored = FullBackupCodec.decode(root.toString())
        assertEquals(DEFAULT_CATEGORY_LABELS.keys.toList(), restored.settings?.categoryOrder)
        assertEquals(emptyMap<String, Long>(), restored.settings?.monthlyIncomeTargets)
        assertEquals(com.moasseum.app.domain.HomeDashboardCards.defaults, restored.settings?.homeDashboardCards)
        assertEquals(7, restored.settings?.noSpendChallengeGoalDays)
        assertNull(restored.settings?.noSpendChallengeStartDate)
    }
    @Test fun legacySevenCategorySettingsUpgradeToNineteenWithoutLosingOrder() {
        val root = JSONObject(FullBackupCodec.encode(backup()))
        val settings = root.getJSONObject("settings")
        val legacyKeys = listOf("FOOD", "TRANSPORT", "SHOPPING", "LIVING", "HEALTH", "LEISURE", "OTHER")
        val oldLabels = JSONObject()
        legacyKeys.forEach { key -> oldLabels.put(key, DEFAULT_CATEGORY_LABELS.getValue(key).let { if (key == "HEALTH") "건강" else it }) }
        settings.put("categoryLabels", oldLabels)
        settings.put("categoryOrder", org.json.JSONArray(legacyKeys))

        val restored = FullBackupCodec.decode(root.toString()).settings!!

        assertEquals(19, restored.categoryLabels.size)
        assertEquals("건강", restored.categoryLabels["HEALTH"])
        assertEquals(legacyKeys + DEFAULT_CATEGORY_LABELS.keys.filterNot { it in legacyKeys }, restored.categoryOrder)
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
