package com.moasseum.app.domain

import java.time.LocalDate
import java.time.ZoneId

enum class ActivityNoticeCategory(val label: String) {
    BUDGET("예산"),
    PAYMENT("결제"),
    CHALLENGE("챌린지"),
}

data class ActivityNotice(
    val id: String,
    val category: ActivityNoticeCategory,
    val title: String,
    val message: String,
    val createdAt: Long,
)

/** Build local-only notices from the ledger; transaction details never leave the device. */
fun buildActivityNotices(
    state: LedgerUiState,
    challenge: NoSpendChallengeSettings,
    today: LocalDate = LocalDate.now(),
): List<ActivityNotice> {
    val notices = mutableListOf<ActivityNotice>()
    val budgetCycle = budgetCycleForDate(today, state.budgetPayday)
    val budget = state.budgetAmount?.takeIf { it > 0L && state.month == java.time.YearMonth.from(today) }
    if (budget != null) {
        val expenses = state.transactions
            .filter {
                it.type == TransactionType.EXPENSE &&
                    it.occurredDate in budgetCycle.startDate..minOf(today, budgetCycle.endDate)
            }
            .sortedWith(compareBy<Transaction> { it.occurredAt }.thenBy { it.id })
        var periodExpenseTotal = 0L
        expenses.forEach { periodExpenseTotal = (periodExpenseTotal + it.amount).coerceAtMost(Long.MAX_VALUE) }
        val periodId = budgetCycle.startMonth
        val halfway = budget / 2 + budget % 2
        fun crossingAt(threshold: Long): Long? {
            var total = 0L
            for (transaction in expenses) {
                total = (total + transaction.amount).coerceAtMost(Long.MAX_VALUE)
                if (total >= threshold) return transaction.occurredAt
            }
            return null
        }
        crossingAt(halfway)?.let { at ->
            notices += ActivityNotice(
                id = "budget:$periodId:half",
                category = ActivityNoticeCategory.BUDGET,
                title = "이번 예산 기간의 절반을 사용했어요",
                message = "${budgetCycle.label} · 현재 ${formatWon(periodExpenseTotal)} 사용 · 목표 ${formatWon(budget)}",
                createdAt = at,
            )
        }
        if (budget < Long.MAX_VALUE && periodExpenseTotal > budget) {
            crossingAt(budget + 1L)?.let { at ->
                notices += ActivityNotice(
                    id = "budget:$periodId:over",
                    category = ActivityNoticeCategory.BUDGET,
                    title = "이번 예산 기간의 목표 지출을 넘었어요",
                    message = "${budgetCycle.label} · 목표보다 ${formatWon(periodExpenseTotal - budget)} 더 사용했어요",
                    createdAt = at,
                )
            }
        }
    }

    val startDate = challenge.startDate
    if (startDate != null && !startDate.isAfter(today) &&
        noSpendStreakDays(state.transactions, startDate, today) >= challenge.goalDays
    ) {
        notices += ActivityNotice(
            id = "challenge:${startDate}:complete:${challenge.goalDays}",
            category = ActivityNoticeCategory.CHALLENGE,
            title = "무지출 챌린지 목표를 달성했어요",
            message = "${challenge.goalDays}일 연속 무지출을 기록했어요",
            createdAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }
    return notices.sortedByDescending(ActivityNotice::createdAt)
}
