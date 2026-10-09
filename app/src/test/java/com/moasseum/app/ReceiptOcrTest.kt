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
    fun `toss calendar totals weekdays and own account transfers are not imported as purchases`() {
        val candidates = PhotoTransactionImport.extractWithReceiptFallback(
            """
            ← 10
            전체 카드 입출금 페이기타
            일 월 화 수 목 금 토
            4 5 6 7 8 9 10
            -14,550 -10,700 -3,000 +500 +10,000 +6,000
            -18,000 -15,000 -6,750
            8건
            확인하기
            -5,500%!
            부안 마실영화관| 토스뱅크 체크카드
            -1,250
            내토스뱅크계좌 김민수
            6,000
            박서준 내 토스뱅크계좌
            8일 목요일
            10,000
            이서연 내 토스뱅크 통장
            15,0002l
            내토스뱅크 통장 토스정승화
            6,000
            김지은 내 토스뱅크계좌
            7일 수요일 평소보다 많이 쓸
            500원
            카드캐시백 내 토스뱅크 통장
            -18,0002l
            dtryx 디트릭스토스뱅크 체크카드
            캐시백 500
            6일화요일
            -3,000
            하이푸드토스뱅크 체크카드
            5일 월요일
            -8,4002l
            버거킹|토스뱅크 체크카드
            -2,300
            CU)
            CU토스뱅크 체크카드
            4일 일요일
            -120,000
            내 계좌 이체 | 아이통장 토스뱅크
            -120,000
            내 계좌 이체 | 토스뱅크 통장 아이통장
            """.trimIndent(),
            today = LocalDate.of(2026, 10, 10),
        )

        assertEquals(candidates.joinToString { "${it.transaction.amount}:${it.transaction.merchant}:${it.transaction.occurredDate}" }, 11, candidates.size)
        assertEquals(
            listOf(5_500L, 1_250L, 6_000L, 10_000L, 15_000L, 6_000L, 500L, 18_000L, 3_000L, 8_400L, 2_300L),
            candidates.map { it.transaction.amount },
        )
        assertEquals("부안 마실영화관", candidates[0].transaction.merchant)
        assertEquals(LocalDate.of(2026, 10, 9), candidates[0].transaction.occurredDate)
        assertEquals(TransactionType.EXPENSE, candidates[1].transaction.type)
        assertEquals("김민수", candidates[1].transaction.merchant)
        assertEquals(TransactionType.INCOME, candidates[2].transaction.type)
        assertEquals("박서준", candidates[2].transaction.merchant)
        assertEquals(LocalDate.of(2026, 10, 8), candidates[3].transaction.occurredDate)
        assertEquals(LocalDate.of(2026, 10, 8), candidates[5].transaction.occurredDate)
        assertEquals("정승화", candidates[4].transaction.merchant)
        assertEquals(TransactionType.INCOME, candidates[6].transaction.type)
        assertEquals("카드캐시백", candidates[6].transaction.merchant)
        assertEquals("디트릭스", candidates[7].transaction.merchant)
        assertEquals("LEISURE", candidates[7].transaction.categoryKey)
        assertEquals("FOOD", candidates[8].transaction.categoryKey)
        assertEquals("버거킹", candidates[9].transaction.merchant)
        assertEquals("CU", candidates[10].transaction.merchant)
    }

    @Test
    fun `overlapping screenshot rows deduplicate even when OCR varies punctuation`() {
        val first = PhotoTransactionImport.extract(
            "← 10\n7일 수요일\n500원\n카드캐시백 내 토스뱅크 통장\n-18,000\nd.tryx 디트릭스토스뱅크 체크카드",
            today = LocalDate.of(2026, 10, 9),
        ).map { it.copy(sourceImageId = "capture-a") }
        val second = PhotoTransactionImport.extract(
            "← 10\n7일 수요일\n500원\n카드캐시백 내 토스뱅크 통장\n-18,000\ndtryx 디트릭스토스뱅크 체크카드",
            today = LocalDate.of(2026, 10, 9),
        ).map { it.copy(sourceImageId = "capture-b") }

        val preview = PhotoTransactionImport.preview(first + second, emptyList())

        assertEquals(2, preview.candidates.size)
        assertEquals(2, preview.duplicateCount)
    }

    @Test
    fun `unsigned transfer rows use the nearest bank account direction`() {
        val candidates = PhotoTransactionImport.extract(
            "← 10\n6,000\n박서준 내 토스뱅크계좌\n8일 목요일\n10,000\n이서연 내 토스뱅크 통장",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(candidates.joinToString { "${it.transaction.amount}:${it.transaction.merchant}:${it.transaction.type}" }, 2, candidates.size)
    }

    @Test
    fun `ocr icon noise after an amount does not change the amount or lose the transaction`() {
        val candidates = PhotoTransactionImport.extract(
            "← 10\n8일 목요일\n15,0002l\n내 토스뱅크 통장 토스정승화\n6,000\n송민영 내 토스뱅크계좌\n7일 수요일",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(candidates.joinToString { "${it.transaction.amount}:${it.transaction.merchant}:${it.transaction.type}" }, 2, candidates.size)
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

    @Test
    fun `ordinary menu prices without transaction context are ignored`() {
        val candidates = PhotoTransactionImport.extractWithReceiptFallback(
            "추천 메뉴\n아메리카노 5,500원\n케이크 7,000원\n쿠폰 할인 2,000원",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(0, candidates.size)
    }

    @Test
    fun `a transaction history heading alone does not turn menu prices into transactions`() {
        val candidates = PhotoTransactionImport.extract(
            "거래내역\n아메리카노 5,500원\n케이크 7,000원",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(0, candidates.size)
    }

    @Test
    fun `payment brand before merchant is stripped from local merchant recognition`() {
        val candidate = PhotoTransactionImport.extract(
            "카드 이용내역\n10월 9일\n신한카드 스타벅스 강남점\n-4,500원",
            today = LocalDate.of(2026, 10, 9),
        ).single()

        assertEquals("스타벅스 강남점", candidate.transaction.merchant)
        assertEquals("신한카드", candidate.paymentMethod)
    }

    @Test
    fun `unsigned amount on a dated bank row requires type review`() {
        val candidates = PhotoTransactionImport.extract(
            "카드 이용내역\n10월 9일\n스타벅스 강남점\n5,500원",
            today = LocalDate.of(2026, 10, 9),
        )

        assertEquals(1, candidates.size)
        assertEquals("스타벅스 강남점", candidates.single().transaction.merchant)
        assertEquals(5_500L, candidates.single().transaction.amount)
        assert(candidates.single().transaction.needsConfirmation.contains("type"))
    }

    @Test
    fun `split cancellation subtitle excludes only its own payment row`() {
        val candidates = PhotoTransactionImport.extract(
            """
            10월 2일 금요일
            -5,000원
            취소 | 카카오T | 토스뱅크 체크카드
            -3,700원
            메가커피 | 토스뱅크 체크카드
            """.trimIndent(),
            today = LocalDate.of(2026, 10, 10),
        )

        assertEquals(1, candidates.size)
        assertEquals("메가커피", candidates.single().transaction.merchant)
        assertEquals(3_700L, candidates.single().transaction.amount)
    }

    @Test
    fun `noisy Baedal OCR aliases canonicalize but ambiguous transfer names require review`() {
        val candidates = PhotoTransactionImport.extract(
            """
            10월 2일 금요일
            -2,650원
            바배달의만족 리센느메이의시나몬롤 | 토스뱅크 체크카드
            50,000원
            정회진 내 토스뱅크계좌
            """.trimIndent(),
            today = LocalDate.of(2026, 10, 10),
        )

        assertEquals(2, candidates.size)
        val delivery = candidates.single { it.transaction.amount == 2_650L }
        assertEquals("배달의민족", delivery.transaction.merchant)
        assertEquals("FOOD", delivery.transaction.categoryKey)
        val transfer = candidates.single { it.transaction.amount == 50_000L }
        assertEquals("정회진", transfer.transaction.merchant)
        assert(transfer.transaction.needsConfirmation.contains("merchant"))
    }

    @Test
    fun `lottery credit is not mistaken for a bank transfer and its category needs review`() {
        val candidate = PhotoTransactionImport.extract(
            "거래내역\n10월 1일\n30원\n체크카드 복권 당첨 내 토스뱅크 통장",
            today = LocalDate.of(2026, 10, 10),
        ).single()

        assertEquals(TransactionType.INCOME, candidate.transaction.type)
        assertEquals("복권 당첨", candidate.transaction.merchant)
        assertEquals("OTHER", candidate.transaction.categoryKey)
        assert(candidate.transaction.needsConfirmation.contains("category"))
    }

    @Test
    fun `three Fold screenshots keep October dates merchant names and only unique completed transactions`() {
        val today = LocalDate.of(2026, 10, 10)
        val captureOne = PhotoTransactionImport.extract(
            """
            10
            전체 카드 입출금 페이기타
            토
            27 28 29 30 1 2 3
            4150500 51250 43000 493 +50000
            -16450 -44080 -14411 14700
            -5,000!
            취소|카카오T|토스뱅크 체크카드
            50,000%!
            정회진내 토스뱅크계좌
            1일 목요일
            배탈민족 -2,650%
            배달의민족리센느메이의시나몬롤외 1개|토스뱅크 체크카드
            30원
            체크카드 복권 당첨 내 토스뱅크 통장
            -2,000
            CU토스뱅크 체크카드
            -1,700
            하이푸드 |토스뱅크 체크카드
            63원
            통장 이자 내 토스뱅크 통장
            N -8,061
            네이버페이토스뱅크 체크카드
            9월
            30일 수요일
            """.trimIndent(),
            today = today,
        ).map { it.copy(sourceImageId = "10281") }
        val captureTwo = PhotoTransactionImport.extract(
            """
            10
            전체 카드 입출금 페이기타
            일 월 화 수 목 토
            4 5 6 7 10
            -14,550 -10700 -3,000 +500 +10,000 +6,000
            -18,000 -15,000 -6,750
            4일 일요일
            -120,000
            내계좌 이체 |아이통장 토스뱅크
            -120,000
            내 계좌 이체 | 토스뱅크 통장 아이통장
            -14,550
            네이버페이토스뱅크 체크카드
            2일금요일
            -500
            내토스뱅크 통장 카드 캐시백 취소
            52,800%
            토스뱅크카드 취소| 쿠팡(쿠페이) 나이스
            -3,700
            메가커피|토스뱅크 체크카드
            -6,000%
            소문마라탕|토스뱅크 체크카드
            -5,000%!
            카카오T|토스뱅크 체크카드
            1,300
            송민영 내 토스뱅크계좌
            """.trimIndent(),
            today = today,
        ).map { it.copy(sourceImageId = "10279") }
        val captureThree = PhotoTransactionImport.extract(
            """
            10
            전체 카드 입출금 페이기타
            토
            27 28 29 30 1 2 3
            4708 4150500 41250 43000 493 +50000
            735868 -16450 -44080 14411 -14700
            내토스뱅크 통장 카드 캐시백 취소
            52,800
            토스뱅크카드 취소 |쿠팡(쿠페이)나이스
            -3,700
            메가커피| 토스뱅크 체크카드
            -6,000
            소문마라탕|토스뱅크 체크카드
            -5,000
            카카오T|토스뱅크 체크카드
            1,300!
            송민영 내 토스뱅크계좌
            -5,000!
            취소|카카오T | 토스뱅크 체크카드
            50,000
            정회진 내 토스뱅크계좌
            1일 목요일
            -2,650
            배달의민족
            배달의민족리센느메이의시나몬롤외 1개 |토스뱅크 체크카드
            30원
            체크카드 복권 당첨 내토스뱅크 통장
            """.trimIndent(),
            today = today,
        ).map { it.copy(sourceImageId = "10283") }

        val preview = PhotoTransactionImport.preview(captureOne + captureTwo + captureThree, emptyList())
        assertEquals(12, preview.candidates.size)
        val actual = preview.candidates.map {
            "${it.transaction.occurredDate}|${it.transaction.type}|${it.transaction.amount}|${it.transaction.merchant}"
        }.toSet()

        assertEquals(7, preview.duplicateCount)
        assertEquals(
            setOf(
                "2026-10-02|INCOME|50000|정회진",
                "2026-10-01|EXPENSE|2650|배달의민족",
                "2026-10-01|INCOME|30|복권 당첨",
                "2026-10-01|EXPENSE|2000|CU",
                "2026-10-01|EXPENSE|1700|하이푸드",
                "2026-10-01|INCOME|63|통장 이자",
                "2026-10-01|EXPENSE|8061|네이버페이",
                "2026-10-04|EXPENSE|14550|네이버페이",
                "2026-10-02|EXPENSE|3700|메가커피",
                "2026-10-02|EXPENSE|6000|소문마라탕",
                "2026-10-02|EXPENSE|5000|카카오T",
                "2026-10-02|INCOME|1300|송민영",
            ),
            actual,
        )
        assertEquals("FOOD", preview.candidates.single { it.transaction.merchant == "소문마라탕" }.transaction.categoryKey)
        assertEquals("FOOD", preview.candidates.single { it.transaction.merchant == "배달의민족" }.transaction.categoryKey)
        assert(preview.candidates.single { it.transaction.amount == 8_061L }.transaction.needsConfirmation.contains("category"))
        assert(preview.candidates.single { it.transaction.merchant == "정회진" }.transaction.needsConfirmation.contains("merchant"))
    }
}
