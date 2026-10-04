package com.moasseum.app

import com.moasseum.app.data.PaymentCardSettings
import com.moasseum.app.domain.PaymentCard
import org.junit.Assert.*
import org.junit.Test

class PaymentCardSettingsTest {
    @Test fun `legacy three field cards stay readable`() {
        assertEquals(listOf(PaymentCard("CARD_1", "생활비 카드", 25)), PaymentCardSettings.decode("CARD_1|생활비 카드|25"))
    }

    @Test fun `new fields round trip without changing linked payment method on rename`() {
        val cards = listOf(PaymentCard("1", "새 카드 이름", 25, "기존 결제수단", 14, 0))
        assertEquals(cards, PaymentCardSettings.decode(PaymentCardSettings.encode(cards)))
    }

    @Test fun `malformed optional fields fall back and bad cards are skipped`() {
        val result = PaymentCardSettings.decode("1|정상|25||nope|nope\n2|오류|99\n3|짧음")
        assertEquals(listOf(PaymentCard("1", "정상", 25)), result)
        assertEquals(1, PaymentCardSettings.decode("1|카드|10|카드|31|0").single().periodEndMonthsBeforeDue)
    }

    @Test fun `embedded delimiters and newlines cannot corrupt settings`() {
        val card = PaymentCard("1", "생활|카드\n이름", 25, "결제\r수단")
        val result = PaymentCardSettings.decode(PaymentCardSettings.encode(listOf(card))).single()
        assertEquals(card, result)
    }
}
