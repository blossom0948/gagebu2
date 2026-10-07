package com.moasseum.app

import com.moasseum.app.data.CsvBackup
import com.moasseum.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.TimeZone

class AuditRegressionTest {
    private fun seoulTransaction() = Transaction(1, TransactionType.EXPENSE, 1000,
        LocalDate.parse("2026-10-01").atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
        "FOOD", "합성 QA", "", "카드", "MANUAL", timezone = "Asia/Seoul")

    @Test fun transactionDateAndMonthDoNotMoveWhenTheDeviceTimezoneChanges() {
        val original = TimeZone.getDefault()
        try {
            val transaction = seoulTransaction()
            for (zone in listOf("UTC", "America/Los_Angeles", "Pacific/Auckland")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(LocalDate.parse("2026-10-01"), transaction.occurredDate)
                assertEquals(1000, LedgerUiState(YearMonth.of(2026, 10), listOf(transaction)).expenseTotal)
                assertTrue(LedgerUiState(YearMonth.of(2026, 9), listOf(transaction)).monthTransactions.isEmpty())
            }
        } finally { TimeZone.setDefault(original) }
    }

    @Test fun csvRoundTripPreservesTheOriginalTimezone() {
        val transaction = seoulTransaction()
        val restored = CsvBackup.decode(CsvBackup.encode(listOf(transaction))).single()
        assertEquals(transaction.timezone, restored.timezone)
        assertEquals(transaction.occurredAt, restored.occurredAt)
    }

    @Test fun legacyCsvStillImportsWithoutATimezoneColumn() {
        val restored = CsvBackup.decode("type,amount,occurredAt,categoryKey,merchant\nEXPENSE,1000,1790780400000,FOOD,합성 QA").single()
        assertNull(restored.timezone)
    }

    @Test fun invalidCsvTimezoneIsRejectedBeforeWritingToTheDatabase() {
        assertThrows(IllegalArgumentException::class.java) {
            CsvBackup.decode("type,amount,occurredAt,categoryKey,merchant,timezone\nEXPENSE,1000,1790780400000,FOOD,합성 QA,Invalid/Zone")
        }
    }

    @Test fun pastedNegativeDecimalAndMixedAmountsAreNotSilentlyChanged() {
        for (value in listOf("-1000", "−1000", "12.5", "1e3", "10x20", "1/2", "+1000", "12\n.50")) assertNull(value, parseAmount(value))
        assertEquals(12000L, parseAmount("₩12,000원"))
    }

    @Test fun amountLimitIsEnforcedAtBothBoundaries() {
        assertEquals(1L, parseAmount("1"))
        assertEquals(1_000_000_000_000L, parseAmount("1,000,000,000,000"))
        assertNull(parseAmount("1,000,000,000,001"))
        assertNull(parseAmount("999999999999999999999999999999"))
    }

    @Test fun aNewAiRequestSupersedesAnEarlierResponse() {
        val gate = LatestRequestGate()
        val old = gate.start(); val latest = gate.start()
        assertFalse(gate.isCurrent(old)); assertTrue(gate.isCurrent(latest))
    }

    @Test fun changingMonthsInvalidatesAnInFlightAiResponse() {
        val gate = LatestRequestGate()
        val old = gate.start(); gate.invalidate()
        assertFalse(gate.isCurrent(old))
    }

    @Test fun returningToTheOldMonthCannotReviveAnOldRequest() {
        val gate = LatestRequestGate()
        val old = gate.start(); gate.invalidate(); gate.invalidate()
        assertFalse(gate.isCurrent(old)); assertTrue(gate.isCurrent(gate.start()))
    }

    @Test fun analysisAndQuestionRequestsAreIndependent() {
        val analysis = LatestRequestGate(); val question = LatestRequestGate()
        val request = question.start(); analysis.start(); analysis.invalidate()
        assertTrue(question.isCurrent(request))
    }

    private fun dated(day: String, amount: Long, type: TransactionType = TransactionType.EXPENSE) = seoulTransaction().copy(
        type = type, amount = amount,
        occurredAt = LocalDate.parse(day).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
    )

    @Test fun todayCardsDoNotBecomeZeroWhenViewingAnOlderMonth() {
        val today = LocalDate.now()
        val rows = listOf(dated(today.toString(), 1000), dated(today.toString(), 3000, TransactionType.INCOME), dated(today.toString(), 500, TransactionType.TRANSFER))
        val state = LedgerUiState(YearMonth.from(today).minusMonths(1), rows)
        assertEquals(1000, state.todayExpenseTotal)
        assertEquals(1, state.todayTransactionCount)
        assertEquals(1, state.weekTransactionCount)
    }

    @Test fun aWeekSpanningTwoMonthsIncludesBothMonthsAndOnlyExpenses() {
        val rows = listOf(dated("2026-09-29", 1000), dated("2026-10-01", 2000), dated("2026-09-27", 4000), dated("2026-10-02", 8000), dated("2026-10-01", 16000, TransactionType.INCOME))
        assertEquals(ExpensePeriod(3000, 2), expensePeriod(rows, LocalDate.parse("2026-09-28"), LocalDate.parse("2026-10-01")))
    }

    @Test fun expensePeriodIncludesItsBoundariesButRejectsReversedDates() {
        val day = LocalDate.parse("2026-10-01")
        assertEquals(ExpensePeriod(1000, 1), expensePeriod(listOf(dated(day.toString(), 1000)), day, day))
        assertThrows(IllegalArgumentException::class.java) { expensePeriod(emptyList(), day, day.minusDays(1)) }
    }
}
