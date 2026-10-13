package com.moasseum.app

import android.content.Context
import android.content.pm.ShortcutManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickEntryShortcutManifestTest {
    @Test
    fun launcherLongPressOffersOnlyExpenseAndIncomeQuickEntry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue("Launcher shortcut checks are restricted to the isolated QA app.", context.packageName.endsWith(".qa"))
        val shortcutManager = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
        val shortcuts = shortcutManager.manifestShortcuts.associateBy { it.id }

        assertEquals(setOf("quick_expense", "quick_income"), shortcuts.keys)
        val expense = shortcuts.getValue("quick_expense")
        val expenseIntent = requireNotNull(expense.intent)
        assertEquals(context.getString(R.string.quick_add_expense_shortcut), expense.shortLabel.toString())
        assertEquals(ACTION_WIDGET_QUICK_ADD, expenseIntent.action)
        assertEquals("moasseum://shortcut/quick-add/expense", expenseIntent.dataString)
        assertNotNull(expenseIntent.component)
        assertEquals(context.packageName, expenseIntent.component?.packageName)

        val income = shortcuts.getValue("quick_income")
        val incomeIntent = requireNotNull(income.intent)
        assertEquals(context.getString(R.string.quick_add_income_shortcut), income.shortLabel.toString())
        assertEquals(ACTION_WIDGET_QUICK_ADD, incomeIntent.action)
        assertEquals("moasseum://shortcut/quick-add/income", incomeIntent.dataString)
        assertEquals(context.packageName, incomeIntent.component?.packageName)
    }
}
