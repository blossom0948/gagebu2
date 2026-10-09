package com.moasseum.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.moasseum.app.domain.AiCandidateSource
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.TransactionType
import java.time.LocalDate
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ReceiptOcrRow(val text: String, val bounds: PhotoEvidenceBounds)

data class ReceiptOcrDocument(val text: String, val rows: List<ReceiptOcrRow>) {
    fun boundsForLines(start: Int?, end: Int?): PhotoEvidenceBounds? {
        if (start == null || end == null || start !in rows.indices || end < start) return null
        val selected = rows.subList(start, minOf(end, rows.lastIndex) + 1)
        if (selected.isEmpty()) return null
        return PhotoEvidenceBounds(
            left = selected.minOf { it.bounds.left },
            top = selected.minOf { it.bounds.top },
            right = selected.maxOf { it.bounds.right },
            bottom = selected.maxOf { it.bounds.bottom },
        )
    }
}

object ReceiptOcr {
    private val labeledAmount = Regex(
        """(?i)(?:합계|총액|총\s*결제|결제\s*금액|승인\s*금액|받을\s*금액|청구\s*금액|total|amount\s*due)\D{0,16}([0-9][0-9,]{2,})""",
    )
    private val number = Regex("""(?<!\d)(\d{1,3}(?:,\d{3})+|\d{4,})(?!\d)""")
    private val date = Regex("""(?<!\d)(20\d{2})[./년-]\s*(\d{1,2})[./월-]\s*(\d{1,2})""")
    private val boilerplate = listOf(
        "영수증", "사업자", "등록번호", "대표자", "주소", "전화", "승인번호", "카드번호", "주문번호", "합계", "결제금액",
    )

    suspend fun recognize(context: Context, uri: Uri): String = recognizeDocument(context, uri).text

    suspend fun recognizeDocument(context: Context, uri: Uri): ReceiptOcrDocument = suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        continuation.invokeOnCancellation { recognizer.close() }
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    recognizer.close()
                    if (continuation.isActive) {
                        val rows = visualRows(result.textBlocks.flatMap { it.lines }, image.width, image.height)
                        val text = rows?.joinToString("\n") { it.text } ?: result.text
                        continuation.resume(ReceiptOcrDocument(text, rows.orEmpty()))
                    }
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

    /** Creates private-cache crops containing only the uncertain transaction row(s). */
    fun createEvidenceCrops(
        context: Context,
        uri: Uri,
        crops: Map<String, PhotoEvidenceBounds>,
    ): Map<String, String> {
        if (crops.isEmpty()) return emptyMap()
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val readBounds = runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        }.isSuccess
        if (!readBounds || boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return emptyMap()
        var sampleSize = 1
        while (maxOf(boundsOptions.outWidth, boundsOptions.outHeight) / sampleSize > 2400) sampleSize *= 2
        val bitmap = runCatching {
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull() ?: return emptyMap()
        val directory = File(context.cacheDir, "photo-import-evidence").apply { mkdirs() }
        val outputs = mutableMapOf<String, String>()
        try {
            crops.forEach { (candidateId, bounds) ->
                val horizontalPadding = maxOf(0.025f, (bounds.right - bounds.left) * 0.12f)
                val verticalPadding = maxOf(0.009f, (bounds.bottom - bounds.top) * 0.22f)
                val left = ((bounds.left - horizontalPadding).coerceIn(0f, 1f) * (bitmap.width - 1)).toInt().coerceIn(0, bitmap.width - 1)
                val top = ((bounds.top - verticalPadding).coerceIn(0f, 1f) * (bitmap.height - 1)).toInt().coerceIn(0, bitmap.height - 1)
                val right = ((bounds.right + horizontalPadding).coerceIn(0f, 1f) * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
                val bottom = ((bounds.bottom + verticalPadding).coerceIn(0f, 1f) * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
                val crop = runCatching {
                    Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                }.getOrNull() ?: return@forEach
                val file = File(directory, "${candidateId}.jpg")
                val saved = runCatching {
                    FileOutputStream(file).use { output ->
                        check(crop.compress(Bitmap.CompressFormat.JPEG, 90, output))
                        output.fd.sync()
                    }
                    file.absolutePath
                }.getOrNull()
                crop.recycle()
                if (saved != null) outputs[candidateId] = saved
            }
        } finally {
            bitmap.recycle()
        }
        return outputs
    }

    fun deleteEvidenceCrops(paths: Collection<String>) {
        paths.distinct().forEach { path -> runCatching { File(path).delete() } }
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
            listOf("카페", "커피", "스타벅스", "아메리카노").any(text::contains) -> "CAFE"
            listOf("약국", "병원", "의원", "약품").any(text::contains) -> "HEALTH"
            listOf("택시", "주유", "버스", "지하철").any(text::contains) -> "TRANSPORT"
            listOf("식당", "식품", "배달", "음식").any(text::contains) -> "FOOD"
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

    private data class PositionedLine(val text: String, val bounds: Rect?) {
        val centerY: Double get() = bounds?.let { (it.top + it.bottom) / 2.0 } ?: Double.MAX_VALUE
        val height: Int get() = bounds?.height()?.coerceAtLeast(1) ?: 1
        val left: Int get() = bounds?.left ?: 0
    }

    /** Rebuild OCR in visual row order so merchant and amount stay together, retaining row bounds. */
    private fun visualRows(
        lines: List<com.google.mlkit.vision.text.Text.Line>,
        imageWidth: Int,
        imageHeight: Int,
    ): List<ReceiptOcrRow>? {
        val positioned = lines.map { PositionedLine(it.text.trim(), it.boundingBox) }
            .filter { it.text.isNotBlank() }
            .sortedWith(compareBy<PositionedLine> { it.centerY }.thenBy { it.left })
        if (positioned.isEmpty() || imageWidth <= 0 || imageHeight <= 0 || positioned.any { it.bounds == null }) return null

        val rows = mutableListOf<MutableList<PositionedLine>>()
        positioned.forEach { line ->
            val row = rows.lastOrNull()?.takeIf { previous ->
                val baseline = previous.map(PositionedLine::centerY).average()
                val threshold = maxOf(10.0, minOf(line.height, previous.minOf(PositionedLine::height)) * 0.55)
                kotlin.math.abs(line.centerY - baseline) <= threshold
            }
            if (row == null) rows += mutableListOf(line) else row += line
        }
        return rows.map { row ->
            val bounds = row.mapNotNull(PositionedLine::bounds)
            ReceiptOcrRow(
                text = row.sortedBy(PositionedLine::left).joinToString(" ") { it.text },
                bounds = PhotoEvidenceBounds(
                    left = (bounds.minOf { it.left }.toFloat() / imageWidth).coerceIn(0f, 1f),
                    top = (bounds.minOf { it.top }.toFloat() / imageHeight).coerceIn(0f, 1f),
                    right = (bounds.maxOf { it.right }.toFloat() / imageWidth).coerceIn(0f, 1f),
                    bottom = (bounds.maxOf { it.bottom }.toFloat() / imageHeight).coerceIn(0f, 1f),
                ),
            )
        }
    }
}
