package com.moasseum.app

import com.moasseum.app.data.BatchCommandAction
import com.moasseum.app.data.BatchCommandParser
import com.moasseum.app.data.BatchCommandPlan
import com.moasseum.app.data.BatchEditValues
import com.moasseum.app.data.BatchTarget
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BatchCommandParserTest {
    private val today = LocalDate.of(2026, 10, 8)

    @Test fun splitsMultipleAddsWithoutSplittingThousands() {
        val plan = BatchCommandParser.parse("점심 8천원 그리고 커피 4,500원", emptyList(), today) as BatchCommandPlan.Add

        assertEquals(2, plan.candidates.size)
        assertEquals(listOf(8_000L, 4_500L), plan.candidates.map { it.amount })
        assertEquals(listOf("FOOD", "CAFE"), plan.candidates.map { it.categoryKey })
        assertEquals("점심", plan.candidates.first().merchant)
    }

    @Test fun deletePreviewMatchesOnlyPrivateRowsAndLeavesFinalSelectionToUser() {
        val todayCafe = transaction(1, 4_500, "개인 카페", "CAFE", today)
        val yesterdayCafe = transaction(2, 4_500, "어제 카페", "CAFE", today.minusDays(1))
        val sharedCafe = transaction(3, 4_500, "파트너 카페", "CAFE", today, ledger = "shared:ledger", scope = "SHARED")
        val installment = transaction(4, 4_500, "할부 카페", "CAFE", today, installment = "group")

        val plan = BatchCommandParser.parse("삭제: 오늘 카페 지출", listOf(todayCafe, yesterdayCafe, sharedCafe, installment), today)
            as BatchCommandPlan.Change

        assertEquals(BatchCommandAction.DELETE, plan.action)
        assertEquals(listOf(1L), plan.matches.map { it.id })
    }

    @Test fun editPlanRequiresArrowAndChangesOnlyExplicitFields() {
        val row = transaction(1, 4_500, "카페", "CAFE", today)
        val plan = BatchCommandParser.parse("수정: 카페 4,500원 -> 금액 5,000원", listOf(row), today)
            as BatchCommandPlan.Change

        assertEquals(BatchCommandAction.UPDATE, plan.action)
        assertEquals(listOf(1L), plan.matches.map { it.id })
        assertEquals(5_000L, plan.edit?.amount)
        assertNull(plan.edit?.merchant)
        assertNull(plan.edit?.categoryKey)
    }

    @Test fun editCanPreviewTypeDateAndMemoChanges() {
        val row = transaction(1, 4_500, "카페", "CAFE", today)
        val plan = BatchCommandParser.parse(
            "수정: 카페 4,500원 -> 유형=수입, 날짜=2026-10-07, 메모=환불",
            listOf(row), today,
        ) as BatchCommandPlan.Change

        assertEquals(TransactionType.INCOME, plan.edit?.type)
        assertEquals(today.minusDays(1), plan.edit?.occurredDate)
        assertEquals("환불", plan.edit?.memo)
    }

    @Test fun broadDeleteWithoutTargetIsRejected() {
        val row = transaction(1, 4_500, "카페", "CAFE", today)
        val error = runCatching { BatchCommandParser.parse("삭제: 지출", listOf(row), today) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test fun aiQueryPlanRunsOnlyAgainstEligibleLocalRows() {
        val privateRow = transaction(1, 4_500, "스타벅스", "CAFE", today)
        val otherMerchant = transaction(2, 4_500, "개인 카페", "CAFE", today)
        val sharedRow = transaction(3, 4_500, "스타벅스", "CAFE", today, ledger = "shared:ledger", scope = "SHARED")
        val plan = BatchCommandParser.previewChange(
            action = BatchCommandAction.UPDATE,
            target = BatchTarget(
                type = TransactionType.EXPENSE,
                categoryKey = "CAFE",
                merchantTokens = listOf("스타벅스"),
                from = today,
                through = today,
            ),
            edit = BatchEditValues(amount = 5_000),
            transactions = listOf(privateRow, otherMerchant, sharedRow),
        )

        assertEquals(listOf(1L), plan.matches.map { it.id })
        assertEquals(5_000L, plan.edit?.amount)
    }

    @Test fun aiPlanRejectsUnboundedSearchAndTransferEdits() {
        val unbounded = runCatching {
            BatchCommandParser.previewChange(BatchCommandAction.DELETE, BatchTarget(), null, emptyList())
        }.exceptionOrNull()
        assertTrue(unbounded is IllegalArgumentException)
        val transferEdit = runCatching {
            BatchCommandParser.previewChange(
                BatchCommandAction.UPDATE,
                BatchTarget(merchantTokens = listOf("스타벅스")),
                BatchEditValues(type = TransactionType.TRANSFER),
                emptyList(),
            )
        }.exceptionOrNull()
        assertTrue(transferEdit is IllegalArgumentException)
    }

    @Test fun exactIsoDateDoesNotBecomeAnAmountFilter() {
        val row = transaction(1, 4_500, "커피", "CAFE", today)
        val plan = BatchCommandParser.parse("삭제: 2026-10-08", listOf(row), today) as BatchCommandPlan.Change
        assertEquals(listOf(1L), plan.matches.map { it.id })
    }

    @Test fun addLimitAndMissingCandidateAreRejected() {
        val missing = runCatching { BatchCommandParser.parse("커피", emptyList(), today) }.exceptionOrNull()
        assertTrue(missing is IllegalArgumentException)
        val tooMany = (1..31).joinToString(" 그리고 ") { "점심 ${it + 1000}원" }
        val limitError = runCatching { BatchCommandParser.parse(tooMany, emptyList(), today) }.exceptionOrNull()
        assertTrue(limitError is IllegalArgumentException)
    }

    private fun transaction(
        id: Long,
        amount: Long,
        merchant: String,
        category: String,
        date: LocalDate,
        ledger: String = "personal",
        scope: String = "PRIVATE",
        installment: String? = null,
    ) = Transaction(
        id = id,
        type = TransactionType.EXPENSE,
        amount = amount,
        occurredAt = date.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
        categoryKey = category,
        merchant = merchant,
        memo = "",
        paymentMethod = "카드",
        source = "MANUAL",
        timezone = "Asia/Seoul",
        ledgerId = ledger,
        sharingScope = scope,
        installmentGroupId = installment,
    )
}
