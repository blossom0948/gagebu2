package com.moasseum.app.notification

import com.moasseum.app.data.local.NotificationCandidateEntity
import java.security.MessageDigest
import java.util.Locale

object PaymentNotificationParser {
    private val numericAmount = Regex("""(?:₩\s*)?(\d{1,3}(?:,\d{3})+|\d{3,})\s*원?""")
    private val KoreanAmount = Regex("""(?:₩\s*)?(\d+(?:\.\d+)?)\s*(만|천)\s*원?""")

    fun parse(
        packageName: String,
        title: String,
        body: String,
        postedAt: Long,
    ): NotificationCandidateEntity? {
        val normalized = "$title $body".replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return null

        val expenseSignal = listOf("승인", "결제", "출금", "사용", "이용", "payment", "purchase", "withdrawal")
            .any { normalized.contains(it, ignoreCase = true) }
        val incomeSignal = listOf("입금", "급여", "월급", "환급", "받았", "deposit", "salary", "refund")
            .any { normalized.contains(it, ignoreCase = true) }
        val cancelled = listOf("취소", "cancel", "거절", "실패", "reversed")
            .any { normalized.contains(it, ignoreCase = true) }
        if ((!expenseSignal && !incomeSignal) || cancelled) return null

        val amount = findAmount(normalized) ?: return null
        if (amount <= 0L) return null

        val merchant = findMerchant(title, body)
        if (merchant.isBlank()) return null
        val type = if (incomeSignal && !expenseSignal) "INCOME" else "EXPENSE"
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

    private fun findAmount(text: String): Long? {
        val korean = KoreanAmount.find(text)
        if (korean != null) {
            val base = korean.groupValues[1].toDoubleOrNull() ?: return null
            return when (korean.groupValues[2]) {
                "만" -> (base * 10_000).toLong()
                "천" -> (base * 1_000).toLong()
                else -> null
            }
        }
        return numericAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()
    }

    private fun findMerchant(title: String, body: String): String {
        val source = body.ifBlank { title }
        val cleaned = source
            .replace(numericAmount, " ")
            .replace(KoreanAmount, " ")
            .replace(Regex("[|•·:/\\n]"), " ")
        val noise = setOf(
            "승인", "결제", "출금", "사용", "이용", "입금", "완료", "원", "카드", "신용", "체크",
            "잔액", "누적", "일시불", "할부", "취소", "payment", "purchase", "approved", "입금완료",
        )
        return cleaned
            .split(Regex("\\s+|,"))
            .map { it.trim() }
            .filter { token -> token.length >= 2 && noise.none { word -> token.equals(word, ignoreCase = true) } }
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
