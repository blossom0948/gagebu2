package com.moasseum.app.data

import com.moasseum.app.data.local.BudgetEntity
import com.moasseum.app.data.local.FinanceDao
import com.moasseum.app.data.local.NotificationCandidateEntity
import com.moasseum.app.data.local.RecurringTransactionEntity
import com.moasseum.app.data.local.TransactionEntity
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.firstRecurringOccurrence
import com.moasseum.app.domain.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.TimeZone

class FinanceRepository(
    private val dao: FinanceDao,
) {
    fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeTransactions().map { entities -> entities.map(TransactionEntity::toDomain) }

    fun observeBudget(monthKey: String): Flow<BudgetEntity?> = dao.observeBudget(monthKey)

    fun observePendingNotificationCandidates(): Flow<List<NotificationCandidate>> =
        dao.observePendingNotificationCandidates().map { candidates -> candidates.map(NotificationCandidateEntity::toDomain) }

    fun observeRecurringRules(): Flow<List<RecurringRule>> =
        dao.observeRecurringRules().map { rules -> rules.map(RecurringTransactionEntity::toDomain) }

    suspend fun ensureBudget(monthKey: String) {
        if (dao.getBudget(monthKey) == null) {
            dao.upsertBudget(
                BudgetEntity(
                    monthKey = monthKey,
                    amount = DEFAULT_MONTHLY_BUDGET,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun updateBudget(monthKey: String, amount: Long) {
        dao.upsertBudget(
            BudgetEntity(
                monthKey = monthKey,
                amount = amount,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun addTransaction(
        amount: Long,
        type: TransactionType,
        occurredAt: LocalDate,
        categoryKey: String,
        merchant: String,
        memo: String,
        source: String = "MANUAL",
        paymentMethod: String = "카드",
    ) {
        val now = System.currentTimeMillis()
        val occurredAtMillis = occurredAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.insertTransaction(
            TransactionEntity(
                type = type.name,
                amount = amount,
                occurredAt = occurredAtMillis,
                timezone = TimeZone.getDefault().id,
                categoryKey = categoryKey,
                merchant = merchant.trim(),
                memo = memo.trim(),
                paymentMethod = paymentMethod,
                source = source,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun softDeleteTransaction(id: Long) {
        dao.softDeleteTransaction(id = id, deletedAt = System.currentTimeMillis())
    }

    suspend fun restoreTransaction(id: Long) {
        dao.restoreTransaction(id = id, updatedAt = System.currentTimeMillis())
    }

    suspend fun updateTransaction(
        id: Long,
        amount: Long,
        type: TransactionType,
        occurredAt: LocalDate,
        categoryKey: String,
        merchant: String,
        memo: String,
        paymentMethod: String,
    ): Boolean {
        val updated = dao.updateTransaction(
            id = id,
            type = type.name,
            amount = amount,
            occurredAt = occurredAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            timezone = TimeZone.getDefault().id,
            categoryKey = categoryKey,
            merchant = merchant.trim(),
            memo = memo.trim(),
            paymentMethod = paymentMethod,
            updatedAt = System.currentTimeMillis(),
        )
        return updated > 0
    }

    suspend fun importTransactions(rows: List<ImportedTransaction>): Int {
        var inserted = 0
        rows.forEach { row ->
            val merchant = row.merchant.trim()
            val memo = row.memo.trim()
            if (!dao.transactionExists(
                    type = row.type.name,
                    amount = row.amount,
                    occurredAt = row.occurredAt,
                    categoryKey = row.categoryKey,
                    merchant = merchant,
                    memo = memo,
                    paymentMethod = row.paymentMethod,
                )
            ) {
                val now = System.currentTimeMillis()
                dao.insertTransaction(
                    TransactionEntity(
                        type = row.type.name,
                        amount = row.amount,
                        occurredAt = row.occurredAt,
                        timezone = TimeZone.getDefault().id,
                        categoryKey = row.categoryKey,
                        merchant = merchant,
                        memo = memo,
                        paymentMethod = row.paymentMethod,
                        source = row.source,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                inserted++
            }
        }
        return inserted
    }

    suspend fun saveNotificationCandidate(candidate: NotificationCandidateEntity): Long? =
        dao.insertNotificationCandidate(candidate).takeIf { it > 0L }

    suspend fun acceptNotificationCandidate(id: Long) {
        val candidate = dao.getNotificationCandidate(id) ?: return
        addTransaction(
            amount = candidate.amount,
            type = if (candidate.type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE,
            occurredAt = java.time.Instant.ofEpochMilli(candidate.postedAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate(),
            categoryKey = candidate.categoryKey,
            merchant = candidate.merchant,
            memo = "알림에서 가져온 거래",
            source = "NOTIFICATION",
            paymentMethod = "알림 감지",
        )
        dao.updateNotificationCandidateStatus(id, "ACCEPTED")
    }

    suspend fun dismissNotificationCandidate(id: Long) {
        dao.updateNotificationCandidateStatus(id, "DISMISSED")
    }

    suspend fun clearAllLocalRecords() {
        dao.clearAllLocalRecords()
        ensureBudget(YearMonth.now().toString())
    }

    suspend fun reassignDeletedCustomCategory(categoryKey: String) {
        require(categoryKey.matches(Regex("CUSTOM_[A-F0-9]{12}"))) { "기본 카테고리는 삭제할 수 없어요." }
        dao.deleteCustomCategory(categoryKey, "OTHER", System.currentTimeMillis())
    }

    suspend fun addRecurringRule(
        amount: Long,
        type: TransactionType,
        merchant: String,
        categoryKey: String,
        memo: String,
        paymentMethod: String,
        dayOfMonth: Int,
    ) {
        require(amount > 0L && merchant.isNotBlank() && dayOfMonth in 1..31)
        val today = LocalDate.now()
        val nextOccurrence = firstRecurringOccurrence(today, dayOfMonth)
        val now = System.currentTimeMillis()
        dao.insertRecurringRule(
            RecurringTransactionEntity(
                type = type.name,
                amount = amount,
                merchant = merchant.trim(),
                dayOfMonth = dayOfMonth,
                nextOccurrenceDate = nextOccurrence.toString(),
                categoryKey = categoryKey,
                memo = memo.trim(),
                paymentMethod = paymentMethod,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        postDueRecurringTransactions(today)
    }

    suspend fun setRecurringRuleActive(id: Long, active: Boolean) {
        dao.setRecurringRuleActive(id, active, System.currentTimeMillis())
    }

    suspend fun deleteRecurringRule(id: Long) {
        dao.deleteRecurringRule(id)
    }

    suspend fun postDueRecurringTransactions(today: LocalDate = LocalDate.now()): Int =
        dao.postDueRecurringTransactions(today.toString(), TimeZone.getDefault().id, System.currentTimeMillis())

    companion object {
        const val DEFAULT_MONTHLY_BUDGET = 1_500_000L
    }
}

private fun TransactionEntity.toDomain(): Transaction =
    Transaction(
        id = id,
        type = if (type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE,
        amount = amount,
        occurredAt = occurredAt,
        categoryKey = categoryKey,
        merchant = merchant,
        memo = memo,
        paymentMethod = paymentMethod,
        source = source,
    )

private fun RecurringTransactionEntity.toDomain(): RecurringRule =
    RecurringRule(
        id = id,
        type = if (type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE,
        amount = amount,
        merchant = merchant,
        dayOfMonth = dayOfMonth,
        nextOccurrenceDate = LocalDate.parse(nextOccurrenceDate),
        categoryKey = categoryKey,
        memo = memo,
        paymentMethod = paymentMethod,
        isActive = isActive,
    )
