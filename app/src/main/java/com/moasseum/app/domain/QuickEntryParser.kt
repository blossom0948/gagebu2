package com.moasseum.app.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class ParsedQuickEntry(
    val amount: Long,
    val type: TransactionType,
    val merchant: String,
)

/** Parses a single, explicit quick-entry phrase without sending it to a server. */
fun parseQuickEntry(input: String): ParsedQuickEntry? {
    if (input.length !in 1..240) return null
    val amountPattern = Regex("(?<![0-9])([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,15})(?:\\.([0-9]{1,3}))?\\s*(억|만|천|백)?\\s*원?(?![0-9])")
    val amountMatch = amountPattern.findAll(input).lastOrNull() ?: return null
    val whole = amountMatch.groupValues[1].replace(",", "")
    val decimal = amountMatch.groupValues[2]
    val unit = amountMatch.groupValues[3]
    val parsed = runCatching {
        BigDecimal(whole + if (decimal.isEmpty()) "" else ".$decimal")
            .multiply(BigDecimal(when (unit) { "억" -> "100000000"; "만" -> "10000"; "천" -> "1000"; "백" -> "100"; else -> "1" }))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()?.takeIf { it in 1..1_000_000_000_000L } ?: return null

    val context = input.replace(amountMatch.value, " ")
        .replace(Regex("[0-9]{1,4}[/.-][0-9]{1,2}([/.-][0-9]{1,4})?"), " ")
        .replace(Regex("[0-9]{1,2}:[0-9]{2}"), " ")
    val isIncome = Regex("입금|수입|받았|받음|월급|급여|환급|정산받", RegexOption.IGNORE_CASE).containsMatchIn(context)
    val merchant = context
        .replace(Regex("오늘|어제|내일|방금|입금|수입|지출|결제|사용|썼어요|썼어|받았어요|받았어|받음|월급|급여|환급|정산받(?:았어요|았어|음)?", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("[\\p{Punct}\\p{Z}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
        .take(80)
        .takeIf(String::isNotBlank) ?: return null
    return ParsedQuickEntry(parsed, if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE, merchant)
}
