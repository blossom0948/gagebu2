package com.moasseum.app.notification

import com.moasseum.app.data.local.NotificationCandidateEntity
import com.moasseum.app.domain.TransactionType
import java.security.MessageDigest
import java.util.Locale

object PaymentNotificationParser {
    private val numericAmount = Regex("""(\d{1,3}(?:,\d{3})+|\d{3,})""")
    private val wonAmount = Regex("""(?:[₩￦]\s*)?(\d{1,3}(?:,\d{3})+|\d{3,})\s*(?:원|KRW)""", RegexOption.IGNORE_CASE)
    private val symbolAmount = Regex("""[₩￦]\s*(\d{1,3}(?:,\d{3})+|\d{3,})""")
    private val koreanAmount = Regex("""(\d+(?:\.\d+)?)\s*(만|천)\s*원?""")
    private val markedKoreanAmount = Regex("""(\d+(?:\.\d+)?)\s*(만|천)\s*원""")
    private val actionAmount = Regex("""(?:승인|결제|출금|입금|환급|송금|이체|사용)\s*(?:금액\s*)?[:：]?\s*(\d{1,3}(?:,\d{3})+)\s*원?(?![-/.]\d)""")
    private val explicitActionAmount = Regex("""(?:승인|결제|출금|입금|환급|송금|이체|사용)\s*금액\s*[:：]?\s*(\d{3,})(?![\d,])\s*원?(?![-/.]\d)""")
    private val amountBeforeAction = Regex("""(?:[₩￦]\s*)?(\d{1,3}(?:,\d{3})+|\d{3,})\s*(?:원|KRW)?\s*(?:승인|결제완료|결제 완료|출금|입금|환급|송금완료|이체완료|입금완료|입금되었습니다|입금됐)(?!번호)""", RegexOption.IGNORE_CASE)
    private val hardExcludedContexts = listOf(
        "승인번호", "인증번호", "결제번호", "예약번호", "주문번호", "결제예정", "납부예정", "출금예정",
    )
    private val promotionalContexts = listOf("쿠폰", "할인", "적립", "포인트", "이벤트", "특가")
    private val cancelledTerms = listOf("취소", "cancel", "거절", "실패", "reversed")
    private val expenseTerms = listOf(
        "승인", "결제완료", "결제 완료", "결제가 완료", "결제되었습니다", "결제됐", "결제", "출금",
        "사용내역", "사용 내역", "이용내역", "이용 내역", "매입", "구매완료", "구매 완료",
        "payment", "purchase", "withdrawal", "보냈", "송금완료", "이체완료", "출금완료",
    )
    private val incomeTerms = listOf(
        "입금", "급여", "월급", "환급", "받았", "받음", "보낸 분", "보낸분", "보낸사람", "송금인",
        "매출", "deposit", "salary", "refund", "송금받", "이체받", "입금완료", "입금되었습니다", "입금됐",
    )
    private val strongTransactionTerms = listOf(
        "승인", "결제완료", "결제 완료", "결제가 완료", "결제되었습니다", "결제됐", "출금", "입금", "환급",
        "급여", "월급", "송금완료", "이체완료", "입금완료", "입금되었습니다", "입금됐", "출금완료",
        "구매완료", "구매 완료", "보낸 분", "보낸분", "보낸사람", "송금인", "payment", "purchase",
        "withdrawal", "deposit", "salary", "refund",
    )

