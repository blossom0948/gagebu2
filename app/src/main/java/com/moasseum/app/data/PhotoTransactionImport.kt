package com.moasseum.app.data

import com.moasseum.app.domain.AiCandidateSource
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.notification.PaymentNotificationParser
import java.time.LocalDate

/** A locally OCR'd record candidate. No source image or OCR text is retained. */
data class PhotoTransactionCandidate(
    val transaction: AiTransactionCandidate,
    val paymentMethod: String = "금융앱 캡처",
    val sourceImageId: String = "",
    val id: String = java.util.UUID.randomUUID().toString(),
)

data class PhotoImportPreview(
    val candidates: List<PhotoTransactionCandidate>,
    val duplicateCount: Int,
)

data class PhotoImportSaveResult(val insertedIds: List<Long>, val duplicateCount: Int) {
    val insertedCount: Int get() = insertedIds.size
}

sealed interface PhotoImportState {
    data object Idle : PhotoImportState
    data class Loading(val completedImages: Int, val totalImages: Int) : PhotoImportState
    data class Review(
        val candidates: List<PhotoTransactionCandidate>,
        val duplicateCount: Int,
        val imageCount: Int,
        val failedImageCount: Int,
        val unselectedCandidateIds: Set<String> = emptySet(),
    ) : PhotoImportState
    data class Error(val message: String) : PhotoImportState
}

/**
 * Parses transaction rows from Korean banking/payment-app screenshots. This is
 * intentionally conservative: balance, credit-limit, cancelled and summary
 * values are not transactions, and every returned item still requires review.
 */
object PhotoTransactionImport {
    private val fullDate = Regex("(?<!\\d)(20\\d{2})\\s*[./년-]\\s*(\\d{1,2})\\s*[./월-]\\s*(\\d{1,2})")
    private val monthDayText = Regex("(?<!\\d)(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일?")
    private val monthDayPunctuation = Regex("(?<!\\d)(\\d{1,2})\\s*[./-]\\s*(\\d{1,2})(?!\\s*[:.])")
    private val money = Regex("(?<![\\d/])([-+＋−]?\\s*(?:\\d{1,3}(?:,\\d{3})+|\\d{4,}|\\d{1,3}(?=\\s*원)))(?:\\s*원)?(?!\\d)")
    private val excludedLine = Regex("잔액|출금가능|사용가능|한도|누적|이번\\s*달.*(?:지출|사용)|이번달.*(?:지출|사용)|월간.*(?:지출|사용)|당월.*(?:지출|사용)|총\\s*지출|합계|총액|결제예정|청구예정|입금예정|출금예정|계좌번호|카드번호|승인번호|거래번호|주문번호|결제번호|인증번호|접수번호|취소|승인취소|할인|적립|포인트|캐시백")
    private val nonMerchantLine = Regex("^(?:원|KRW|전체|내역|거래내역|이용내역|결제내역|입출금내역|최근|오늘|어제|상세|더보기|정렬|필터|검색|토스|카카오페이|네이버페이|삼성페이|카드|신용카드|체크카드|신한카드|현대카드|국민카드|KB국민카드|우리카드|하나카드|롯데카드|삼성카드|NH농협카드|계좌|입금|출금|이체|결제|승인|취소|완료|잔액|포인트|수수료|혜택|월간|주간|예정)(?:\\s.*)?$")

    fun extract(
        ocrText: String,
        today: LocalDate = LocalDate.now(),
        defaultPaymentMethod: String = "금융앱 캡처",
    ): List<PhotoTransactionCandidate> {
        val lines = ocrText.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.isEmpty()) return emptyList()

        val amountLines = lines.indices.filter { index ->
            !excludedLine.containsMatchIn(lines[index]) && extractAmount(lines[index]) != null
        }
        if (amountLines.isEmpty()) return emptyList()

