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

    @Test
    fun `quick entry intent accepts exact widget and launcher shortcut routes`() {
        assertEquals(
            TransactionType.EXPENSE,
            quickEntryTypeFromIntent(ACTION_WIDGET_QUICK_ADD, null, "moasseum://shortcut/quick-add/expense"),
        )
        assertEquals(
            TransactionType.INCOME,
            quickEntryTypeFromIntent(ACTION_WIDGET_QUICK_ADD, null, "moasseum://shortcut/quick-add/income"),
        )
        assertEquals(
            TransactionType.EXPENSE,
            quickEntryTypeFromIntent(ACTION_WIDGET_QUICK_ADD, "expense", "moasseum://widget/quick-add/income"),
        )
        assertNull(quickEntryTypeFromIntent("other.action", "EXPENSE", "moasseum://shortcut/quick-add/expense"))
        assertNull(quickEntryTypeFromIntent(ACTION_WIDGET_QUICK_ADD, null, "moasseum://shortcut/quick-add/transfer"))
        assertNull(quickEntryTypeFromIntent(ACTION_WIDGET_QUICK_ADD, null, "moasseum://shortcut/quick-add/anything/expense"))
        assertNull(quickEntryTypeFromIntent(ACTION_WIDGET_QUICK_ADD, null, "moasseum://shortcut/quick-add/expense?mode=bad"))
    }
}
