package com.moasseum.app

import com.moasseum.app.data.ReceiptOcr
import com.moasseum.app.data.PhotoTransactionImport
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.Transaction
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReceiptOcrTest {
    @Test
    fun `receipt text prefers labeled total and recognizes merchant and date`() {
        val candidate = ReceiptOcr.candidateFromText(
            "카페 모아씀\n2026.09.17 14:30\n아메리카노 4,500\n합계 4,500원\n승인번호 123456",
            today = LocalDate.of(2026, 9, 17),
        )

        assertNotNull(candidate)
        assertEquals(4_500L, candidate!!.amount)
        assertEquals("카페 모아씀", candidate.merchant)
        assertEquals(LocalDate.of(2026, 9, 17), candidate.occurredDate)
        assertEquals("CAFE", candidate.categoryKey)
        assertEquals(TransactionType.EXPENSE, candidate.type)
    }

    @Test
    fun `receipt parser returns no candidate if it cannot find an amount`() {
        assertNull(ReceiptOcr.candidateFromText("카페 모아씀\n감사합니다"))
    }

    @Test
    fun `finance screenshot extracts every dated expense and deposit`() {
        val candidates = PhotoTransactionImport.extract(
            """
            10월 9일
            스타벅스 강남점
            신한카드
            -4,500원
            10월 8일
            쿠팡
            -19,900원
            10월 8일
            급여 입금
            +2,000,000원
            """.trimIndent(),
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(3, candidates.size)
        assertEquals("스타벅스 강남점", candidates[0].transaction.merchant)
        assertEquals(4_500L, candidates[0].transaction.amount)
        assertEquals(LocalDate.of(2026, 10, 9), candidates[0].transaction.occurredDate)
        assertEquals("신한카드", candidates[0].paymentMethod)
        assertEquals("쿠팡", candidates[1].transaction.merchant)
        assertEquals(TransactionType.EXPENSE, candidates[1].transaction.type)
        assertEquals(LocalDate.of(2026, 10, 8), candidates[1].transaction.occurredDate)
        assertEquals(TransactionType.INCOME, candidates[2].transaction.type)
        assertEquals(2_000_000L, candidates[2].transaction.amount)
    }

    @Test
    fun `photo import excludes existing and repeated captured transactions`() {
        val candidate = PhotoTransactionImport.extract(
            "10월 9일\n스타벅스 강남점\n-4,500원",
            today = LocalDate.of(2026, 10, 9),
        ).single()
        val existing = Transaction(
            id = 1,
            type = TransactionType.EXPENSE,
            amount = 4_500,
            occurredAt = LocalDate.of(2026, 10, 9).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            categoryKey = "CAFE",
            merchant = "스타벅스",
            memo = "",
            paymentMethod = "카드",
            source = "MANUAL",
        )

        val preview = PhotoTransactionImport.preview(listOf(candidate), listOf(existing))

        assertEquals(0, preview.candidates.size)
        assertEquals(1, preview.duplicateCount)

        val overlap = PhotoTransactionImport.preview(
            listOf(candidate.copy(sourceImageId = "screen-a"), candidate.copy(sourceImageId = "screen-b")),
            emptyList(),
        )
        assertEquals(1, overlap.candidates.size)
        assertEquals(1, overlap.duplicateCount)
    }

    @Test
    fun `multi photo preview keeps all new rows and excludes existing and cross photo duplicates`() {
        val firstCapture = PhotoTransactionImport.extract(
            "10월 9일\n스타벅스 강남점\n-4,500원\n10월 8일\n쿠팡\n-19,900원",
            today = LocalDate.of(2026, 10, 9),
        ).map { it.copy(sourceImageId = "bank-a") }
        val secondCapture = PhotoTransactionImport.extract(
            "10월 9일\n스타벅스\n-4,500원\n10월 7일\n올리브영\n-30,000원",
            today = LocalDate.of(2026, 10, 9),
        ).map { it.copy(sourceImageId = "bank-b") }
        val existing = Transaction(
            id = 9,
            type = TransactionType.EXPENSE,
            amount = 19_900,
            occurredAt = LocalDate.of(2026, 10, 8).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            categoryKey = "SHOPPING",
            merchant = "쿠팡",
            memo = "",
            paymentMethod = "카드",
            source = "MANUAL",
        )

        val preview = PhotoTransactionImport.preview(firstCapture + secondCapture, listOf(existing))

        assertEquals(2, preview.candidates.size)
        assertEquals(listOf("스타벅스 강남점", "올리브영"), preview.candidates.map { it.transaction.merchant })
        assertEquals(2, preview.duplicateCount)
    }

    @Test
    fun `screenshot parser ignores balances and cancelled amounts`() {
        val candidates = PhotoTransactionImport.extract(
            "10.09\n스타벅스\n-4,500원\n잔액 100,000원\n결제 취소 8,000원\n월간 지출 200,000원",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(1, candidates.size)
        assertEquals(4_500L, candidates.single().transaction.amount)
    }

    @Test
    fun `screenshot parser honors explicit signs and ignores reference numbers`() {
        val candidates = PhotoTransactionImport.extract(
            "계좌번호 1234-5678-9012\n카페\n+1,000원\n편의점\n-1,000원",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(2, candidates.size)
        assertEquals(TransactionType.INCOME, candidates[0].transaction.type)
        assertEquals(TransactionType.EXPENSE, candidates[1].transaction.type)
        assertEquals("카페", candidates[0].transaction.merchant)
        assertEquals("편의점", candidates[1].transaction.merchant)
    }

    @Test
    fun `finance screenshot with a monthly summary keeps every transaction row`() {
        val candidates = PhotoTransactionImport.extractWithReceiptFallback(
            "이번 달 지출 24,400원\n10월 9일 스타벅스 강남점 -4,500원\n10월 8일 쿠팡 -19,900원",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(2, candidates.size)
        assertEquals(4_500L, candidates[0].transaction.amount)
        assertEquals(19_900L, candidates[1].transaction.amount)
    }

    @Test
    fun `receipt summary is not treated as a finance transaction without receipt details`() {
        val candidates = PhotoTransactionImport.extractWithReceiptFallback(
            "이번 달 총 지출 24,400원\n월간 합계 24,400원",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(0, candidates.size)
    }
}
