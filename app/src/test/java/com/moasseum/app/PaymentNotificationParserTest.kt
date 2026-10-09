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
    fun kakaoConversationIsExcludedEvenWhenAiClaimsItIsFinancial() {
        listOf("1000원 입금했어", "결제 완료 1,000원", "급여 300만원 받았어요").forEach { body ->
            assertNull(PaymentNotificationParser.parse("com.kakao.talk", "오픈채팅", body, 1_000L))
            assertNull(PaymentNotificationParser.parse("com.kakao.talk", "친구", body, 1_000L, TransactionType.INCOME))
        }
    }

    @Test
    fun kakaoPayAppRemainsEligibleForFinancialDetection() {
        assertEquals(1_000L, PaymentNotificationParser.parse("com.kakaopay.app", "카카오페이", "결제 완료 1,000원", 1_000L)?.amount)
    }

    @Test
    fun numericOnlyNotificationIsNotFinancial() {
        assertFalse(PaymentNotificationParser.shouldInspectWithAi("오늘의 운세", "행운 번호 12000"))
        assertNull(PaymentNotificationParser.parse("com.example", "오늘의 운세", "행운 번호 12000", 1_000L))
    }

    @Test
    fun genericUserOrUsageTextWithMoneyDoesNotBecomeATransactionEvenWhenAiIsEnabled() {
        val body = "사용자 안내: 이용 방법을 확인하세요. 참고 금액 12,000원"
        assertFalse(PaymentNotificationParser.shouldInspectWithAi("서비스 안내", body))
        assertNull(PaymentNotificationParser.parse("com.example", "서비스 안내", body, 1_000L))
        assertNull(PaymentNotificationParser.parse("com.example", "서비스 안내", body, 1_000L, TransactionType.EXPENSE))
    }

    @Test
    fun paymentInstructionsWithAnAmountAreNotTreatedAsCompletedTransactions() {
        val body = "결제 방법 안내: 상품권 1,000원부터 선택할 수 있어요"
        assertFalse(PaymentNotificationParser.shouldInspectWithAi("서비스 안내", body))
        assertNull(PaymentNotificationParser.parse("com.example.store", "서비스 안내", body, 1_000L))
        assertNull(PaymentNotificationParser.parse("com.example.store", "서비스 안내", body, 1_000L, TransactionType.EXPENSE))
    }

    @Test
    fun messagingNotificationWithIncidentalNumbersIsNotFinancial() {
        val body = "이모티콘을 보냈습니다. 1234 타이밍 맞게 불꽃놀이 1698이요ㅎㅎ 간절한 1700에."

        assertFalse(PaymentNotificationParser.shouldInspectWithAi("쫑알메이113", body))
        assertNull(PaymentNotificationParser.parse("com.kakao.talk", "쫑알메이113", body, 1_000L))
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
        assertEquals("CAFE", candidate?.categoryKey)
    }

    @Test
    fun supermarketPaymentIsCategorizedAsFoodInsteadOfOther() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.example.bank",
            title = "국민카드 승인",
            body = "12,300원 이마트 결제",
            postedAt = 1_000L,
        )
        assertEquals("FOOD", candidate?.categoryKey)
        assertEquals("FOOD", PaymentNotificationParser.inferCategoryFrom("홈플러스 21,000원 승인"))
        assertEquals("FOOD", PaymentNotificationParser.inferCategoryFrom("롯데마트 21,000원 승인"))
    }

    @Test
    fun couponAndApprovalNumberAreNotSavedAsTransactions() {
        assertNull(PaymentNotificationParser.parse("com.shop", "결제 혜택", "3,000원 쿠폰 할인", 1_000L))
        assertNull(PaymentNotificationParser.parse("com.bank", "거래 안내", "승인번호 123456", 1_000L))
    }

    @Test
    fun promotionOnlyAmountsAreRejectedButAnExplicitApprovalUsesTheApprovedAmount() {
        assertNull(
            PaymentNotificationParser.parse(
                "com.shop", "결제 안내", "쿠폰 3,000원 할인 혜택이 적용되었어요. 결제가 완료되었습니다.", 1_000L,
            ),
        )
        val candidate = PaymentNotificationParser.parse(
            "com.bank", "카드 알림", "쿠폰 할인 3,000원 · 실제 승인 5,900원 스타벅스", 1_000L,
        )
        assertEquals(5_900L, candidate?.amount)
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
    fun incomeAndWithdrawalAreClassifiedFromExplicitBankActions() {
        val incoming = PaymentNotificationParser.parse("com.bank", "입금 알림", "급여 2,500,000원 입금", 1_000L)
        val outgoing = PaymentNotificationParser.parse("com.bank", "출금 알림", "계좌에서 30,000원 출금", 1_000L)
        assertEquals("INCOME", incoming?.type)
        assertEquals("EXPENSE", outgoing?.type)
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
        assertTrue(PaymentNotificationParser.shouldInspectWithAi("국민카드", "승인 5,900 스타벅스"))
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
