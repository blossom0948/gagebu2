package com.moasseum.app

import com.moasseum.app.domain.TransactionType
import com.moasseum.app.notification.PaymentNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationParserTest {
    @Test
    fun numericOnlyNotificationIsNotFinancial() {
        assertFalse(PaymentNotificationParser.shouldInspectWithAi("오늘의 운세", "행운 번호 12000"))
        assertNull(PaymentNotificationParser.parse("com.example", "오늘의 운세", "행운 번호 12000", 1_000L))
    }

    @Test
    fun cardApprovalWithWonAmountIsParsed() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.example.bank",
            title = "국민카드",
            body = "홍*길동님 5,900원 승인 스타벅스 강남점",
            postedAt = 1_000L,
        )

        assertEquals(5_900L, candidate?.amount)
        assertEquals("EXPENSE", candidate?.type)
        assertEquals("스타벅스", candidate?.merchant)
    }

    @Test
    fun couponAndApprovalNumberAreNotSavedAsTransactions() {
        assertNull(PaymentNotificationParser.parse("com.shop", "결제 혜택", "3,000원 쿠폰 할인", 1_000L))
        assertNull(PaymentNotificationParser.parse("com.bank", "거래 안내", "승인번호 123456", 1_000L))
    }

    @Test
    fun aiConfirmedTransactionCanUseTransferDirectionAndAmount() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.bank",
            title = "은행 알림",
            body = "보낸 분 김*수 2026-09-17 12,000원",
            postedAt = 1_000L,
            aiConfirmedType = TransactionType.INCOME,
        )

        assertEquals(12_000L, candidate?.amount)
        assertEquals("INCOME", candidate?.type)
    }

    @Test
    fun aiConfirmationDoesNotTurnPlainDateOrNumbersIntoAnAmount() {
        assertNull(
            PaymentNotificationParser.parse(
                packageName = "com.bank",
                title = "승인 알림",
                body = "승인 2026-09-17 인증 코드 123456",
                postedAt = 1_000L,
                aiConfirmedType = TransactionType.EXPENSE,
            ),
        )
        assertNull(
            PaymentNotificationParser.parse(
                packageName = "com.bank",
                title = "은행 알림",
                body = "승인 12345678",
                postedAt = 1_000L,
                aiConfirmedType = TransactionType.EXPENSE,
            ),
        )
    }

    @Test
    fun commaGroupedAmountCanFollowTransactionActionWithoutCurrencySuffix() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.example.bank",
            title = "국민카드",
            body = "승인 5,900 스타벅스",
            postedAt = 1_000L,
        )

        assertEquals(5_900L, candidate?.amount)
    }

    @Test
    fun validApprovalSurvivesIncidentalCardSummaryWords() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.card",
            title = "카드 알림",
            body = "5,900원 승인 결제일 25일 혜택 안내",
            postedAt = 1_000L,
        )

        assertEquals(5_900L, candidate?.amount)
    }

    @Test
    fun pointsUsageIsNotACompletedPayment() {
        assertNull(
            PaymentNotificationParser.parse(
                packageName = "com.shop",
                title = "포인트 안내",
                body = "5,000원 포인트 사용",
                postedAt = 1_000L,
            ),
        )
    }

    @Test
    fun cancelledTransactionsAreAlwaysIgnored() {
        assertNull(
            PaymentNotificationParser.parse(
                "com.bank",
                "카드 승인 취소",
                "스타벅스 5,900원 결제 취소",
                1_000L,
                aiConfirmedType = TransactionType.EXPENSE,
            ),
        )
        assertTrue(PaymentNotificationParser.shouldInspectWithAi("카드 승인", "스타벅스 5,900원"))
    }
}