    fun parse(
        packageName: String,
        title: String,
        body: String,
        postedAt: Long,
        aiConfirmedType: TransactionType? = null,
    ): NotificationCandidateEntity? {
        if (NotificationSourcePolicy.isExcluded(packageName)) return null
        val normalized = "$title $body".replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return null

        if (cancelledTerms.any { normalized.contains(it, ignoreCase = true) }) return null
        val expenseSignal = expenseTerms.any { normalized.contains(it, ignoreCase = true) }
        val incomeSignal = incomeTerms.any { normalized.contains(it, ignoreCase = true) }
        // AI can refine direction, but a number or chat message alone is never transaction evidence.
        if (!expenseSignal && !incomeSignal) return null
        if (hardExcludedContexts.any { normalized.contains(it, ignoreCase = true) }) return null
        val hasStrongTransactionSignal = strongTransactionTerms.any { normalized.contains(it, ignoreCase = true) }
        if (!hasStrongTransactionSignal) return null
        if (promotionalContexts.any { normalized.contains(it, ignoreCase = true) } &&
            amountBeforeAction.find(normalized) == null && actionAmount.find(normalized) == null &&
            explicitActionAmount.find(normalized) == null
        ) return null

        val amount = findMarkedAmount(normalized)
        amount ?: return null
        if (amount <= 0L) return null

        val merchant = findMerchant(title, body)
        val type = aiConfirmedType?.name ?: if (incomeSignal && !expenseSignal) "INCOME" else "EXPENSE"
        val fingerprint = sha256("$packageName|$title|$body|${postedAt / 60_000L}")

        return NotificationCandidateEntity(
            packageName = packageName,
            title = title.ifBlank { "결제 알림" }.take(80),
            preview = normalized.take(180),
            merchant = merchant.take(80),
            amount = amount,
            type = type,
            categoryKey = inferCategoryFrom(normalized),
            postedAt = postedAt,
            fingerprint = fingerprint,
        )
    }