        val results = mutableListOf<PhotoTransactionCandidate>()
        var activeDate: LocalDate? = null
        for (index in lines.indices) {
            dateIn(lines[index], today)?.let { activeDate = it }
            if (index !in amountLines) continue

            val line = lines[index]
            if (excludedLine.containsMatchIn(line)) continue
            val amountMatch = extractAmount(line) ?: continue
            val previousAmount = amountLines.lastOrNull { it < index } ?: -1
            val nextAmount = amountLines.firstOrNull { it > index } ?: lines.size
            val contextStart = maxOf(previousAmount + 1, index - 3, 0)
            val contextEnd = minOf(nextAmount - 1, index + 2, lines.lastIndex)
            val explicitDate = dateIn(line, today)
            val occurredDate = explicitDate ?: activeDate ?: today
            val transactionContext = (maxOf(contextStart, index - 2)..minOf(contextEnd, index + 1))
                .map(lines::get).joinToString(" ")
            val type = inferType(amountMatch.value, transactionContext)
            val merchant = merchantFrom(line, lines, index, previousAmount, nextAmount)
            if (merchant.isBlank()) continue

            val inferredCategory = PaymentNotificationParser.inferCategoryFrom(merchant)
            val needsConfirmation = buildList {
                if (merchant == "알 수 없음") add("merchant")
                if (explicitDate == null && activeDate == null) add("date")
                if (inferredCategory == "OTHER") add("category")
            }
            results += PhotoTransactionCandidate(
                transaction = AiTransactionCandidate(
                    type = type,
                    amount = amountMatch.amount,
                    occurredDate = occurredDate,
                    categoryKey = inferredCategory,
                    merchant = merchant,
                    memo = "금융앱 내역 사진에서 인식",
                    source = AiCandidateSource.LOCAL,
                    amountConfidence = 0.78,
                    dateConfidence = if (explicitDate != null || activeDate != null) 0.72 else 0.4,
                    categoryConfidence = if (inferredCategory == "OTHER") 0.35 else 0.65,
                    needsConfirmation = needsConfirmation,
                ),
                paymentMethod = paymentMethodFrom((maxOf(contextStart, index - 2)..minOf(contextEnd, index + 1)).map(lines::get), defaultPaymentMethod),
            )
        }
        return results
    }

    /** Receipt photos remain supported; a transaction-list screenshot can yield many rows. */
    fun extractWithReceiptFallback(
        ocrText: String,
        today: LocalDate = LocalDate.now(),
    ): List<PhotoTransactionCandidate> {
        val explicitReceipt = Regex("영수증|사업자\\s*등록번호|공급받는자|공급자").containsMatchIn(ocrText)
        if (explicitReceipt) {
            val receipt = ReceiptOcr.candidateFromText(ocrText, today) ?: return emptyList()
            return listOf(PhotoTransactionCandidate(receipt, "카드"))
        }
        val rows = extract(ocrText, today)
        if (rows.size > 1) return rows
        val receiptSummary = Regex("합계|총액|총\\s*결제|승인번호|공급가액|부가세").containsMatchIn(ocrText)
        if (rows.size == 1 && receiptSummary) {
            ReceiptOcr.candidateFromText(ocrText, today)?.let {
                return listOf(PhotoTransactionCandidate(it, "카드"))
            }
        }
        if (rows.isNotEmpty()) return rows
        return emptyList()
    }

    fun preview(
        candidates: List<PhotoTransactionCandidate>,
        existing: List<Transaction>,
    ): PhotoImportPreview {
        val accepted = mutableListOf<PhotoTransactionCandidate>()
        var duplicates = 0
        candidates.forEach { candidate ->
            if (existing.any { isDuplicate(candidate.transaction, it) } || accepted.any {
                    it.sourceImageId != candidate.sourceImageId && sameTransaction(candidate.transaction, it.transaction)
                }
            ) {
                duplicates++
            } else {
                accepted += candidate
            }
        }
        return PhotoImportPreview(accepted, duplicates)
    }

    fun isDuplicate(candidate: AiTransactionCandidate, existing: Transaction): Boolean =
        existing.type == candidate.type && existing.amount == candidate.amount &&
            existing.occurredDate == candidate.occurredDate && merchantMatches(candidate.merchant, existing.merchant)

    fun isDuplicate(
        candidate: AiTransactionCandidate,
        existingType: String,
        existingAmount: Long,
        existingDate: LocalDate,
        existingMerchant: String,
    ): Boolean = candidate.type.name == existingType && candidate.amount == existingAmount &&
        candidate.occurredDate == existingDate && merchantMatches(candidate.merchant, existingMerchant)

    fun sameTransaction(left: AiTransactionCandidate, right: AiTransactionCandidate): Boolean =
        left.type == right.type && left.amount == right.amount && left.occurredDate == right.occurredDate &&
            merchantMatches(left.merchant, right.merchant)

    private fun merchantMatches(left: String, right: String): Boolean {
        val a = normalizeMerchant(left)
        val b = normalizeMerchant(right)
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        return minOf(a.length, b.length) >= 4 && (a.startsWith(b) || b.startsWith(a))
    }

    private fun normalizeMerchant(value: String): String = value.lowercase()
        .replace("주식회사", "")
        .replace("유한회사", "")
        .replace(Regex("[^\\p{L}\\p{N}]"), "")
        .replace(Regex("\\d{3,}$"), "")

    private data class AmountMatch(val amount: Long, val value: String)

    private fun extractAmount(line: String): AmountMatch? {
        val withoutDates = fullDate.replace(line, " ")
        val withoutMonthDay = monthDayText.replace(withoutDates, " ")
        val match = money.find(withoutMonthDay) ?: return null
        val raw = match.groupValues[1].replace(" ", "").replace("＋", "+").replace("−", "-")
        val amount = raw.removePrefix("+").removePrefix("-").replace(",", "").toLongOrNull()
            ?.takeIf { it in 1..1_000_000_000_000L } ?: return null
        return AmountMatch(amount, raw)
    }

    private fun dateIn(text: String, today: LocalDate): LocalDate? {
        fullDate.find(text)?.let { match ->
            return runCatching {
                LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
            }.getOrNull()
        }
        val monthDay = monthDayText.find(text) ?: monthDayPunctuation.find(text) ?: return null
        val month = monthDay.groupValues[1].toIntOrNull() ?: return null
        val day = monthDay.groupValues[2].toIntOrNull() ?: return null
        val inferredYear = if (month > today.monthValue + 1 && today.monthValue <= 2) today.year - 1 else today.year
        val date = runCatching { LocalDate.of(inferredYear, month, day) }.getOrNull() ?: return null
        return if (date.isAfter(today.plusDays(7))) runCatching { date.minusYears(1) }.getOrNull() else date
    }

    private fun inferType(amountText: String, context: String): TransactionType {
        val lower = context.lowercase()
        val sign = amountText.trim().firstOrNull()
        if (sign == '+' || sign == '＋') return TransactionType.INCOME
        if (sign == '-' || sign == '−') return TransactionType.EXPENSE
        val incomeSignal = listOf("입금", "받았", "수입", "환급", "급여", "월급", "예금이자", "이자입금").any(lower::contains)
        val expenseSignal = listOf("결제", "승인", "출금", "구매", "사용", "납부").any(lower::contains)
        return if (incomeSignal && !expenseSignal) TransactionType.INCOME else TransactionType.EXPENSE
    }

    private fun merchantFrom(
        amountLine: String,
        allLines: List<String>,
        index: Int,
        previousAmount: Int,
        nextAmount: Int,
    ): String {
        val inline = amountLine
            .replace(money, " ")
            .replace(fullDate, " ")
            .replace(monthDayText, " ")
            .replace(Regex("\\b\\d{1,2}:\\d{2}\\b"), " ")
        usableMerchant(inline)?.let { return it }

        val candidates = mutableListOf<Pair<Int, String>>()
        // Some banking apps put the date and payment label between merchant and amount.
        for (distance in 1..4) {
            val before = index - distance
            if (before > previousAmount && before >= 0) usableMerchant(allLines[before])?.let { candidates += distance to it }
            val after = index + distance
            if (after < nextAmount && after <= allLines.lastIndex) usableMerchant(allLines[after])?.let { candidates += distance to it }
        }
        return candidates.minByOrNull { it.first }?.second ?: "알 수 없음"
    }

    private fun usableMerchant(raw: String): String? {
        val line = raw.trim()
            .replace(fullDate, " ")
            .replace(monthDayText, " ")
            .replace(monthDayPunctuation, " ")
            .replace(Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b"), " ")
            .replace(Regex("[-+＋−]?\\s*[0-9][0-9,]*\\s*(?:원|KRW)?", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\b\\d{4,}\\b"), " ")
            .replace(Regex("[·|•:：()\\[\\]{}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '−', '+', '원')
        if (line.length !in 2..60 || line.none(Char::isLetter)) return null
        if (nonMerchantLine.containsMatchIn(line) || excludedLine.containsMatchIn(line)) return null
        if (line.contains("원") && line.length <= 4) return null
        return line
    }

    private fun paymentMethodFrom(context: List<String>, default: String): String {
        val text = context.joinToString(" ").lowercase()
        return when {
            "토스" in text -> "토스"
            "카카오페이" in text -> "카카오페이"
            "네이버페이" in text -> "네이버페이"
            "삼성페이" in text -> "삼성페이"
            "현대카드" in text -> "현대카드"
            "신한카드" in text -> "신한카드"
            "국민카드" in text || "kb국민" in text -> "KB국민카드"
            "우리카드" in text -> "우리카드"
            "하나카드" in text -> "하나카드"
            "롯데카드" in text -> "롯데카드"
            "삼성카드" in text -> "삼성카드"
            else -> default
        }
    }

}
