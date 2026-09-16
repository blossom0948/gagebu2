package com.moasseum.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.moasseum.app.domain.nextRecurringOccurrence
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
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
        WHERE id = :id AND deletedAt IS NULL
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
