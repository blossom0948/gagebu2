package com.moasseum.app

import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.parseQuickEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickEntryParserTest {
    @Test fun parsesPlainAmountAndKeepsMerchant() {
        val result = requireNotNull(parseQuickEntry("점심 8,000원"))
        assertEquals(8_000L, result.amount)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("점심", result.merchant)
    }

    @Test fun parsesKoreanUnitsAndIncomeWordsLocally() {
        val expense = requireNotNull(parseQuickEntry("오늘 카페 8천원"))
        assertEquals(8_000L, expense.amount)
        assertEquals("카페", expense.merchant)
        val income = requireNotNull(parseQuickEntry("친구 정산받음 1.2만원"))
        assertEquals(12_000L, income.amount)
        assertEquals(TransactionType.INCOME, income.type)
        assertEquals("친구", income.merchant)
    }

    @Test fun rejectsMissingAmountMerchantAndOutOfRangeValues() {
        assertNull(parseQuickEntry("점심 먹음"))
        assertNull(parseQuickEntry("8천원"))
        assertNull(parseQuickEntry("가게 999999999999999999999원"))
    }
}
