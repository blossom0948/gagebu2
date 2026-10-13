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
    @Test fun externalDepositChangesLinkedBalanceWithoutChangingIncomeOrExpenseTotals() {
        val account = Account("A", "은행", 10_000)
        val rows = listOf(tx(TransactionType.EXTERNAL_DEPOSIT, 5_000), tx(TransactionType.INCOME, 2_000, from = null))
        assertEquals(15_000L, accountBalance(account, rows))
        val state = LedgerUiState(YearMonth.of(2026, 10), rows)
        assertEquals(0L, state.expenseTotal)
        assertEquals(2_000L, state.incomeTotal)
        assertEquals("+₩5,000", formatSignedWon(5_000, TransactionType.EXTERNAL_DEPOSIT))
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

    @Test fun paydayCycleMovesAtBoundaryAndClampsShortMonths() {
        assertEquals(BudgetCycle(LocalDate.parse("2026-09-15"), LocalDate.parse("2026-10-14")), budgetCycleForDate(LocalDate.parse("2026-10-14"), 15))
        assertEquals(BudgetCycle(LocalDate.parse("2026-10-15"), LocalDate.parse("2026-11-14")), budgetCycleForDate(LocalDate.parse("2026-10-15"), 15))
        assertEquals(BudgetCycle(LocalDate.parse("2026-02-28"), LocalDate.parse("2026-03-30")), budgetCycle(YearMonth.of(2026, 2), 31))
        assertEquals(BudgetCycle(LocalDate.parse("2024-02-29"), LocalDate.parse("2024-03-30")), budgetCycle(YearMonth.of(2024, 2), 31))
    }

    @Test fun paydayBasedRolloverCountsOnlyExpensesInEachBudgetCycle() {
        val plans = listOf(
            BudgetPlan(YearMonth.of(2026, 9), 10_000, true),
            BudgetPlan(YearMonth.of(2026, 10), 10_000, false),
        )
        val rows = listOf(
            tx(TransactionType.EXPENSE, 6_000, "2026-10-05"),
            tx(TransactionType.EXPENSE, 2_000, "2026-10-15"),
            tx(TransactionType.EXPENSE, 9_000, "2026-09-10"),
        )
        assertEquals(14_000L, effectiveBudget(YearMonth.of(2026, 10), plans, rows, payday = 15)!!.total)
    }

    @Test fun paydayBudgetSummaryIncomeUsesTheSameCycleAsBudgetSpending() {
        val rows = listOf(
            tx(TransactionType.INCOME, 20_000, "2026-08-24"),
            tx(TransactionType.INCOME, 30_000, "2026-08-25"),
            tx(TransactionType.INCOME, 15_000, "2026-09-24"),
            tx(TransactionType.INCOME, 50_000, "2026-09-25"),
        )
        val state = LedgerUiState(YearMonth.of(2026, 8), rows, budgetPayday = 25)

        assertEquals(45_000L, state.budgetPeriodIncomeTotal)
        assertEquals(50_000L, state.incomeTotal)
    }

    @Test fun calendarMonthCategoriesStaySeparateFromThePaydayBudgetCycle() {
        val month = YearMonth.now().minusMonths(2)
        fun expense(date: LocalDate, amount: Long, category: String) = Transaction(
            id = amount,
            type = TransactionType.EXPENSE,
            amount = amount,
            occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            categoryKey = category,
            merchant = "테스트",
            memo = "",
            paymentMethod = "카드",
            source = "MANUAL",
        )
        val rows = listOf(
            expense(month.minusMonths(1).atDay(28), 4_000L, "FOOD"),
            expense(month.atDay(5), 9_000L, "FOOD"),
            expense(month.atDay(26), 8_000L, "CAFE"),
            expense(month.plusMonths(1).atDay(5), 4_000L, "FOOD"),
        )
        val state = LedgerUiState(month, rows, budgetPayday = 25)

        assertEquals(12_000L, state.budgetPeriodExpenseTotal)
        assertEquals(listOf("FOOD" to 9_000L, "CAFE" to 8_000L), state.categoryTotals.map { it.key to it.total })
    }

    @Test fun paydayBudgetPaceUsesCurrentCycleRatherThanCalendarMonth() {
        val rows = listOf(
            tx(TransactionType.EXPENSE, 4_000, "2026-10-14"),
            tx(TransactionType.EXPENSE, 2_000, "2026-10-15"),
        )
        val state = LedgerUiState(YearMonth.of(2026, 10), rows, budgetAmount = 10_000, budgetPayday = 15)
        val before = budgetPace(state, LocalDate.parse("2026-10-14"))
        val after = budgetPace(state, LocalDate.parse("2026-10-15"))
        assertEquals(1, before.remainingDays)
        assertEquals(258, after.dailyAllowance)
        assertEquals(62_000L, after.projectedExpense)
    }
}
