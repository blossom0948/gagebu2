package com.moasseum.app

import com.moasseum.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class AccountsAndBudgetTest {
    private fun tx(type: TransactionType, amount: Long, date: String = "2026-10-04", from: String? = "A", to: String? = null) =
        Transaction(1, type, amount, LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), "OTHER", "테스트", "", "카드", "MANUAL", from, to)

    @Test fun transferPreservesTotalAssetsAndIsNotIncomeOrExpense() {
        val a = Account("A", "은행", 10000); val b = Account("B", "현금", 3000)
        val rows = listOf(tx(TransactionType.TRANSFER, 2000, to = "B"))
        assertEquals(8000, accountBalance(a, rows)); assertEquals(5000, accountBalance(b, rows))
        assertEquals(13000, accountBalance(a, rows) + accountBalance(b, rows))
        val state = LedgerUiState(YearMonth.of(2026, 10), rows)
        assertEquals(0, state.expenseTotal); assertEquals(0, state.incomeTotal)
    }
    @Test fun balancesIncludeOnlyLinkedTransactionsAndArchivedAccountsKeepBalance() {
        val rows = listOf(tx(TransactionType.EXPENSE, 1000), tx(TransactionType.INCOME, 2000), tx(TransactionType.EXPENSE, 9999, from = null))
        assertEquals(11000, accountBalance(Account("A", "계좌", 10000, archived = true), rows))
    }
    @Test fun removingAndEditingTransferRecalculatesBothAccounts() {
        val a = Account("A", "A", 10000); val b = Account("B", "B", 0)
        assertEquals(7000, accountBalance(a, listOf(tx(TransactionType.TRANSFER, 3000, to = "B"))))
        assertEquals(3000, accountBalance(b, listOf(tx(TransactionType.TRANSFER, 3000, to = "B"))))
        assertEquals(10000, accountBalance(a, emptyList())); assertEquals(0, accountBalance(b, emptyList()))
    }
    @Test fun rolloverChainsAcrossAdjacentMonthsAndResetsAtGap() {
        val plans = listOf(BudgetPlan(YearMonth.of(2026, 8), 10000, true), BudgetPlan(YearMonth.of(2026, 9), 10000, true), BudgetPlan(YearMonth.of(2026, 10), 10000, false))
        val rows = listOf(tx(TransactionType.EXPENSE, 8000, "2026-08-01"), tx(TransactionType.EXPENSE, 11000, "2026-09-01"), tx(TransactionType.TRANSFER, 9999, "2026-09-02", to = "B"))
        assertEquals(11000, effectiveBudget(YearMonth.of(2026, 10), plans, rows)!!.total)
        assertEquals(10000, effectiveBudget(YearMonth.of(2026, 10), plans.filterNot { it.month.monthValue == 9 }, rows)!!.total)
    }
    @Test fun overBudgetDoesNotCarryDebtAndToggleStopsCarry() {
        val plans = listOf(BudgetPlan(YearMonth.of(2026, 9), 10000, true), BudgetPlan(YearMonth.of(2026, 10), 10000, false))
        assertEquals(10000, effectiveBudget(YearMonth.of(2026, 10), plans, listOf(tx(TransactionType.EXPENSE, 20000, "2026-09-01")))!!.total)
        assertEquals(10000, effectiveBudget(YearMonth.of(2026, 10), plans.map { it.copy(rollover = false) }, emptyList())!!.total)
    }
    @Test fun paceIncludesTodayAndComparesMatchingDayOnly() {
        val rows = listOf(tx(TransactionType.EXPENSE, 4000), tx(TransactionType.EXPENSE, 3000, "2026-09-04"), tx(TransactionType.EXPENSE, 9999, "2026-09-05"))
        val pace = budgetPace(LedgerUiState(YearMonth.of(2026, 10), rows, 32000), LocalDate.parse("2026-10-04"))
        assertEquals(28, pace.remainingDays); assertEquals(1000, pace.dailyAllowance); assertEquals(31000L, pace.projectedExpense); assertEquals(3000, pace.previousSamePeriodExpense)
    }
    @Test fun futureAndPastMonthPaceDoesNotDivideByZero() {
        assertNull(budgetPace(LedgerUiState(YearMonth.of(2026, 11), budgetAmount = 100), LocalDate.parse("2026-10-04")).projectedExpense)
        assertEquals(0, budgetPace(LedgerUiState(YearMonth.of(2026, 9), budgetAmount = 100), LocalDate.parse("2026-10-04")).remainingDays)
    }
}
