package com.moasseum.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.moasseum.app.domain.nextRecurringOccurrence
import com.moasseum.app.domain.editedRecurringOccurrence
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Transaction
    suspend fun importRows(rows: List<com.moasseum.app.data.ImportedTransaction>, timezone: String, now: Long): Int {
        var count = 0
        rows.forEach { row ->
            require(row.amount in 1..1_000_000_000_000L && row.merchant.isNotBlank())
            row.accountId?.let { require(getAccount(it) != null) { "CSV에 연결된 계좌가 기기에 없어요. JSON 전체 백업으로 계좌와 함께 복원해 주세요." } }
            row.destinationAccountId?.let { require(getAccount(it) != null) { "CSV에 받는 계좌가 없어요. JSON 전체 백업을 사용해 주세요." } }
            if (row.type.name == "TRANSFER") require(row.accountId != null && row.destinationAccountId != null && row.accountId != row.destinationAccountId)
            else require(row.destinationAccountId == null)
            val merchant = row.merchant.trim(); val memo = row.memo.trim()
            if (!transactionExists(row.type.name, row.amount, row.occurredAt, row.categoryKey, merchant, memo, row.paymentMethod, row.accountId, row.destinationAccountId)) {
                insertTransaction(TransactionEntity(type = row.type.name, amount = row.amount, occurredAt = row.occurredAt, timezone = row.timezone ?: timezone,
                    categoryKey = row.categoryKey, merchant = merchant, memo = memo, paymentMethod = row.paymentMethod, source = row.source,
                    accountId = row.accountId, destinationAccountId = row.destinationAccountId, createdAt = now, updatedAt = now))
                count++
            }
        }
        return count
    }
    @Query("SELECT * FROM transactions ORDER BY id")
    suspend fun allTransactionsForBackup(): List<TransactionEntity>
    @Query("SELECT * FROM accounts ORDER BY id")
    suspend fun allAccountsForBackup(): List<AccountEntity>
    @Query("SELECT * FROM budgets ORDER BY monthKey")
    suspend fun allBudgetsForBackup(): List<BudgetEntity>
    @Query("SELECT * FROM budgets ORDER BY monthKey")
    fun observeAllBudgets(): Flow<List<BudgetEntity>>
    @Query("UPDATE budgets SET rollover = :enabled, updatedAt = :now WHERE monthKey = :monthKey")
    suspend fun setBudgetRollover(monthKey: String, enabled: Boolean, now: Long)
    @Query("SELECT * FROM recurring_transactions ORDER BY id")
    suspend fun allRulesForBackup(): List<RecurringTransactionEntity>

    @Transaction
    suspend fun backupSnapshot(): com.moasseum.app.data.FullBackup = com.moasseum.app.data.FullBackup(
        allTransactionsForBackup(), allBudgetsForBackup(), allRulesForBackup(), allAccountsForBackup(),
    )

    @Transaction
    suspend fun restoreSnapshot(backup: com.moasseum.app.data.FullBackup) {
        require(!backup.legacy)
        // Keep existing notification fingerprints/statuses so restoration cannot replay old alerts.
        deleteAllTransactions(); deleteAllBudgets(); deleteAllRecurringRules(); deleteAllAccounts()
        backup.accounts.forEach { upsertAccount(it) }
        backup.transactions.forEach { insertTransaction(it) }
        backup.budgets.forEach { upsertBudget(it) }
        backup.recurringRules.forEach { insertRecurringRule(it) }
    }
    @Query("SELECT * FROM accounts ORDER BY archived ASC, name ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountEntity)

    @Query("UPDATE accounts SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun archiveAccount(id: String, archived: Boolean, now: Long)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): TransactionEntity?

    @Query("UPDATE transactions SET accountId = :accountId, updatedAt = :now WHERE id = :id AND deletedAt IS NULL AND type != 'TRANSFER'")
    suspend fun setTransactionAccount(id: Long, accountId: String?, now: Long)

    @Transaction
    suspend fun saveLinkedTransaction(transaction: TransactionEntity): Long {
        require(transaction.amount in 1..1_000_000_000_000L)
        transaction.accountId?.let { require(getAccount(it)?.archived == false) { "사용 중인 계좌를 선택해 주세요." } }
        if (transaction.type == "TRANSFER") {
            require(transaction.accountId != null && transaction.destinationAccountId != null && transaction.accountId != transaction.destinationAccountId) { "서로 다른 두 계좌를 선택해 주세요." }
            require(getAccount(transaction.destinationAccountId)?.archived == false) { "받는 계좌를 확인해 주세요." }
        } else require(transaction.destinationAccountId == null)
        return insertTransaction(transaction)
    }

    @Query("UPDATE transactions SET amount = :amount, occurredAt = :occurredAt, timezone = :timezone, accountId = :fromId, destinationAccountId = :toId, merchant = :merchant, memo = :memo, updatedAt = :now WHERE id = :id AND type = 'TRANSFER' AND deletedAt IS NULL")
    suspend fun updateTransferFields(id: Long, amount: Long, occurredAt: Long, fromId: String, toId: String, merchant: String, memo: String, now: Long, timezone: String)

    @Transaction
    suspend fun saveTransferEdit(id: Long, amount: Long, occurredAt: Long, fromId: String, toId: String, memo: String, now: Long, timezone: String = ZoneId.systemDefault().id) {
        require(amount in 1..1_000_000_000_000L && fromId != toId)
        val from = getAccount(fromId)
        val to = getAccount(toId)
        require(from?.archived == false && to?.archived == false) { "사용 중인 두 계좌를 선택해 주세요." }
        require(getTransaction(id)?.let { it.type == "TRANSFER" && it.deletedAt == null } == true)
        updateTransferFields(id, amount, occurredAt, fromId, toId, "${from.name} → ${to.name}", memo, now, timezone)
    }

    @Transaction
    suspend fun linkTransactionAccount(id: Long, accountId: String?, now: Long) {
        accountId?.let { require(getAccount(it)?.archived == false) }
        setTransactionAccount(id, accountId, now)
    }

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("DELETE FROM notification_candidates")
    suspend fun deleteAllNotificationCandidates()

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurringRules()

    @Transaction
    suspend fun clearAllLocalRecords() {
        deleteAllTransactions()
        deleteAllBudgets()
        deleteAllNotificationCandidates()
        deleteAllRecurringRules()
        deleteAllAccounts()
    }

    @Query(
        """
        SELECT * FROM transactions
        WHERE deletedAt IS NULL
        ORDER BY occurredAt DESC, id DESC
        """,
    )
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM recurring_transactions ORDER BY isActive DESC, dayOfMonth ASC, id ASC")
    fun observeRecurringRules(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND nextOccurrenceDate <= :today ORDER BY nextOccurrenceDate ASC, id ASC")
    suspend fun getDueRecurringRules(today: String): List<RecurringTransactionEntity>

    @Insert
    suspend fun insertRecurringRule(rule: RecurringTransactionEntity): Long

    @Query("UPDATE recurring_transactions SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setRecurringRuleActive(id: Long, isActive: Boolean, updatedAt: Long)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRecurringRule(id: Long)

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getRecurringRule(id: Long): RecurringTransactionEntity?

    @Query("""
        UPDATE recurring_transactions SET amount = :amount, type = :type, merchant = :merchant,
            categoryKey = :categoryKey, memo = :memo, paymentMethod = :paymentMethod,
            dayOfMonth = :dayOfMonth, nextOccurrenceDate = :nextOccurrenceDate, updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateRecurringRuleFields(
        id: Long, amount: Long, type: String, merchant: String, categoryKey: String,
        memo: String, paymentMethod: String, dayOfMonth: Int, nextOccurrenceDate: String, updatedAt: Long,
    )

    @Transaction
    suspend fun editRecurringRule(
        id: Long, amount: Long, type: String, merchant: String, categoryKey: String,
        memo: String, paymentMethod: String, dayOfMonth: Int, today: String, now: Long,
    ) {
        val existing = getRecurringRule(id) ?: error("반복 거래가 없어요. 목록을 다시 확인해 주세요.")
        val next = editedRecurringOccurrence(
            LocalDate.parse(existing.nextOccurrenceDate), existing.dayOfMonth, dayOfMonth, LocalDate.parse(today),
        )
        updateRecurringRuleFields(id, amount, type, merchant, categoryKey, memo, paymentMethod, dayOfMonth, next.toString(), now)
    }

    @Query("UPDATE recurring_transactions SET nextOccurrenceDate = :nextOccurrenceDate, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRecurringNextOccurrence(id: Long, nextOccurrenceDate: String, updatedAt: Long)

    @Transaction
    suspend fun postDueRecurringTransactions(todayValue: String, timezone: String, now: Long): Int {
        val today = LocalDate.parse(todayValue)
        val zoneId = ZoneId.of(timezone)
        var inserted = 0
        getDueRecurringRules(todayValue).forEach { rule ->
            var occurrence = LocalDate.parse(rule.nextOccurrenceDate)
            var generated = 0
            while (!occurrence.isAfter(today) && generated < 120) {
                val occurredAt = occurrence.atStartOfDay(zoneId).toInstant().toEpochMilli()
                insertTransaction(
                    TransactionEntity(
                        type = rule.type,
                        amount = rule.amount,
                        occurredAt = occurredAt,
                        timezone = timezone,
                        categoryKey = rule.categoryKey,
                        merchant = rule.merchant,
                        memo = rule.memo,
                        paymentMethod = rule.paymentMethod,
                        source = "RECURRING",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                occurrence = nextRecurringOccurrence(occurrence, rule.dayOfMonth)
                generated++
                inserted++
            }
            updateRecurringNextOccurrence(rule.id, occurrence.toString(), now)
        }
        return inserted
    }

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, deletedAt: Long)

    @Query("UPDATE transactions SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreTransaction(id: Long, updatedAt: Long)

    @Query(
        """
        UPDATE transactions SET
            type = :type,
            amount = :amount,
            occurredAt = :occurredAt,
            timezone = :timezone,
            categoryKey = :categoryKey,
            merchant = :merchant,
            memo = :memo,
            paymentMethod = :paymentMethod,
            updatedAt = :updatedAt
        WHERE id = :id AND deletedAt IS NULL AND type != 'TRANSFER' AND :type != 'TRANSFER'
        """,
    )
    suspend fun updateTransaction(
        id: Long,
        type: String,
        amount: Long,
        occurredAt: Long,
        timezone: String,
        categoryKey: String,
        merchant: String,
        memo: String,
        paymentMethod: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM transactions
            WHERE deletedAt IS NULL
                AND type = :type AND amount = :amount AND occurredAt = :occurredAt
                AND categoryKey = :categoryKey AND merchant = :merchant
                AND memo = :memo AND paymentMethod = :paymentMethod
                AND ((type = 'TRANSFER' AND accountId IS :accountId AND destinationAccountId IS :destinationAccountId)
                     OR (type != 'TRANSFER' AND (:accountId IS NULL OR accountId IS :accountId)))
        )
        """,
    )
    suspend fun transactionExists(
        type: String,
        amount: Long,
        occurredAt: Long,
        categoryKey: String,
        merchant: String,
        memo: String,
        paymentMethod: String,
        accountId: String? = null,
        destinationAccountId: String? = null,
    ): Boolean

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey LIMIT 1")
    suspend fun getBudget(monthKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey LIMIT 1")
    fun observeBudget(monthKey: String): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query(
        """
        SELECT * FROM notification_candidates
        WHERE status = 'PENDING'
        ORDER BY postedAt DESC, id DESC
        """,
    )
    fun observePendingNotificationCandidates(): Flow<List<NotificationCandidateEntity>>

    @Query("SELECT * FROM notification_candidates WHERE id = :id LIMIT 1")
    suspend fun getNotificationCandidate(id: Long): NotificationCandidateEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationCandidate(candidate: NotificationCandidateEntity): Long

    @Query("UPDATE notification_candidates SET status = :status WHERE id = :id")
    suspend fun updateNotificationCandidateStatus(id: Long, status: String)

    @Query("UPDATE notification_candidates SET status = 'DISMISSED' WHERE status = 'PENDING' AND packageName IN (:packages)")
    suspend fun dismissExcludedNotificationCandidates(packages: List<String>)

    @Transaction
    suspend fun acceptCandidateTransaction(id: Long, transaction: TransactionEntity): Boolean {
        if (getNotificationCandidate(id)?.status != "PENDING") return false
        insertTransaction(transaction)
        updateNotificationCandidateStatus(id, "ACCEPTED")
        return true
    }

    @Query("UPDATE transactions SET categoryKey = :replacementKey, updatedAt = :updatedAt WHERE categoryKey = :deletedKey")
    suspend fun reassignTransactionCategory(deletedKey: String, replacementKey: String, updatedAt: Long)

    @Query("UPDATE recurring_transactions SET categoryKey = :replacementKey, updatedAt = :updatedAt WHERE categoryKey = :deletedKey")
    suspend fun reassignRecurringCategory(deletedKey: String, replacementKey: String, updatedAt: Long)

    @Query("UPDATE notification_candidates SET categoryKey = :replacementKey WHERE categoryKey = :deletedKey")
    suspend fun reassignNotificationCandidateCategory(deletedKey: String, replacementKey: String)

    @Transaction
    suspend fun deleteCustomCategory(deletedKey: String, replacementKey: String, updatedAt: Long) {
        reassignTransactionCategory(deletedKey, replacementKey, updatedAt)
        reassignRecurringCategory(deletedKey, replacementKey, updatedAt)
        reassignNotificationCandidateCategory(deletedKey, replacementKey)
    }
}
