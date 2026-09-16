package com.moasseum.app

import com.moasseum.app.data.CsvBackup
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CsvBackupTest {
    @Test
    fun `CSV export and import preserve quoted Korean transaction fields`() {
        val transaction = Transaction(
            id = 41L,
            type = TransactionType.EXPENSE,
            amount = 12_300L,
            occurredAt = 1_758_000_000_000L,
            categoryKey = "FOOD",
            merchant = "카페, 모아씀",
            memo = "친구와 ${34.toChar()}모아${34.toChar()} 만남\n영수증 확인",
            paymentMethod = "체크카드",
            source = "MANUAL",
        )

        val imported = CsvBackup.decode(CsvBackup.encode(listOf(transaction))).single()

        assertEquals(TransactionType.EXPENSE, imported.type)
        assertEquals(transaction.amount, imported.amount)
        assertEquals(transaction.occurredAt, imported.occurredAt)
        assertEquals(transaction.categoryKey, imported.categoryKey)
        assertEquals(transaction.merchant, imported.merchant)
        assertEquals(transaction.memo, imported.memo)
        assertEquals(transaction.paymentMethod, imported.paymentMethod)
        assertEquals(transaction.source, imported.source)
    }

    @Test
    fun `CSV import rejects unknown schemas and invalid amounts`() {
        assertThrows(IllegalArgumentException::class.java) {
            CsvBackup.decode("name,total\n카페,12000")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CsvBackup.decode("type,amount,occurredAt,categoryKey,merchant\nEXPENSE,0,1758000000000,FOOD,카페")
        }
    }
}
