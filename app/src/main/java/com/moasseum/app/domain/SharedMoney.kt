package com.moasseum.app.domain

enum class SharedMoneyMode(val label: String, val description: String) {
    EQUAL_SPLIT("반반 정산", "공유한 지출을 50:50으로 나눠 참고 정산액을 보여줘요."),
    SHARED_FUND("공동 자금", "공유한 수입과 지출을 하나의 공동 장부로 합산해요."),
    SEPARATE("각자 관리", "각자 결제한 공유 거래를 비교하고 정산은 계산하지 않아요."),
    INCOME_OVERVIEW("수입 비교", "공동 장부에 공유한 두 사람의 월간 수입을 비교해요."),
    MONTHLY_SAVINGS("매월 저축", "이번 달 공동 목표와 모은 금액을 함께 관리해요."),
}

data class SharedLedgerSettings(
    val moneyMode: SharedMoneyMode = SharedMoneyMode.EQUAL_SPLIT,
)

data class SharedPayerTotals(
    val myExpense: Long,
    val partnerExpense: Long,
    val myIncome: Long,
    val partnerIncome: Long,
) {
    val expenseTotal: Long get() = myExpense + partnerExpense
    val incomeTotal: Long get() = myIncome + partnerIncome
    val partnerOwesMe: Long get() = (myExpense - expenseTotal / 2).coerceAtLeast(0)
    val iOwePartner: Long get() = (expenseTotal / 2 - myExpense).coerceAtLeast(0)
}

data class SharedSavingsGoalAmount(val target: Long, val current: Long)

data class SharedSavingsProgress(val target: Long, val current: Long) {
    val fraction: Float get() = if (target <= 0L) 0f else (current.toDouble() / target).toFloat().coerceIn(0f, 1f)
}

data class SharedMoneyNudge(val title: String, val message: String)

/** A private, deterministic, non-judgmental prompt; it uses only shared monthly totals. */
fun sharedMoneyNudge(expense: Long, budget: Long?, transactionCount: Int): SharedMoneyNudge {
    val spent = expense.coerceAtLeast(0L)
    val plan = budget?.takeIf { it > 0L }
    return when {
        transactionCount <= 0 -> SharedMoneyNudge("함께 시작해요", "공유할 기록을 하나씩 고르면 이번 달 흐름이 함께 보여요.")
        plan == null -> SharedMoneyNudge("함께 정할 기준 하나", "이번 달 생활비 기준을 정하면 다음 선택이 더 편해져요.")
        spent == 0L -> SharedMoneyNudge("좋은 출발이에요", "아직 공동 지출이 없어요. 필요한 기록만 편하게 남겨 보세요.")
        spent > plan -> SharedMoneyNudge("다음 달을 위한 작은 대화", "계획보다 더 쓴 달도 괜찮아요. 다음 달에 조정할 항목 하나만 함께 골라봐요.")
        spent.toDouble() / plan.toDouble() >= 0.75 -> SharedMoneyNudge("차근차근 조율해요", "예산의 ${(spent.toDouble() * 100.0 / plan).toInt().coerceIn(0, 100)}%를 사용했어요. 남은 일정에 맞춰 기준을 같이 살펴봐요.")
        else -> SharedMoneyNudge("지금 흐름이 좋아요", "공동 지출이 목표 안에서 움직이고 있어요. 필요한 곳에 잘 쓰고 있어요.")
    }
}

fun sharedSavingsProgress(goals: Iterable<SharedSavingsGoalAmount>): SharedSavingsProgress {
    var target = 0L
    var current = 0L
    goals.forEach { goal ->
        target = saturatingAdd(target, goal.target.coerceAtLeast(0L))
        current = saturatingAdd(current, goal.current.coerceAtLeast(0L))
    }
    return SharedSavingsProgress(target = target, current = current.coerceAtMost(target))
}

private fun saturatingAdd(left: Long, right: Long): Long =
    if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right

fun sharedPayerTotals(transactions: List<Transaction>, userId: String): SharedPayerTotals {
    fun sum(type: TransactionType, mine: Boolean): Long = transactions.asSequence()
        .filter { it.type == type }
        .filter { (it.ownerId == userId || it.ownerId == "local-user") == mine }
        .sumOf(Transaction::amount)
    return SharedPayerTotals(
        myExpense = sum(TransactionType.EXPENSE, mine = true),
        partnerExpense = sum(TransactionType.EXPENSE, mine = false),
        myIncome = sum(TransactionType.INCOME, mine = true),
        partnerIncome = sum(TransactionType.INCOME, mine = false),
    )
}
