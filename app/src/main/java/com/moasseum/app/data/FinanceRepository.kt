package com.moasseum.app.data

import com.moasseum.app.data.local.BudgetEntity
import com.moasseum.app.data.local.FinanceDao
import com.moasseum.app.data.local.TransactionEntity
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
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
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun softDeleteTransaction(id: Long) {
        dao.softDeleteTransaction(id = id, deletedAt = System.currentTimeMillis())
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
