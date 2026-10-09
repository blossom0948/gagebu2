package com.moasseum.app.data

import com.moasseum.app.domain.AiCandidateSource
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.notification.PaymentNotificationParser
import java.time.LocalDate

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
    private val dayHeading = Regex("(?<!\\d)(\\d{1,2})\\s*일\\s*(?:월|화|수|목|금|토|일)(?:요일)?")
    private val monthHeader = Regex("^[←<‹]?\\s*(\\d{1,2})\\s*(?:월)?\\s*$")
    private val money = Regex("(?<![\\d/])([-+＋−]?\\s*(?:\\d{1,3}(?:,\\d{3})+|\\d{4,}|\\d{1,3}(?=\\s*원)))(?:\\s*원)?(?!\\d)")
    private val excludedLine = Regex("잔액|출금가능|사용가능|한도|누적|이번\\s*달.*(?:지출|사용)|이번달.*(?:지출|사용)|월간.*(?:지출|사용)|당월.*(?:지출|사용)|총\\s*지출|합계|총액|결제예정|청구예정|입금예정|출금예정|계좌번호|카드번호|승인번호|거래번호|주문번호|결제번호|인증번호|접수번호|취소|승인취소|할인|적립|포인트")
    private val nonMerchantLine = Regex("^(?:원|KRW|전체|내역|거래내역|이용내역|결제내역|입출금내역|최근|오늘|어제|상세|더보기|정렬|필터|검색|확인하기|토스|카카오페이|네이버페이|삼성페이|카드|신용카드|체크카드|신한카드|현대카드|국민카드|KB국민카드|우리카드|하나카드|롯데카드|삼성카드|NH농협카드|계좌|입금|출금|이체|결제|승인|취소|완료|잔액|포인트|수수료|혜택|월간|주간|예정|금액|가맹점|이용일|결제일|내역없음|총합계|(?:월|화|수|목|금|토|일)(?:요일)?|평소보다\\s*많이\\s*씀|캐시백\\s*가능한\\s*내역)(?:\\s.*)?$")
    private val transactionActionSignal = Regex("승인|결제|입금|출금|송금|보냈|받았|급여|월급|환급|당첨|복권|구매|납부")
    private val transactionTypeSignal = Regex("입금|받았|수입|환급|급여|월급|예금이자|이자입금|당첨|복권|결제|승인|출금|구매|사용|납부")
    private val financialPageSignal = Regex("카드.{0,4}(?:이용|승인|내역)|거래내역|이용내역|입출금|결제내역|송금내역")

    fun extract(
        ocrText: String,
        today: LocalDate = LocalDate.now(),
        defaultPaymentMethod: String = "금융앱 캡처",
    ): List<PhotoTransactionCandidate> {
        val lines = ocrText.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.isEmpty()) return emptyList()

        val amountLines = lines.indices.filter { index ->
            val line = lines[index]
            val amount = extractAmount(line)
            !excludedLine.containsMatchIn(line) && amount != null && hasFinancialEvidence(lines, index, amount, today)
        }
        if (amountLines.isEmpty()) return emptyList()

        val results = mutableListOf<PhotoTransactionCandidate>()
        var activeDate: LocalDate? = null
        // The visible month belongs to the top calendar header. A later `9월`
        // section heading must not retroactively date earlier October rows.
        var activeMonth = visibleMonth(lines, today)
        val firstSectionHeading = lines.indexOfFirst { dayHeading.containsMatchIn(it) }
        val inferredTopDate = if (firstSectionHeading > 0 && amountLines.any { it < firstSectionHeading }) {
            dateIn(lines[firstSectionHeading], today, activeMonth)?.plusDays(1)
        } else {
            null
        }
        for (index in lines.indices) {
            monthHeader.matchEntire(lines[index])?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?.takeIf { it in 1..12 && (lines[index].trim().startsWith("←") || lines[index].contains("월")) }
                ?.let { parsedMonth ->
                    if (parsedMonth != activeMonth) {
                        activeMonth = parsedMonth
                        activeDate = null
                    }
                }
            dateIn(lines[index], today, activeMonth)?.let { activeDate = it }
            if (index !in amountLines) continue

            val line = lines[index]
            if (excludedLine.containsMatchIn(line)) continue
            val amountMatch = extractAmount(line) ?: continue
            val previousAmount = amountLines.lastOrNull { it < index } ?: -1
            val nextAmount = amountLines.firstOrNull { it > index } ?: lines.size
            val contextStart = maxOf(previousAmount + 1, index - 3, 0)
            val contextEnd = minOf(nextAmount - 1, index + 2, lines.lastIndex)
            val explicitDate = dateIn(line, today, activeMonth)
            val inferredDate = inferredTopDate?.takeIf { firstSectionHeading > index }
            val occurredDate = explicitDate ?: activeDate ?: inferredDate ?: today
            val transactionContext = (index..minOf(nextAmount - 1, index + 2, lines.lastIndex))
                .map(lines::get).joinToString(" ")
            // Cancellation labels are frequently OCR'd on the subtitle line,
            // separate from the signed amount. Never import that row as spend.
            if (hasCancellationInRow(lines, index, nextAmount)) continue
            if (isOwnAccountTransfer(transactionContext)) continue
            val transferDirection = inferTransferDirection(transactionContext)
            val type = inferType(amountMatch.value, transactionContext, transferDirection)
            val merchant = merchantFrom(line, lines, index, previousAmount, nextAmount)
            if (merchant.isBlank()) continue

            val isCashbackCredit = transactionContext.contains("캐시백") && hasOwnAccount(transactionContext)
            val inferredCategory = if (isCashbackCredit || transferDirection != null) "FINANCE"
                else PaymentNotificationParser.inferCategoryFrom(merchant)
            val evidenceContext = (maxOf(index - 2, 0)..minOf(index + 2, lines.lastIndex)).map(lines::get).joinToString(" ")
            val hasExplicitType = amountMatch.value.trim().firstOrNull() in setOf('+', '＋', '-', '−') ||
                transferDirection != null || transactionTypeSignal.containsMatchIn(evidenceContext)
            val needsConfirmation = buildList {
                if (merchant == "알 수 없음") add("merchant")
                if (transferDirection != null && merchant.matches(Regex("[가-힣]{2,4}"))) add("merchant")
                if (explicitDate == null && activeDate == null && inferredDate == null) add("date")
                if (inferredCategory == "OTHER") add("category")
                if (!hasExplicitType) add("type")
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
                    dateConfidence = when {
                        explicitDate != null -> 0.82
                        activeDate != null -> 0.82
                        inferredDate != null -> 0.62
                        else -> 0.4
                    },
                    categoryConfidence = if (inferredCategory == "OTHER") 0.35 else 0.65,
                    needsConfirmation = needsConfirmation,
                ),
                paymentMethod = paymentMethodFrom((maxOf(contextStart, index - 2)..minOf(contextEnd, index + 1)).map(lines::get), defaultPaymentMethod),
            )
        }
        return results
    }

    private fun hasCancellationInRow(lines: List<String>, amountIndex: Int, nextAmountIndex: Int): Boolean {
        val end = minOf(amountIndex + 4, nextAmountIndex - 1, lines.lastIndex)
        if (end < amountIndex + 1) return false
        for (index in amountIndex + 1..end) {
            val line = lines[index]
            if (Regex("취소").containsMatchIn(line)) return true
            if (hasDateBoundary(lines, index, index) || extractAmount(line) != null || excludedLine.containsMatchIn(line)) return false
        }
        return false
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

    private fun visibleMonth(lines: List<String>, today: LocalDate): Int {
        return lines.take(6).mapIndexedNotNull { index, line ->
            val month = monthHeader.matchEntire(line)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?.takeIf { it in 1..12 } ?: return@mapIndexedNotNull null
            val explicitHeader = line.trim().startsWith("←") || line.contains("월")
            val bareTopHeader = index == 0 && line.trim().matches(Regex("\\d{1,2}")) &&
                lines.drop(1).take(4).any { it.contains("전체") || it.contains("입출금") || it.contains("카드") }
            month.takeIf { explicitHeader || bareTopHeader }
        }.firstOrNull() ?: today.monthValue
    }

    private fun hasFinancialEvidence(lines: List<String>, index: Int, amount: AmountMatch, today: LocalDate): Boolean {
        val start = maxOf(index - 2, 0)
        val end = minOf(index + 2, lines.lastIndex)
        val contextLines = lines.subList(start, end + 1)
        val hasMerchant = contextLines.withIndex().any { (offset, text) ->
            val absoluteIndex = start + offset
            absoluteIndex != index && !excludedLine.containsMatchIn(text) && usableMerchant(text) != null
        } || usableMerchant(lines[index].replace(money, " ")) != null
        if (!hasMerchant) return false

        val sign = amount.value.trim().firstOrNull()
        if (sign == '+' || sign == '＋' || sign == '-' || sign == '−') return true
        val usableContextLines = contextLines.filterNot(excludedLine::containsMatchIn)
        val forwardLines = mutableListOf(lines[index])
        for (next in index + 1..minOf(index + 2, lines.lastIndex)) {
            if (extractAmount(lines[next]) != null) break
            forwardLines += lines[next]
        }
        val context = forwardLines.joinToString(" ")
        if (inferTransferDirection(context) != null || (context.contains("캐시백") && hasOwnAccount(context))) return true
        if (transactionActionSignal.containsMatchIn(context)) return true

        val hasCurrency = Regex("원|KRW", RegexOption.IGNORE_CASE).containsMatchIn(lines[index])
        val hasDateOrTime = usableContextLines.any { dateIn(it, today, today.monthValue) != null } ||
            usableContextLines.any { Regex("오늘|어제|방금|\\d{1,2}:\\d{2}").containsMatchIn(it) }
        val pageHeader = lines.take(16).joinToString(" ").let(financialPageSignal::containsMatchIn)
        return hasCurrency && hasDateOrTime && hasMerchant && pageHeader
    }

    private fun extractAmount(line: String): AmountMatch? {
        val withoutDates = fullDate.replace(line, " ")
        val withoutMonthDay = monthDayText.replace(withoutDates, " ")
        // ML Kit sometimes reads the adjacent won/icon glyph as `2l` after a grouped amount.
        val normalizedLine = Regex("([-+＋−]?\\s*\\d{1,3}(?:,\\d{3})+)[2lI|!]{1,2}$")
            .replace(withoutMonthDay) { it.groupValues[1] }
        val matches = money.findAll(normalizedLine).toList()
        // Calendar totals and charts often put several signed daily values on one OCR row.
        // None of those values has a single merchant, so never treat them as transactions.
        if (matches.size != 1) return null
        val match = matches.single()
        val raw = match.groupValues[1].replace(" ", "").replace("＋", "+").replace("−", "-")
        val amount = raw.removePrefix("+").removePrefix("-").replace(",", "").toLongOrNull()
            ?.takeIf { it in 1..1_000_000_000_000L } ?: return null
        return AmountMatch(amount, raw)
    }

    private fun dateIn(text: String, today: LocalDate, visibleMonth: Int): LocalDate? {
        fullDate.find(text)?.let { match ->
            return runCatching {
                LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
            }.getOrNull()
        }
        val monthDay = monthDayText.find(text) ?: monthDayPunctuation.find(text)
        val dayMatch = dayHeading.find(text)
        if (monthDay == null && dayMatch == null) return null
        val month = monthDay?.groupValues?.getOrNull(1)?.toIntOrNull() ?: visibleMonth
        val day = monthDay?.groupValues?.getOrNull(2)?.toIntOrNull()
            ?: dayMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return null
        val inferredYear = if (month > today.monthValue + 1 && today.monthValue <= 2) today.year - 1 else today.year
        val date = runCatching { LocalDate.of(inferredYear, month, day) }.getOrNull() ?: return null
        return if (date.isAfter(today.plusDays(7))) runCatching { date.minusYears(1) }.getOrNull() else date
    }

    private fun inferType(amountText: String, context: String, transferDirection: TransactionType?): TransactionType {
        val lower = context.lowercase()
        val sign = amountText.trim().firstOrNull()
        if (sign == '+' || sign == '＋') return TransactionType.INCOME
        if (sign == '-' || sign == '−') return TransactionType.EXPENSE
        if (transferDirection != null) return transferDirection
        if (lower.contains("캐시백") && hasOwnAccount(context)) return TransactionType.INCOME
        val incomeSignal = listOf("입금", "받았", "수입", "환급", "급여", "월급", "예금이자", "이자입금", "당첨", "복권").any(lower::contains)
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

        val candidates = mutableListOf<Triple<Int, Int, String>>()
        // Banking apps commonly place the counterparty/merchant immediately below the amount.
        for (distance in 1..4) {
            val after = index + distance
            if (after < nextAmount && after <= allLines.lastIndex && !hasDateBoundary(allLines, index + 1, after)) {
                usableMerchant(allLines[after])?.let { merchant ->
                    val detailEnd = minOf(nextAmount - 1, after + 2, allLines.lastIndex)
                    val details = (after..detailEnd).joinToString(" ") { allLines[it] }
                    val priority = if (hasPaymentRowEvidence(details)) 0 else 2
                    candidates += Triple(priority, distance, merchant)
                }
            }
            val before = index - distance
            if (before > previousAmount && before >= 0 && !hasDateBoundary(allLines, before, index - 1)) {
                usableMerchant(allLines[before])?.let { merchant -> candidates += Triple(1, distance, merchant) }
            }
        }
        return candidates.minWithOrNull(compareBy<Triple<Int, Int, String>> { it.first }.thenBy { it.second })?.third ?: "알 수 없음"
    }

    private fun hasDateBoundary(lines: List<String>, start: Int, end: Int): Boolean =
        (start..end).any { index ->
            val line = lines.getOrNull(index)?.trim().orEmpty()
            dayHeading.containsMatchIn(line) || monthDayText.containsMatchIn(line) || monthDayPunctuation.containsMatchIn(line)
        }

    private fun hasPaymentRowEvidence(text: String): Boolean =
        Regex("토스\\s*뱅크|계좌|통장|체크카드|신용카드|카드|→|➜|->|받았|보냈|입금|출금|송금").containsMatchIn(text)

    private fun usableMerchant(raw: String): String? {
        val hasAccountContext = hasOwnAccount(raw)
        if (raw.contains("캐시백") && !hasAccountContext) return null
        val paymentPrefix = Regex(
            "^(?:(?:토스|카카오페이|네이버페이|삼성페이|신한카드|현대카드|KB\\s*국민카드|국민카드|우리카드|하나카드|롯데카드|삼성카드|NH농협카드|카드)\\s+)+",
            RegexOption.IGNORE_CASE,
        )
        val line = raw.trim()
            .replace(paymentPrefix, "")
            .replace(fullDate, " ")
            .replace(monthDayText, " ")
            .replace(monthDayPunctuation, " ")
            .replace(dayHeading, " ")
            .replace(Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b"), " ")
            .replace(Regex("[-+＋−]?\\s*[0-9][0-9,]*\\s*(?:원|KRW)?", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\b\\d{4,}\\b"), " ")
            .replace(Regex("내\\s*(?:토스\\s*)?(?:뱅크\\s*)?(?:계좌|통장)"), " ")
            .replace(Regex("토스\\s*뱅크\\s*(?:체크카드|신용카드|카드|계좌|통장)?"), " ")
            .replace(Regex("(?:체크카드|신용카드)"), " ")
            .replace(Regex("(?:계좌\\s*이체|송금|입금|출금|받기|보내기)"), " ")
            .replace(Regex("^\\s*토스(?=[가-힣]{2,})"), " ")
            .replace(Regex("[·|•:：()\\[\\]{}%!#*]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '−', '+', '원')
        if (Regex("d\\.?\\s*tryx|디트릭스", RegexOption.IGNORE_CASE).containsMatchIn(line)) return "디트릭스"
        canonicalMerchant(line)?.let { return it }
        if (line.length !in 2..60 || line.none(Char::isLetter)) return null
        if (nonMerchantLine.containsMatchIn(line) || excludedLine.containsMatchIn(line)) return null
        if (line.contains("원") && line.length <= 4) return null
        return line
    }

    private fun canonicalMerchant(value: String): String? {
        val compact = value.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
        return when {
            Regex("(?:바|배)?배(?:달|탈)(?:의)?(?:민|만)족|바달(?:의)?(?:민|만)족").containsMatchIn(compact) -> "배달의민족"
            compact.contains("네이버페이") -> "네이버페이"
            else -> null
        }
    }

    private fun isOwnAccountTransfer(context: String): Boolean =
        Regex("내계좌이체|내계좌간이체|계좌간이체").containsMatchIn(context.replace(Regex("\\s+"), ""))

    private fun hasOwnAccount(context: String): Boolean =
        Regex("내\\s*(?:토스\\s*)?(?:뱅크\\s*)?(?:계좌|통장)").containsMatchIn(context)

    /** Infer only clear person-to-own-account directions; ambiguous transfers remain unconfirmed. */
    private fun inferTransferDirection(context: String): TransactionType? {
        // Rewards/interest are credits, but they are not person-to-person transfers.
        if (Regex("복권|당첨|캐시백|포인트|이벤트").containsMatchIn(context)) return null
        val ownAccount = Regex("내\\s*(?:토스\\s*)?(?:뱅크\\s*)?(?:계좌|통장)")
        val own = ownAccount.find(context) ?: return null
        val before = context.substring(0, own.range.first)
        val after = context.substring(own.range.last + 1)
        if (Regex("(?:→|➜|->|에서|부터)").containsMatchIn(after)) return TransactionType.EXPENSE
        if (Regex("(?:→|➜|->|에게|한테)").containsMatchIn(before)) return TransactionType.INCOME
        val amountNoise = Regex("[-+＋−]?\\s*\\d[\\d,]*(?:[2lI|!]+)?")
        val nonDateCaption = Regex("평소보다\\s*많이\\s*(?:씀|쓸|쓴)")
        val beforeWords = amountNoise.replace(nonDateCaption.replace(dayHeading.replace(before, " "), " "), " ")
            .replace(Regex("[^\\p{L}]"), "")
        val afterWords = amountNoise.replace(nonDateCaption.replace(dayHeading.replace(after, " "), " "), " ")
            .replace(Regex("[^\\p{L}]"), "")
        return when {
            beforeWords.isNotBlank() && afterWords.isBlank() -> TransactionType.INCOME
            beforeWords.isBlank() && afterWords.isNotBlank() -> TransactionType.EXPENSE
            else -> null
        }
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
