package com.moasseum.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
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

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteTransaction(id: Long, deletedAt: Long)

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
}