    fun shouldInspectWithAi(title: String, body: String): Boolean {
        val normalized = "$title $body".replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank() ||
            cancelledTerms.any { normalized.contains(it, ignoreCase = true) } ||
            hardExcludedContexts.any { normalized.contains(it, ignoreCase = true) }
        ) return false
        val hasAmount = findAmount(normalized) != null
        if (!hasAmount) return false
        val hasCurrency = wonAmount.containsMatchIn(normalized) || symbolAmount.containsMatchIn(normalized) || koreanAmount.containsMatchIn(normalized)
        val hasTransactionSignal = strongTransactionTerms.any { normalized.contains(it, ignoreCase = true) }
        val hasActionAmount = actionAmount.containsMatchIn(normalized) || explicitActionAmount.containsMatchIn(normalized)
        return (hasCurrency || hasActionAmount) && hasTransactionSignal
    }

    fun hasStrongTransactionSignal(title: String, body: String): Boolean {
        val normalized = "$title $body".replace(Regex("\\s+"), " ").trim()
        return strongTransactionTerms.any { normalized.contains(it, ignoreCase = true) }
    }

    private fun findMarkedAmount(text: String): Long? {
        amountBeforeAction.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        explicitActionAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        actionAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        markedKoreanAmount.find(text)?.let { amountValue(it.groupValues[1], it.groupValues[2])?.let { value -> return value } }
        wonAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        symbolAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        return null
    }

    private fun findAmount(text: String): Long? {
        markedKoreanAmount.find(text)?.let { amountValue(it.groupValues[1], it.groupValues[2])?.let { value -> return value } }
        wonAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        symbolAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        koreanAmount.find(text)?.let { amountValue(it.groupValues[1], it.groupValues[2])?.let { value -> return value } }
        return numericAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()
    }

    private fun amountValue(baseValue: String, unit: String): Long? {
        val base = baseValue.toDoubleOrNull() ?: return null
        return when (unit) {
            "만" -> (base * 10_000).toLong()
            "천" -> (base * 1_000).toLong()
            else -> null
        }
    }

    private fun findMerchant(title: String, body: String): String {
        val source = body.ifBlank { title }
        val cleaned = source
            .replace(numericAmount, " ")
            .replace(koreanAmount, " ")
            .replace(Regex("[|•·:/\\n]"), " ")
        val noise = setOf(
            "승인", "결제", "출금", "사용", "이용", "입금", "완료", "원", "카드", "신용", "체크",
            "잔액", "누적", "일시불", "할부", "취소", "payment", "purchase", "approved", "입금완료",
            "안내", "알림", "혜택", "쿠폰", "할인", "포인트", "예정", "예정일", "광고",
        )
        return cleaned
            .split(Regex("\\s+|,"))
            .map { it.trim() }
            .filter { token ->
                token.length >= 2 &&
                    noise.none { word -> token.equals(word, ignoreCase = true) } &&
                    !token.matches(Regex("[0-9*＊•·xX○-]+")) &&
                    !token.endsWith("님") &&
                    !token.matches(Regex("[가-힣][*＊•·][가-힣]*님?"))
            }
            .firstOrNull()
            ?: title.trim().takeIf { it.length >= 2 }?.let { it.replace(Regex("카드|결제"), "").trim() }
            ?: "알림 거래"
    }

    fun inferCategoryFrom(text: String): String {
        val lower = text.lowercase(Locale.KOREAN)
        return when {
            listOf("카페", "커피", "스타벅스", "아메리카노", "컴포즈커피", "메가커피", "빽다방", "이디야", "투썸", "더벤티", "cafe", "coffee").any { lower.contains(it) } -> "CAFE"
            listOf(
                "식당", "치킨", "버거킹", "burger king", "푸드", "배달", "점심", "저녁", "마트", "이마트", "홈플러스", "롯데마트", "코스트코",
                "트레이더스", "하나로마트", "노브랜드", "gs더프레시", "이마트에브리데이", "이마트24", "마켓컬리", "오아시스마켓",
                "배달의민족", "쿠팡이츠", "요기요", "편의점", "cu", "gs25", "세븐일레븐", "food", "restaurant", "supermarket", "grocery",
            ).any { lower.contains(it) } -> "FOOD"
            listOf("택시", "버스", "지하철", "주유", "교통", "카카오t", "티머니", "고속도로", "ktx", "srt", "uber", "taxi", "transport").any { lower.contains(it) } -> "TRANSPORT"
            listOf("쇼핑", "온라인", "쿠팡", "무신사", "다이소", "지마켓", "11번가", "옥션", "네이버쇼핑", "shopping", "store").any { lower.contains(it) } -> "SHOPPING"
            listOf("월세", "전세", "관리비", "주거", "housing").any { lower.contains(it) } -> "HOUSING"
            listOf("통신비", "휴대폰 요금", "인터넷 요금", "통신", "communication").any { lower.contains(it) } -> "COMMUNICATION"
            listOf("병원", "약국", "건강", "의료", "hospital", "pharmacy").any { lower.contains(it) } -> "HEALTH"
            listOf("학원", "교재", "수강료", "교육", "education").any { lower.contains(it) } -> "EDUCATION"
            listOf("전시", "공연", "박물관", "문화", "culture").any { lower.contains(it) } -> "CULTURE"
            listOf("여행", "항공권", "숙박", "호텔", "travel").any { lower.contains(it) } -> "TRAVEL"
            listOf("경조사", "축의금", "조의금", "선물", "gift").any { lower.contains(it) } -> "GIFT"
            listOf("미용실", "헤어", "네일", "화장품", "올리브영", "beauty").any { lower.contains(it) } -> "BEAUTY"
            listOf("반려견", "반려묘", "동물병원", "펫", "pet").any { lower.contains(it) } -> "PET"
            listOf("구독", "멤버십", "정기결제", "넷플릭스", "유튜브 프리미엄", "디즈니+", "스포티파이", "멜론", "웨이브", "티빙", "왓챠", "쿠팡 와우", "subscription").any { lower.contains(it) } -> "SUBSCRIPTION"
            listOf("보험료", "보험", "insurance").any { lower.contains(it) } -> "INSURANCE"
            listOf("이자", "수수료", "금융", "캐시백", "cashback", "finance").any { lower.contains(it) } -> "FINANCE"
            listOf("게임", "영화", "극장", "디트릭스", "dtryx", "놀이공원", "공연", "leisure").any { lower.contains(it) } -> "LEISURE"
            listOf("전기", "한국전력", "한전", "도시가스", "수도요금", "생필품", "생활용품", "생활용품점", "living").any { lower.contains(it) } -> "LIVING"
            else -> "OTHER"
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}
