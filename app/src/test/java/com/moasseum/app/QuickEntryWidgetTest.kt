package com.moasseum.app

import com.moasseum.app.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickEntryWidgetTest {
    @Test
    fun `widget accepts only expense and income routes`() {
        assertEquals(TransactionType.EXPENSE, widgetEntryType("EXPENSE"))
        assertEquals(TransactionType.INCOME, widgetEntryType("INCOME"))
        assertNull(widgetEntryType("TRANSFER"))
        assertNull(widgetEntryType(null))
    }
}
