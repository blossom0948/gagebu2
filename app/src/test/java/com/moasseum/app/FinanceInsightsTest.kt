package com.moasseum.app

import com.moasseum.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class FinanceInsightsTest {
    private fun transaction(id: Long, date: String, amount: Long = 1000, method: String = "생활비 카드", type: TransactionType = TransactionType.EXPENSE) =
        Transaction(id, type, amount, LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), "FOOD", "상점$id", "메모", method, "MANUAL")

    private fun rule(id: Long, next: String, day: Int, active: Boolean = true, type: TransactionType = TransactionType.EXPENSE) =
        RecurringRule(id, type, 10000, "구독$id", day, LocalDate.parse(next), "LIVING", "", "카드", active)

    @Test fun `previous calendar month includes both endpoints and only linked expenses`() {
        val card = PaymentCard("1", "생활비 카드", 25)
        val usage = cardUsage(card, YearMonth.of(2026, 10), listOf(
            transaction(1, "2026-09-01"), transaction(2, "2026-09-30", 2000),
            transaction(3, "2026-08-31"), transaction(4, "2026-10-01"),
            transaction(5, "2026-09-10", method = "카드"), transaction(6, "2026-09-12", type = TransactionType.INCOME),
        ))
        assertEquals(LocalDate.parse("2026-09-01"), usage.periodStart)
        assertEquals(LocalDate.parse("2026-09-30"), usage.periodEnd)
        assertEquals(3000L, usage.total)
        assertEquals(listOf(2L, 1L), usage.transactions.map { it.id })
    }

    @Test fun `custom cutoff spans two months`() {
        val usage = cardUsage(PaymentCard("1", "카드", 25, periodEndDay = 14, periodEndMonthsBeforeDue = 0), YearMonth.of(2026, 10), emptyList())
        assertEquals(LocalDate.parse("2026-09-15"), usage.periodStart)
        assertEquals(LocalDate.parse("2026-10-14"), usage.periodEnd)
    }

    @Test fun `February month end and due date clamp without gaps`() {
        val feb = cardUsage(PaymentCard("1", "카드", 31), YearMonth.of(2026, 3), emptyList())
        assertEquals(LocalDate.parse("2026-02-01"), feb.periodStart)
        assertEquals(LocalDate.parse("2026-02-28"), feb.periodEnd)
        val march = cardUsage(PaymentCard("1", "카드", 31), YearMonth.of(2026, 4), emptyList())
        assertEquals(feb.periodEnd.plusDays(1), march.periodStart)
        assertEquals(LocalDate.parse("2026-02-28"), cardUsage(PaymentCard("1", "카드", 31), YearMonth.of(2026, 2), emptyList()).dueDate)
    }

    @Test fun `leap year and year boundary are handled`() {
        val usage = cardUsage(PaymentCard("1", "카드", 25), YearMonth.of(2028, 3), emptyList())
        assertEquals(LocalDate.parse("2028-02-29"), usage.periodEnd)
        assertEquals(LocalDate.parse("2025-12-01"), cardUsage(PaymentCard("1", "카드", 25), YearMonth.of(2026, 1), emptyList()).periodStart)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cutoff after due date is rejected`() {
        cardUsage(PaymentCard("1", "카드", 10, periodEndMonthsBeforeDue = 0), YearMonth.of(2026, 10), emptyList())
    }

    @Test fun `radar excludes paused rules income and overdue occurrences`() {
        val result = upcomingFixedExpenses(listOf(
            rule(1, "2026-10-05", 5), rule(2, "2026-10-05", 5, active = false),
            rule(3, "2026-10-05", 5, type = TransactionType.INCOME), rule(4, "2026-09-01", 1),
        ), LocalDate.parse("2026-10-04"), 7)
        assertEquals(listOf(1L), result.map { it.rule.id })
    }

    @Test fun `radar seven days includes today but not eighth day`() {
        val result = upcomingFixedExpenses(listOf(rule(1, "2026-10-04", 4), rule(2, "2026-10-10", 10), rule(3, "2026-10-11", 11)), LocalDate.parse("2026-10-04"), 7)
        assertEquals(listOf(1L, 2L), result.map { it.rule.id })
    }

    @Test fun `radar clamps month end and can show two occurrences within thirty two days`() {
        val result = upcomingFixedExpenses(listOf(rule(1, "2026-02-28", 31)), LocalDate.parse("2026-02-28"), 32)
        assertEquals(listOf(LocalDate.parse("2026-02-28"), LocalDate.parse("2026-03-31")), result.map { it.date })
    }

    @Test fun `edit keeps due schedule when day is unchanged`() {
        val next = LocalDate.parse("2026-11-01")
        assertEquals(next, editedRecurringOccurrence(next, 1, 1, LocalDate.parse("2026-10-04")))
    }

    @Test fun `editing day does not repeat a processed month`() {
        assertEquals(LocalDate.parse("2026-11-20"), editedRecurringOccurrence(LocalDate.parse("2026-11-01"), 1, 20, LocalDate.parse("2026-10-04")))
    }

    @Test fun `editing upcoming day uses today as earliest date`() {
        assertEquals(LocalDate.parse("2026-11-01"), editedRecurringOccurrence(LocalDate.parse("2026-10-20"), 20, 1, LocalDate.parse("2026-10-04")))
    }

    @Test fun `history sorting is deterministic across days`() {
        val rows = listOf(transaction(1, "2026-10-02", 2000), transaction(2, "2026-10-01", 9000), transaction(3, "2026-10-02", 1000))
        assertEquals(listOf(3L, 1L, 2L), filterHistoryTransactions(rows, HistoryQuery()).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), filterHistoryTransactions(rows, HistoryQuery(sort = HistorySort.OLDEST)).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), filterHistoryTransactions(rows, HistoryQuery(sort = HistorySort.HIGHEST)).map { it.id })
        assertEquals(listOf(3L, 1L, 2L), filterHistoryTransactions(rows, HistoryQuery(sort = HistorySort.LOWEST)).map { it.id })
    }

    @Test fun `history combines filters and totals only filtered records`() {
        val rows = listOf(transaction(1, "2026-10-02", 2000), transaction(2, "2026-10-02", 9000, method = "현금"), transaction(3, "2026-10-01", type = TransactionType.INCOME))
        val result = filterHistoryTransactions(rows, HistoryQuery(type = TransactionType.EXPENSE, date = LocalDate.parse("2026-10-02"), paymentMethod = "현금", search = " 상점2 ", categoryKey = "FOOD"))
        assertEquals(listOf(2L), result.map { it.id })
        assertEquals(9000L, result.sumOf { it.amount })
    }

    @Test fun `history searches payment method and resolves equal amount ties`() {
        val rows = listOf(transaction(1, "2026-10-01"), transaction(2, "2026-10-02"), transaction(3, "2026-10-02"))
        assertEquals(listOf(3L, 2L, 1L), filterHistoryTransactions(rows, HistoryQuery(search = "생활비", sort = HistorySort.HIGHEST)).map { it.id })
        assertTrue(filterHistoryTransactions(rows, HistoryQuery(paymentMethod = "현금")).isEmpty())
    }
}
