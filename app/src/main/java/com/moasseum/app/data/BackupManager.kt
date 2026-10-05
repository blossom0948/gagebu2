package com.moasseum.app.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class BackupManager(private val context: Context, private val repository: FinanceRepository, private val preferences: UserPreferencesRepository) {
    private val mutex = Mutex()
    val safetyFile: File get() = File(context.filesDir, "before-restore.json")
    suspend fun snapshot(): FullBackup = repository.backupSnapshot().copy(settings = preferences.backupSettings())

    suspend fun restore(backup: FullBackup): Int = mutex.withLock {
        if (backup.legacy) {
            return@withLock repository.importTransactions(backup.transactions.map {
                ImportedTransaction(com.moasseum.app.domain.TransactionType.valueOf(it.type), it.amount, it.occurredAt, it.categoryKey, it.merchant, it.memo, it.paymentMethod, it.source, it.accountId, it.destinationAccountId)
            })
        }
        val validated = FullBackupCodec.decode(FullBackupCodec.encode(backup))
        val before = snapshot()
        val safetyBytes = FullBackupCodec.encode(before).toByteArray(Charsets.UTF_8)
        require(safetyBytes.size <= FullBackupCodec.MAX_BYTES) { "현재 데이터가 안전 백업 용량 한도를 넘어요. 복원을 중단했습니다." }
        // Durable recovery copy is written before replacing anything; a failed write aborts restoration.
        android.util.AtomicFile(safetyFile).let { file ->
            val stream = file.startWrite()
            try { stream.write(safetyBytes); file.finishWrite(stream) }
            catch (error: Throwable) { file.failWrite(stream); throw error }
        }
        try {
            repository.restoreBackup(validated)
            preferences.restoreSettings(requireNotNull(validated.settings))
        } catch (error: Throwable) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                runCatching { repository.restoreBackup(before); preferences.restoreSettings(requireNotNull(before.settings)) }
                .onFailure { throw IllegalStateException("복원을 완료하지 못했어요. 복원 전 안전 백업을 내보내 다시 복원해 주세요.", error) }
            }
            throw error
        }
        validated.transactions.count { it.deletedAt == null }
    }
}
