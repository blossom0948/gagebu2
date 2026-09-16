package com.moasseum.app

import com.moasseum.app.data.ReceiptOcr
import com.moasseum.app.domain.TransactionType
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
        assertEquals("FOOD", candidate.categoryKey)
        assertEquals(TransactionType.EXPENSE, candidate.type)
    }

    @Test
    fun `receipt parser returns no candidate if it cannot find an amount`() {
        assertNull(ReceiptOcr.candidateFromText("카페 모아씀\n감사합니다"))
    }
}
