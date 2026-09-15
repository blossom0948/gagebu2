package com.moasseum.app

import com.moasseum.app.notification.PaymentNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentNotificationParserTest {
    @Test
    fun `payment notification becomes an expense candidate`() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.example.card",
            title = "카드 승인",
            body = "스타벅스 5,500원 승인",
            postedAt = 1_726_400_000_000L,
        )

        assertNotNull(candidate)
        assertEquals(5_500L, candidate?.amount)
        assertEquals("EXPENSE", candidate?.type)
        assertEquals("FOOD", candidate?.categoryKey)
        assertEquals("스타벅스", candidate?.merchant)
    }

    @Test
    fun `income notification becomes an income candidate`() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.example.bank",
            title = "입금 알림",
            body = "급여 2,500,000원 입금",
            postedAt = 1_726_400_000_000L,
        )

        assertEquals("INCOME", candidate?.type)
        assertEquals(2_500_000L, candidate?.amount)
    }

    @Test
    fun `cancelled notification is ignored`() {
        val candidate = PaymentNotificationParser.parse(
            packageName = "com.example.card",
            title = "결제 취소",
            body = "카페 5,500원 취소",
            postedAt = 1_726_400_000_000L,
        )

        assertNull(candidate)
    }
}
