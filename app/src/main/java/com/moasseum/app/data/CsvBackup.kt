package com.moasseum.app.data

import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import java.time.LocalDate

data class ImportedTransaction(
    val type: TransactionType,
    val amount: Long,
    val occurredAt: Long,
    val categoryKey: String,
    val merchant: String,
    val memo: String,
    val paymentMethod: String,
    val source: String,
)

object CsvBackup {
    private val headers = listOf(
        "id", "type", "amount", "occurredAt", "categoryKey", "merchant", "memo", "paymentMethod", "source",
    )

    fun encode(transactions: List<Transaction>): String = buildString {
        append('\uFEFF')
        append(headers.joinToString(","))
        append("\r\n")
        transactions.forEach { transaction ->
            listOf(
                transaction.id.toString(),
                transaction.type.name,
                transaction.amount.toString(),
                transaction.occurredAt.toString(),
                transaction.categoryKey,
                transaction.merchant,
                transaction.memo,
                transaction.paymentMethod,
                transaction.source,
            ).joinTo(this, separator = ",", transform = ::escape)
            append("\r\n")
        }
    }

    fun decode(csv: String): List<ImportedTransaction> {
        val rows = parseRows(csv.removePrefix("\uFEFF"))
            .filterNot { row -> row.all(String::isBlank) }
        require(rows.size >= 2) { "가져올 거래가 없거나 CSV 파일이 비어 있어요." }
        val header = rows.first().map(String::trim)
        val required = setOf("type", "amount", "occurredAt", "categoryKey", "merchant")
        require(header.toSet().containsAll(required)) { "모아씀 CSV 형식이 아니에요. 내역 화면에서 내보낸 CSV를 선택해 주세요." }
        val index = header.withIndex().associate { it.value to it.index }

        return rows.drop(1).mapIndexedNotNull { rowIndex, row ->
            if (row.all(String::isBlank)) return@mapIndexedNotNull null
            fun value(key: String): String = row.getOrNull(index[key] ?: -1)?.trim().orEmpty()
            val lineNumber = rowIndex + 2
            val type = runCatching { TransactionType.valueOf(value("type")) }
                .getOrElse { throw IllegalArgumentException("${lineNumber}번째 줄의 거래 유형을 확인해 주세요.") }
            val amount = value("amount").toLongOrNull()?.takeIf { it > 0L }
                ?: throw IllegalArgumentException("${lineNumber}번째 줄의 금액을 확인해 주세요.")
            val occurredAt = value("occurredAt").toLongOrNull()
                ?: runCatching { LocalDate.parse(value("occurredAt")).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
                    .getOrElse { throw IllegalArgumentException("${lineNumber}번째 줄의 날짜를 확인해 주세요.") }
            val merchant = value("merchant").takeIf(String::isNotBlank)
                ?: throw IllegalArgumentException("${lineNumber}번째 줄의 가맹점이 비어 있어요.")
            ImportedTransaction(
                type = type,
                amount = amount,
                occurredAt = occurredAt,
                categoryKey = value("categoryKey").ifBlank { "OTHER" },
                merchant = merchant,
                memo = value("memo"),
                paymentMethod = value("paymentMethod").ifBlank { "카드" },
                source = value("source").ifBlank { "IMPORT" },
            )
        }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun parseRows(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < csv.length) {
            val character = csv[index]
            when {
                character == '"' && quoted && csv.getOrNull(index + 1) == '"' -> {
                    field.append('"')
                    index++
                }
                character == '"' -> quoted = !quoted
                character == ',' && !quoted -> {
                    row += field.toString()
                    field.clear()
                }
                (character == '\n' || character == '\r') && !quoted -> {
                    if (character == '\r' && csv.getOrNull(index + 1) == '\n') index++
                    row += field.toString()
                    rows += row
                    row = mutableListOf()
                    field.clear()
                }
                else -> field.append(character)
            }
            index++
        }
        require(!quoted) { "CSV 따옴표 짝이 맞지 않아요." }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            rows += row
        }
        return rows
    }
}
