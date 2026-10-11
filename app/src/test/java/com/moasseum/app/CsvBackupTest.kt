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

    @Test fun `CSV round trip preserves installment group`() {
        val group = "971a8c7e-7f32-4e6b-9a74-82c22e218671"
        val entries = (1..2).map { number ->
            Transaction(
                id = number.toLong(), type = TransactionType.EXPENSE, amount = 5_000, occurredAt = number.toLong(),
                categoryKey = "SHOPPING", merchant = "가전", memo = "할부", paymentMethod = "카드", source = "INSTALLMENT",
                installmentGroupId = group, installmentNumber = number, installmentCount = 2,
            )
        }
        val decoded = CsvBackup.decode(CsvBackup.encode(entries))
        assertEquals(listOf(1, 2), decoded.map { it.installmentNumber })
        assertEquals(setOf(group), decoded.mapNotNull { it.installmentGroupId }.toSet())
    }

    @Test fun `CSV round trip preserves original currency and applied rate`() {
        val transaction = Transaction(
            id = 9L, type = TransactionType.EXPENSE, amount = 13_420L, occurredAt = 1_758_000_000_000L,
            categoryKey = "TRAVEL", merchant = "Hotel", memo = "", paymentMethod = "체크카드", source = "MANUAL",
            currency = "USD", originalAmount = "10", exchangeRateToKrw = "1342", exchangeRateDate = "2026-10-09",
        )
        val imported = CsvBackup.decode(CsvBackup.encode(listOf(transaction))).single()
        assertEquals("USD", imported.currency)
        assertEquals("10", imported.originalAmount)
        assertEquals("1342", imported.exchangeRate)
        assertEquals("2026-10-09", imported.exchangeRateDate)
    }

    @Test fun `CSV round trip preserves balance-only external deposits and linked account`() {
        val transaction = Transaction(
            id = 10L, type = TransactionType.EXTERNAL_DEPOSIT, amount = 5_000, occurredAt = 1_758_000_000_000L,
            categoryKey = "OTHER", merchant = "은행 이자", memo = "잔액만", paymentMethod = "계좌 입금",
            source = "EXTERNAL_DEPOSIT", accountId = "checking-1",
        )
        val imported = CsvBackup.decode(CsvBackup.encode(listOf(transaction))).single()
        assertEquals(TransactionType.EXTERNAL_DEPOSIT, imported.type)
        assertEquals("checking-1", imported.accountId)
        assertEquals("EXTERNAL_DEPOSIT", imported.source)
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
