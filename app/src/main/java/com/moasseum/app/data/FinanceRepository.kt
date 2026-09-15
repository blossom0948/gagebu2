package com.moasseum.app.data

import com.moasseum.app.data.local.BudgetEntity
import com.moasseum.app.data.local.FinanceDao
import com.moasseum.app.data.local.NotificationCandidateEntity
import com.moasseum.app.data.local.TransactionEntity
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
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

    suspend fun saveNotificationCandidate(candidate: NotificationCandidateEntity) {
        dao.insertNotificationCandidate(candidate)
    }

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
