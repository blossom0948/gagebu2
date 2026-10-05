package com.moasseum.app.domain

data class Account(
    val id: String,
    val name: String,
    val openingBalance: Long,
    val archived: Boolean = false,
)

fun accountBalance(account: Account, transactions: List<Transaction>): Long =
    transactions.fold(account.openingBalance) { balance, transaction ->
        val change = when (transaction.type) {
            TransactionType.EXPENSE -> if (transaction.accountId == account.id) -transaction.amount else 0L
            TransactionType.INCOME -> if (transaction.accountId == account.id) transaction.amount else 0L
            TransactionType.TRANSFER -> when (account.id) {
                transaction.accountId -> -transaction.amount
                transaction.destinationAccountId -> transaction.amount
                else -> 0L
            }
        }
        Math.addExact(balance, change)
    }

fun validateAccount(name: String, openingBalance: Long) {
    require(name.trim().length in 1..30) { "계좌 이름은 1~30자로 입력해 주세요." }
    require(openingBalance in -1_000_000_000_000L..1_000_000_000_000L) { "시작 잔액 범위를 확인해 주세요." }
}
