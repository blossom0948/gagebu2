package com.moasseum.app.data

import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import java.time.LocalDate
import java.time.YearMonth

enum class BatchCommandAction { ADD, UPDATE, DELETE }

data class BatchTarget(
    val type: TransactionType? = null,
    val amount: Long? = null,
    val categoryKey: String? = null,
    val merchantTokens: List<String> = emptyList(),
    val from: LocalDate? = null,
    val through: LocalDate? = null,
) {
    fun matches(transaction: Transaction): Boolean =
        (type == null || transaction.type == type) &&
            (amount == null || transaction.amount == amount) &&
            (categoryKey == null || transaction.categoryKey == categoryKey) &&
            merchantTokens.all { token -> transaction.merchant.contains(token, ignoreCase = true) } &&
            (from == null || !transaction.occurredDate.isBefore(from)) &&
            (through == null || !transaction.occurredDate.isAfter(through))
}

data class BatchEditValues(
    val amount: Long? = null,
    val merchant: String? = null,
    val categoryKey: String? = null,
    val type: TransactionType? = null,
    val occurredDate: LocalDate? = null,
    val memo: String? = null,
)

sealed interface BatchCommandPlan {
    data class Add(val candidates: List<AiTransactionCandidate>) : BatchCommandPlan
    data class Change(
        val action: BatchCommandAction,
        val target: BatchTarget,
        val matches: List<Transaction>,
        val edit: BatchEditValues? = null,
    ) : BatchCommandPlan
}

/**
 * Safe, on-device batch command preview. Destructive commands are only plans;
 * callers must ask the user to select the exact rows before applying them.
 */
object BatchCommandParser {
    private const val MAX_ITEMS = 30
    private const val MAX_MATCHES = 100
    private val categoryTerms = mapOf(
        "FOOD" to listOf("식비", "음식", "식당", "점심", "저녁", "아침", "치킨", "배달", "밥"),
        "CAFE" to listOf("카페", "커피"),
        "TRANSPORT" to listOf("교통", "버스", "지하철", "택시", "주유"),
        "SHOPPING" to listOf("쇼핑", "쿠팡", "무신사"),
        "HOUSING" to listOf("주거", "월세", "전세", "관리비"),
        "COMMUNICATION" to listOf("통신", "휴대폰", "핸드폰", "인터넷요금"),
        "HEALTH" to listOf("의료", "병원", "약국"),
        "EDUCATION" to listOf("교육", "학원", "교재"),
        "CULTURE" to listOf("문화", "전시", "공연", "박물관"),
        "LEISURE" to listOf("여가", "영화", "게임"),
        "TRAVEL" to listOf("여행", "항공권", "숙박", "호텔"),
        "GIFT" to listOf("경조사", "축의금", "조의금", "선물"),
        "BEAUTY" to listOf("미용", "미용실", "네일", "화장품"),
        "PET" to listOf("반려동물", "반려견", "반려묘", "동물병원"),
        "SUBSCRIPTION" to listOf("구독", "넷플릭스", "유튜브프리미엄", "멤버십"),
        "INSURANCE" to listOf("보험", "보험료"),
        "FINANCE" to listOf("금융", "이자", "수수료"),
        "LIVING" to listOf("생활", "생필품", "전기", "가스"),
        "OTHER" to listOf("기타"),
    )
    private val removableWords = setOf(
        "추가", "기록", "입력", "수정", "변경", "바꿔", "바꿔줘", "고쳐", "삭제", "지워", "지워줘",
        "취소", "해주세요", "해줘", "해", "내역", "거래", "항목", "지출", "수입", "입금", "결제",
        "금액", "카테고리", "가맹점", "이름", "전체", "전부", "모두", "이번", "지난", "달", "월",
        "오늘", "어제", "그제", "그저께", "내일", "을", "를", "이", "가", "은", "는", "으로", "로",
        "원", "에서", "으로", "그리고", "또",
    )

    fun parse(
        raw: String,
        transactions: List<Transaction>,
        today: LocalDate = LocalDate.now(),
    ): BatchCommandPlan {
        val input = raw.trim()
        require(input.isNotEmpty()) { "문장을 입력해 주세요." }
        require(input.length <= 500) { "일괄 문장은 500자 이내로 입력해 주세요." }
        return when {
            input.startsWith("삭제") || input.startsWith("삭제해줘") || input.startsWith("지워") ->
                makeChange(BatchCommandAction.DELETE, input.substringAfter(':', input.dropWhile { !it.isWhitespace() }).trim(), null, transactions, today)
            input.startsWith("수정") || input.startsWith("변경") -> {
                val body = input.substringAfter(':', input.dropWhile { !it.isWhitespace() }).trim()
                val arrow = listOf("->", "→", "=>").firstOrNull(body::contains)
                    ?: throw IllegalArgumentException("수정은 ‘수정: 찾을 조건 -> 바꿀 내용’으로 입력해 주세요.")
                val splitAt = body.indexOf(arrow)
                val targetText = body.substring(0, splitAt).trim()
                val patchText = body.substring(splitAt + arrow.length).trim()
                val edit = parseEdit(patchText)
                makeChange(BatchCommandAction.UPDATE, targetText, edit, transactions, today)
            }
            else -> parseAdd(input, today)
        }
    }

