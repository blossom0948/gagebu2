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
    val accountId: String? = null,
    val destinationAccountId: String? = null,
    val timezone: String? = null,
    val installmentGroupId: String? = null,
    val installmentNumber: Int? = null,
    val installmentCount: Int? = null,
)

object CsvBackup {
    private val headers = listOf(
        "id", "type", "amount", "occurredAt", "categoryKey", "merchant", "memo", "paymentMethod", "source",
        "accountId", "destinationAccountId", "timezone", "installmentGroupId", "installmentNumber", "installmentCount",
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
                transaction.accountId.orEmpty(),
                transaction.destinationAccountId.orEmpty(),
                transaction.timezone,
                transaction.installmentGroupId.orEmpty(),
                transaction.installmentNumber?.toString().orEmpty(),
                transaction.installmentCount?.toString().orEmpty(),
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

        val imported = rows.drop(1).mapIndexedNotNull { rowIndex, row ->
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
            val accountId = value("accountId").takeIf(String::isNotBlank)
            val destination = value("destinationAccountId").takeIf(String::isNotBlank)
            val timezone = value("timezone").takeIf(String::isNotBlank)?.also {
                require(runCatching { java.time.ZoneId.of(it) }.isSuccess) { "${lineNumber}번째 줄의 시간대를 확인해 주세요." }
            }
            val installmentGroupId = value("installmentGroupId").takeIf(String::isNotBlank)?.also {
                require(runCatching { java.util.UUID.fromString(it) }.isSuccess) { "${lineNumber}번째 줄의 할부 ID를 확인해 주세요." }
            }
            val installmentNumber = value("installmentNumber").takeIf(String::isNotBlank)?.toIntOrNull()
            val installmentCount = value("installmentCount").takeIf(String::isNotBlank)?.toIntOrNull()
            require((installmentGroupId == null && installmentNumber == null && installmentCount == null) ||
                (type == TransactionType.EXPENSE && installmentGroupId != null && installmentNumber != null && installmentNumber in 1..60 && installmentCount != null && installmentCount in 2..60 && installmentNumber <= installmentCount)) {
                "${lineNumber}번째 줄의 할부 정보를 확인해 주세요."
            }
            if (type == TransactionType.TRANSFER) require(accountId != null && destination != null && accountId != destination) { "${lineNumber}번째 줄의 이체 계좌를 확인해 주세요. 계좌까지 복원하려면 JSON 전체 백업을 사용하세요." }
            else require(destination == null)
            ImportedTransaction(
                type = type,
                amount = amount,
                occurredAt = occurredAt,
                categoryKey = value("categoryKey").ifBlank { "OTHER" },
                merchant = merchant,
                memo = value("memo"),
                paymentMethod = value("paymentMethod").ifBlank { "카드" },
                source = value("source").ifBlank { "IMPORT" },
                accountId = accountId,
                destinationAccountId = destination,
                timezone = timezone,
                installmentGroupId = installmentGroupId,
                installmentNumber = installmentNumber,
                installmentCount = installmentCount,
            )
        }
        require(imported.filter { it.installmentGroupId != null }.groupBy { it.installmentGroupId }.values.all { group ->
            val count = group.first().installmentCount
            count != null && group.size == count && group.mapNotNull { it.installmentNumber }.toSet() == (1..count).toSet() && group.all { it.installmentCount == count }
        }) { "CSV 할부 회차가 빠졌거나 중복됐어요." }
        return imported
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
