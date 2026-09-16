package com.moasseum.app.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.moasseum.app.domain.AiCandidateSource
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ReceiptOcr {
    private val labeledAmount = Regex(
        """(?i)(?:합계|총액|총\s*결제|결제\s*금액|승인\s*금액|받을\s*금액|청구\s*금액|total|amount\s*due)\D{0,16}([0-9][0-9,]{2,})""",
    )
    private val number = Regex("""(?<!\d)(\d{1,3}(?:,\d{3})+|\d{4,})(?!\d)""")
    private val date = Regex("""(?<!\d)(20\d{2})[./년-]\s*(\d{1,2})[./월-]\s*(\d{1,2})""")
    private val boilerplate = listOf(
        "영수증", "사업자", "등록번호", "대표자", "주소", "전화", "승인번호", "카드번호", "주문번호", "합계", "결제금액",
    )

    suspend fun recognize(context: Context, uri: Uri): String = suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        continuation.invokeOnCancellation { recognizer.close() }
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    if (continuation.isActive) continuation.resume(result.text)
                }
                .addOnFailureListener { error ->
                    recognizer.close()
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        } catch (error: Exception) {
            recognizer.close()
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }

    fun candidateFromText(text: String, today: LocalDate = LocalDate.now()): AiTransactionCandidate? {
        val amountMatch = labeledAmount.find(text)
        val amount = amountMatch?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()
            ?: number.findAll(text).mapNotNull { it.groupValues[1].replace(",", "").toLongOrNull() }
                .filter { it in 100L..1_000_000_000L }
                .maxOrNull()
        if (amount == null || amount <= 0L) return null

        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        val merchant = lines.firstOrNull { line ->
            line.any { it.isLetter() } && line.none(Char::isDigit) && boilerplate.none { line.contains(it, ignoreCase = true) }
        }?.take(80) ?: "알 수 없음"
        val receiptDate = date.find(text)?.let { match ->
            runCatching {
                LocalDate.of(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt())
            }.getOrNull()
        } ?: today
        val category = when {
            listOf("약국", "병원", "의원", "약품").any(text::contains) -> "HEALTH"
            listOf("택시", "주유", "버스", "지하철").any(text::contains) -> "TRANSPORT"
            listOf("카페", "식당", "식품", "배달", "음식").any(text::contains) -> "FOOD"
            else -> "OTHER"
        }
        val needsConfirmation = buildList {
            if (amountMatch == null) add("amount")
            if (merchant == "알 수 없음") add("merchant")
            if (category == "OTHER") add("category")
            if (date.find(text) == null) add("date")
        }
        return AiTransactionCandidate(
            type = TransactionType.EXPENSE,
            amount = amount,
            occurredDate = receiptDate,
            categoryKey = category,
            merchant = merchant,
            memo = "영수증에서 인식",
            source = AiCandidateSource.LOCAL,
            amountConfidence = if (amountMatch != null) 0.9 else 0.55,
            dateConfidence = if (date.find(text) != null) 0.85 else 0.45,
            categoryConfidence = if (category != "OTHER") 0.7 else 0.4,
            needsConfirmation = needsConfirmation,
        )
    }
}