    private fun parseAdd(input: String, today: LocalDate): BatchCommandPlan.Add {
        val body = input.removePrefix("추가:").removePrefix("기록:").trim()
        val pieces = body.split(Regex("\\r?\\n|\\s+그리고\\s+|\\s+또\\s+|,\\s*(?=[^0-9])"))
            .map(String::trim).filter(String::isNotBlank)
        require(pieces.size in 1..MAX_ITEMS) { "한 번에 1~${MAX_ITEMS}건까지 입력할 수 있어요." }
        val candidates = pieces.map { phrase ->
            runCatching {
                val parsed = LocalNaturalLanguageParser.parse(phrase, today)
                if (parsed.merchant == "알 수 없음") {
                    val fallback = listOf("점심", "저녁", "아침", "간식").firstOrNull(phrase::contains) ?: "기타 지출"
                    parsed.copy(merchant = fallback, needsConfirmation = parsed.needsConfirmation + "merchant")
                } else parsed
            }
                .getOrElse { throw IllegalArgumentException("‘${phrase.take(36)}’에서 거래를 읽지 못했어요. 금액과 내용을 확인해 주세요.") }
        }
        return BatchCommandPlan.Add(candidates)
    }

    private fun makeChange(
        action: BatchCommandAction,
        targetText: String,
        edit: BatchEditValues?,
        transactions: List<Transaction>,
        today: LocalDate,
    ): BatchCommandPlan.Change {
        val target = parseTarget(targetText, today)
        return previewChange(action, target, edit, transactions)
    }

    /** Applies a validated query plan to local rows only; no transaction rows leave the device. */
    fun previewChange(
        action: BatchCommandAction,
        target: BatchTarget,
        edit: BatchEditValues?,
        transactions: List<Transaction>,
    ): BatchCommandPlan.Change {
        require(action == BatchCommandAction.UPDATE || action == BatchCommandAction.DELETE) { "수정 또는 삭제만 미리 볼 수 있어요." }
        require(target.amount == null || target.amount in 1..1_000_000_000_000L) { "검색 금액을 확인해 주세요." }
        require(target.categoryKey == null || target.categoryKey in categoryTerms.keys) { "검색 카테고리를 확인해 주세요." }
        require(target.merchantTokens.size <= 4 && target.merchantTokens.all { it.trim().length in 2..60 }) {
            "검색 가맹점 조건을 확인해 주세요."
        }
        require(target.from == null || target.through == null || !target.from.isAfter(target.through)) {
            "검색 날짜 범위를 확인해 주세요."
        }
        require(target.amount != null || target.categoryKey != null || target.merchantTokens.isNotEmpty() ||
            target.from != null || target.through != null) {
            "대상을 좁혀 주세요. 예: 오늘 카페 지출 또는 특정 가맹점"
        }
        if (action == BatchCommandAction.UPDATE) {
            require(edit != null) { "바꿀 금액·가맹점·카테고리를 적어 주세요." }
            require(edit.amount == null || edit.amount in 1..1_000_000_000_000L) { "변경할 금액을 확인해 주세요." }
            require(edit.merchant == null || edit.merchant.trim().length in 1..120) { "변경할 가맹점을 확인해 주세요." }
            require(edit.categoryKey == null || edit.categoryKey in categoryTerms.keys) { "변경할 카테고리를 확인해 주세요." }
            require(edit.type == null || edit.type == TransactionType.EXPENSE || edit.type == TransactionType.INCOME) {
                "수입 또는 지출 유형만 변경할 수 있어요."
            }
            require(edit.memo == null || edit.memo.length <= 300) { "메모는 300자 이내로 입력해 주세요." }
            require(
                edit.amount != null || edit.merchant != null || edit.categoryKey != null ||
                    edit.type != null || edit.occurredDate != null || edit.memo != null,
            ) { "변경할 내용을 확인해 주세요." }
        } else {
            require(edit == null) { "삭제 요청에 변경 내용이 포함되어 있어요." }
        }
        val matches = transactions.asSequence()
            .filter { it.ledgerId == "personal" && it.sharingScope != "SHARED" && it.type != TransactionType.TRANSFER }
            .filter { it.installmentGroupId == null }
            .filter(target::matches)
            .sortedByDescending { it.occurredAt }
            .take(MAX_MATCHES + 1)
            .toList()
        require(matches.isNotEmpty()) { "조건에 맞는 개인 거래가 없어요." }
        require(matches.size <= MAX_MATCHES) { "조건에 맞는 거래가 너무 많아요. 날짜나 가맹점으로 더 좁혀 주세요." }
        return BatchCommandPlan.Change(action, target, matches, edit)
    }

