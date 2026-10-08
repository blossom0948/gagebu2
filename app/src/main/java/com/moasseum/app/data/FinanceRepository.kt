package com.moasseum.app.data

import com.moasseum.app.data.local.BudgetEntity
import com.moasseum.app.data.local.AccountEntity
import com.moasseum.app.domain.Account
import com.moasseum.app.domain.validateAccount
import com.moasseum.app.data.local.FinanceDao
import com.moasseum.app.data.local.NotificationCandidateEntity
import com.moasseum.app.data.local.RecurringTransactionEntity
import com.moasseum.app.data.local.TransactionEntity
import com.moasseum.app.domain.NotificationCandidate
import com.moasseum.app.domain.RecurringRule
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.firstRecurringOccurrence
import com.moasseum.app.domain.installmentSchedule
import com.moasseum.app.domain.toDomain
import com.moasseum.app.notification.NotificationSourcePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.TimeZone

class FinanceRepository(
    private val dao: FinanceDao,
) {
    suspend fun backupSnapshot(): FullBackup = dao.backupSnapshot()
    suspend fun restoreBackup(backup: FullBackup) = dao.restoreSnapshot(backup)
    fun observeAccounts(): Flow<List<Account>> = dao.observeAccounts().map { rows ->
        rows.map { Account(it.id, it.name, it.openingBalance, it.archived) }
    }

    suspend fun saveAccount(id: String?, name: String, openingBalance: Long) {
        validateAccount(name, openingBalance)
        val existing = id?.let { dao.getAccount(it) }
        require(id == null || existing != null)
        dao.upsertAccount(AccountEntity(id ?: java.util.UUID.randomUUID().toString(), name.trim(), openingBalance, existing?.archived ?: false, System.currentTimeMillis()))
    }

    suspend fun archiveAccount(id: String, archived: Boolean) = dao.archiveAccount(id, archived, System.currentTimeMillis())

    suspend fun linkTransactionAccount(id: Long, accountId: String?) = dao.linkTransactionAccount(id, accountId, System.currentTimeMillis())

    suspend fun saveTransfer(id: Long?, amount: Long, fromId: String, toId: String, date: LocalDate, memo: String) {
        val now = System.currentTimeMillis()
        val occurredAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (id != null) dao.saveTransferEdit(id, amount, occurredAt, fromId, toId, memo.trim(), now)
        else {
            val from = dao.getAccount(fromId) ?: error("보내는 계좌가 없어요.")
            val to = dao.getAccount(toId) ?: error("받는 계좌가 없어요.")
            dao.saveLinkedTransaction(TransactionEntity(type = "TRANSFER", amount = amount, occurredAt = occurredAt,
                timezone = TimeZone.getDefault().id, categoryKey = "OTHER", merchant = "${from.name} → ${to.name}",
                memo = memo.trim(), paymentMethod = "계좌 이체", accountId = fromId, destinationAccountId = toId,
                createdAt = now, updatedAt = now))
        }
    }
    fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeTransactions().map { entities -> entities.map(TransactionEntity::toDomain) }

    fun observeBudget(monthKey: String): Flow<BudgetEntity?> = dao.observeBudget(monthKey)
    fun observeAllBudgets(): Flow<List<BudgetEntity>> = dao.observeAllBudgets()
    suspend fun setBudgetRollover(monthKey: String, enabled: Boolean) {
        ensureBudget(monthKey)
        dao.setBudgetRollover(monthKey, enabled, System.currentTimeMillis())
    }

    fun observePendingNotificationCandidates(): Flow<List<NotificationCandidate>> =
        dao.observePendingNotificationCandidates().map { candidates ->
            candidates.filterNot { NotificationSourcePolicy.isExcluded(it.packageName) }
                .map(NotificationCandidateEntity::toDomain)
        }

    suspend fun dismissExcludedNotificationCandidates() =
        dao.dismissExcludedNotificationCandidates(NotificationSourcePolicy.excludedPackages)

    fun observeRecurringRules(): Flow<List<RecurringRule>> =
        dao.observeRecurringRules().map { rules -> rules.map(RecurringTransactionEntity::toDomain) }

    suspend fun ensureBudget(monthKey: String) {
        if (dao.getBudget(monthKey) == null) {
            val previous = dao.getBudget(YearMonth.parse(monthKey).minusMonths(1).toString())?.takeIf { it.rollover }
            dao.upsertBudget(
                BudgetEntity(
                    monthKey = monthKey,
                    amount = previous?.amount ?: DEFAULT_MONTHLY_BUDGET,
                    rollover = previous != null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun ensureRollingBudget(monthKey: String) {
        if (dao.getBudget(YearMonth.parse(monthKey).minusMonths(1).toString())?.rollover == true) ensureBudget(monthKey)
    }

    suspend fun updateBudget(monthKey: String, amount: Long) {
        require(amount in 1..1_000_000_000_000L)
        val previous = dao.getBudget(monthKey)
        dao.upsertBudget(
            BudgetEntity(
                monthKey = monthKey,
                amount = amount,
                rollover = previous?.rollover ?: false,
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
        accountId: String? = null,
    ) {
        require(type != TransactionType.TRANSFER)
        val now = System.currentTimeMillis()
        val occurredAtMillis = occurredAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dao.saveLinkedTransaction(
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
                accountId = accountId,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun addBatchTransactions(candidates: List<AiTransactionCandidate>, paymentMethod: String): Int {
        require(candidates.size in 1..30 && paymentMethod.isNotBlank())
        val now = System.currentTimeMillis()
        val timezone = TimeZone.getDefault().id
        val rows = candidates.map { candidate ->
            require(candidate.type != TransactionType.TRANSFER)
            require(candidate.amount in 1..1_000_000_000_000L && candidate.merchant.isNotBlank() && candidate.categoryKey.isNotBlank())
            TransactionEntity(
                type = candidate.type.name,
                amount = candidate.amount,
                occurredAt = candidate.occurredDate.atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli(),
                timezone = timezone,
                categoryKey = candidate.categoryKey,
                merchant = candidate.merchant.trim(),
                memo = candidate.memo.trim(),
                paymentMethod = paymentMethod,
                source = "AI_BATCH",
                createdAt = now,
                updatedAt = now,
            )
        }
        dao.saveBatchTransactions(rows)
        return rows.size
    }

    suspend fun applyBatchDelete(ids: List<Long>): Int =
        dao.applyPersonalBatchDelete(ids, System.currentTimeMillis())

    suspend fun applyBatchEdit(ids: List<Long>, values: BatchEditValues): Int {
        val timezone = values.occurredDate?.let { TimeZone.getDefault().id }
        val occurredAt = values.occurredDate?.atStartOfDay(ZoneId.of(timezone!!))?.toInstant()?.toEpochMilli()
        return dao.applyPersonalBatchEdit(
            ids = ids,
            amount = values.amount,
            merchant = values.merchant?.trim(),
            categoryKey = values.categoryKey,
            type = values.type?.name,
            occurredAt = occurredAt,
            timezone = timezone,
            memo = values.memo?.trim(),
            now = System.currentTimeMillis(),
        )
    }

    suspend fun restoreBatch(ids: List<Long>): Int = dao.restorePersonalBatch(ids, System.currentTimeMillis())

    suspend fun addInstallmentPlan(
        total: Long,
        count: Int,
        firstChargeDate: LocalDate,
        categoryKey: String,
        merchant: String,
        memo: String,
        paymentMethod: String,
    ) {
        require(merchant.isNotBlank()) { "가맹점 이름을 입력해 주세요." }
        require(categoryKey.isNotBlank() && paymentMethod.isNotBlank())
        val schedule = installmentSchedule(total, count, firstChargeDate)
        val now = System.currentTimeMillis()
        val timezone = TimeZone.getDefault().id
        val groupId = java.util.UUID.randomUUID().toString()
        dao.saveInstallmentPlan(schedule.map { part ->
            TransactionEntity(
                type = TransactionType.EXPENSE.name,
                amount = part.amount,
                occurredAt = part.date.atStartOfDay(ZoneId.of(timezone)).toInstant().toEpochMilli(),
                timezone = timezone,
                categoryKey = categoryKey,
                merchant = merchant.trim(),
                memo = listOf(memo.trim(), "할부 ${part.number}/$count").filter(String::isNotBlank).joinToString(" · "),
                paymentMethod = paymentMethod,
                source = "INSTALLMENT",
                installmentGroupId = groupId,
                installmentNumber = part.number,
                installmentCount = count,
                createdAt = now,
                updatedAt = now,
            )
        })
    }

    suspend fun deleteInstallmentPlan(groupId: String): Int =
        dao.softDeleteInstallmentPlan(groupId, System.currentTimeMillis())

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
        require(type != TransactionType.TRANSFER && amount in 1..1_000_000_000_000L && merchant.isNotBlank())
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

    suspend fun importTransactions(rows: List<ImportedTransaction>): Int = dao.importRows(rows, TimeZone.getDefault().id, System.currentTimeMillis())

    suspend fun saveNotificationCandidate(candidate: NotificationCandidateEntity): Long? =
        dao.insertNotificationCandidate(candidate).takeIf { it > 0L }

    suspend fun acceptNotificationCandidate(id: Long): Boolean {
        val candidate = dao.getNotificationCandidate(id) ?: return false
        if (NotificationSourcePolicy.isExcluded(candidate.packageName)) return false
        val now = System.currentTimeMillis()
        val occurredAt = java.time.Instant.ofEpochMilli(candidate.postedAt)
            .atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.acceptCandidateTransaction(
            id,
            TransactionEntity(
                amount = candidate.amount,
                type = candidate.type,
                occurredAt = occurredAt,
                timezone = TimeZone.getDefault().id,
                categoryKey = candidate.categoryKey,
                merchant = candidate.merchant,
                memo = "알림에서 가져온 거래",
                source = "NOTIFICATION",
                paymentMethod = "알림 감지",
                createdAt = now,
                updatedAt = now,
            ),
        )
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
        require(type != TransactionType.TRANSFER && amount in 1..1_000_000_000_000L && merchant.isNotBlank() && dayOfMonth in 1..31)
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

    suspend fun editRecurringRule(
        id: Long, amount: Long, type: TransactionType, merchant: String, categoryKey: String,
        memo: String, paymentMethod: String, dayOfMonth: Int,
    ) {
        require(type != TransactionType.TRANSFER && amount in 1..1_000_000_000_000L && merchant.isNotBlank() && dayOfMonth in 1..31)
        dao.editRecurringRule(
            id, amount, type.name, merchant.trim(), categoryKey, memo.trim(), paymentMethod,
            dayOfMonth, LocalDate.now().toString(), System.currentTimeMillis(),
        )
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
        type = TransactionType.valueOf(type),
        amount = amount,
        occurredAt = occurredAt,
        categoryKey = categoryKey,
        merchant = merchant,
        memo = memo,
        paymentMethod = paymentMethod,
        source = source,
        accountId = accountId,
        destinationAccountId = destinationAccountId,
        timezone = timezone,
        ownerId = ownerId,
        ledgerId = ledgerId,
        sharingScope = sharingScope,
        cloudId = cloudId,
        installmentGroupId = installmentGroupId,
        installmentNumber = installmentNumber,
        installmentCount = installmentCount,
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
