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
    private val amountActionTerms = listOf("승인", "결제", "출금", "입금", "송금", "환급", "급여", "월급", "이체", "사용내역", "이용내역", "받았", "충전")
    private val hardExcludedContexts = listOf(
        "승인번호", "인증번호", "결제번호", "예약번호", "주문번호", "결제예정", "납부예정", "출금예정",
    )
    private val softExcludedContexts = listOf("쿠폰", "할인", "혜택", "적립", "포인트", "이벤트", "특가")
    private val cancelledTerms = listOf("취소", "cancel", "거절", "실패", "reversed")
    private val strongTransactionTerms = listOf(
        "승인", "결제완료", "결제 완료", "출금", "입금", "환급", "급여", "월급", "송금완료", "이체완료",
        "payment", "purchase", "withdrawal", "deposit", "salary", "refund",
    )

    fun parse(
        packageName: String,
        title: String,
        body: String,
        postedAt: Long,
        aiConfirmedType: TransactionType? = null,
    ): NotificationCandidateEntity? {
        val normalized = "$title $body".replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return null

        if (cancelledTerms.any { normalized.contains(it, ignoreCase = true) }) return null
        val expenseSignal = listOf(
            "승인", "결제", "출금", "사용", "이용", "매입", "구매", "payment", "purchase", "withdrawal",
            "보냈", "송금완료", "이체완료", "출금완료",
        )
            .any { normalized.contains(it, ignoreCase = true) }
        val incomeSignal = listOf(
            "입금", "급여", "월급", "환급", "받았", "받음", "매출", "deposit", "salary", "refund", "송금받", "이체받",
            "입금완료",
        )
            .any { normalized.contains(it, ignoreCase = true) }
        if (aiConfirmedType == null && (!expenseSignal && !incomeSignal)) return null
        if (aiConfirmedType == null && hardExcludedContexts.any { normalized.contains(it, ignoreCase = true) }) return null
        val hasStrongTransactionSignal = strongTransactionTerms.any { normalized.contains(it, ignoreCase = true) }
        if (aiConfirmedType == null && softExcludedContexts.any { normalized.contains(it, ignoreCase = true) } && !hasStrongTransactionSignal) return null

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
            categoryKey = inferCategory(normalized),
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
        val hasMoneyContext = amountActionTerms.any { normalized.contains(it, ignoreCase = true) }
        val hasTransactionSignal = strongTransactionTerms.any { normalized.contains(it, ignoreCase = true) }
        val hasActionAmount = actionAmount.containsMatchIn(normalized) || explicitActionAmount.containsMatchIn(normalized)
        return (hasCurrency || hasActionAmount) && (hasTransactionSignal || hasMoneyContext)
    }

    fun hasStrongTransactionSignal(title: String, body: String): Boolean {
        val normalized = "$title $body".replace(Regex("\\s+"), " ").trim()
        return strongTransactionTerms.any { normalized.contains(it, ignoreCase = true) }
    }

    private fun findMarkedAmount(text: String): Long? {
        markedKoreanAmount.find(text)?.let { amountValue(it.groupValues[1], it.groupValues[2])?.let { value -> return value } }
        wonAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        symbolAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        explicitActionAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()?.let { return it }
        return actionAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()
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

    private fun inferCategory(text: String): String {
        val lower = text.lowercase(Locale.KOREAN)
        return when {
            listOf("카페", "커피", "스타벅스", "식당", "치킨", "배달", "점심", "저녁", "마트", "편의점", "food", "restaurant").any { lower.contains(it) } -> "FOOD"
            listOf("택시", "버스", "지하철", "주유", "교통", "uber", "taxi", "transport").any { lower.contains(it) } -> "TRANSPORT"
            listOf("쇼핑", "온라인", "쿠팡", "무신사", "shopping", "store").any { lower.contains(it) } -> "SHOPPING"
            listOf("병원", "약국", "건강", "hospital", "pharmacy").any { lower.contains(it) } -> "HEALTH"
            listOf("넷플릭스", "유튜브", "게임", "영화", "leisure", "subscription").any { lower.contains(it) } -> "LEISURE"
            else -> "OTHER"
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}