    private fun parseTarget(text: String, today: LocalDate): BatchTarget {
        val normalized = text.lowercase()
        val type = when {
            listOf("수입", "입금", "월급").any(normalized::contains) -> TransactionType.INCOME
            listOf("지출", "결제").any(normalized::contains) -> TransactionType.EXPENSE
            else -> null
        }
        val date = when {
            "그저께" in normalized || "그제" in normalized -> today.minusDays(2) to today.minusDays(2)
            "어제" in normalized -> today.minusDays(1) to today.minusDays(1)
            "오늘" in normalized -> today to today
            "내일" in normalized -> today.plusDays(1) to today.plusDays(1)
            "이번 달" in normalized || "이번달" in normalized -> YearMonth.from(today).atDay(1) to YearMonth.from(today).atEndOfMonth()
            "지난달" in normalized || "저번달" in normalized -> YearMonth.from(today).minusMonths(1).atDay(1) to YearMonth.from(today).minusMonths(1).atEndOfMonth()
            else -> Regex("\\b(20\\d{2})-(\\d{1,2})-(\\d{1,2})\\b").find(text)?.let { match ->
                runCatching { LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt()) }
                    .getOrNull()?.let { it to it }
            } ?: Regex("(\\d{1,2})월\\s*(\\d{1,2})일").find(text)?.let { match ->
                runCatching { LocalDate.of(today.year, match.groupValues[1].toInt(), match.groupValues[2].toInt()) }
                    .getOrNull()?.let { it to it }
            }
        }
        val category = categoryTerms.entries.firstOrNull { (_, terms) -> terms.any(normalized::contains) }?.key
        val amountText = text.replace(Regex("20\\d{2}-\\d{1,2}-\\d{1,2}|\\d{1,2}월\\s*\\d{1,2}일"), " ")
        val amount = LocalNaturalLanguageParser.findAmount(amountText)
        val withoutAmounts = text.replace(Regex("\\d{1,3}(?:,\\d{3})+\\s*원?|\\d{3,}\\s*원?|\\d+(?:\\.\\d+)?\\s*(?:만|천)\\s*원?"), " ")
            .replace(Regex("20\\d{2}-\\d{1,2}-\\d{1,2}|\\d{1,2}월\\s*\\d{1,2}일"), " ")
        val categoryTokens = categoryTerms.values.flatten().toSet()
        val merchantTokens = withoutAmounts
            .replace(Regex("오늘|어제|그저께|그제|내일|이번 달|이번달|지난달|저번달|\\d{1,2}월|월말"), " ")
            .replace(Regex("삭제|지워줘|지워|삭제해줘|수정|변경|바꿔줘|바꿔|금액|카테고리|가맹점|이름|지출|수입|입금|결제|내역|거래|항목|전체|전부|모두|해주세요|해줘|해|으로|에서|그리고|또|원"), " ")
            .split(Regex("[\\s,.:;!?/→]+"))
            .map { it.trim().trimEnd('을', '를', '은', '는', '이', '가', '로', '과', '와') }
            .filter { token -> token.length >= 2 && token !in removableWords && token !in categoryTokens && token.none(Char::isDigit) }
            .distinct()
            .take(4)
        return BatchTarget(type, amount, category, merchantTokens, date?.first, date?.second)
    }

    private fun parseEdit(text: String): BatchEditValues {
        val amount = LocalNaturalLanguageParser.findAmount(text)
        val merchant = Regex("(?:가맹점|이름)\\s*[=:]?\\s*([^,;]+)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)?.take(120)
        val categoryValue = Regex("(?:카테고리|분류)\\s*[=:]?\\s*([^,;]+)", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.trim()?.lowercase()
        val category = when {
            categoryValue != null -> categoryTerms.entries.firstOrNull { (key, terms) ->
                key.equals(categoryValue, ignoreCase = true) || terms.any { it == categoryValue }
            }?.key
            merchant == null -> categoryTerms.entries.firstOrNull { (_, terms) -> terms.any(text.lowercase()::contains) }?.key
            else -> null
        }
        val type = Regex("유형\\s*[=:]\\s*(수입|지출)")
            .find(text)?.groupValues?.getOrNull(1)?.let { if (it == "수입") TransactionType.INCOME else TransactionType.EXPENSE }
        val occurredDate = Regex("날짜\\s*[=:]\\s*(20\\d{2}-\\d{1,2}-\\d{1,2})")
            .find(text)?.groupValues?.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val memo = Regex("메모\\s*[=:]\\s*([^,;]+)")
            .find(text)?.groupValues?.getOrNull(1)?.trim()?.takeIf(String::isNotBlank)?.take(300)
        val values = BatchEditValues(amount, merchant, category, type, occurredDate, memo)
        require(values.amount != null || values.merchant != null || values.categoryKey != null ||
            values.type != null || values.occurredDate != null || values.memo != null) {
            "금액·가맹점·카테고리·유형·날짜·메모 중 바꿀 값을 적어 주세요."
        }
        return values
    }
}
