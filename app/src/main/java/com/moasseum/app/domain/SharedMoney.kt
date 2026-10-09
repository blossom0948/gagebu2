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
