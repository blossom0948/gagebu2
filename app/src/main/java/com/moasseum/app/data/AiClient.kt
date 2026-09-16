package com.moasseum.app.data

import com.moasseum.app.BuildConfig
import com.moasseum.app.domain.AiCandidateSource
import com.moasseum.app.domain.AiTransactionCandidate
import com.moasseum.app.domain.LedgerUiState
import com.moasseum.app.domain.SpendingAnalysis
import com.moasseum.app.domain.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NotificationAiClassification(
    val isFinancialTransaction: Boolean,
    val transactionType: TransactionType?,
)

class AiClient(
    private val baseUrl: String = BuildConfig.AI_API_BASE_URL,
    private val bearerTokenProvider: () -> String? = { null },
) {
    suspend fun parseTransaction(text: String, today: LocalDate = LocalDate.now()): Result<AiTransactionCandidate> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(text.isNotBlank()) { "기록할 문장을 입력해 주세요." }
                if (baseUrl.isBlank()) {
                    LocalNaturalLanguageParser.parse(text.trim(), today)
                } else {
                    parseWithServer(text.trim(), today)
                }
            }
        }

    suspend fun analyzeSpending(state: LedgerUiState): Result<SpendingAnalysis> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(baseUrl.isNotBlank()) { "AI 분석 서버 주소가 설정되지 않았어요." }
                analyzeWithServer(state)
            }
        }

    suspend fun askSpending(question: String, state: LedgerUiState): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(question.trim().isNotBlank()) { "질문을 입력해 주세요." }
                require(question.trim().length <= 200) { "질문은 200자 이내로 입력해 주세요." }
                if (baseUrl.isBlank()) LocalSpendingAnswer.answer(question.trim(), state) else askSpendingWithServer(question.trim(), state)
            }
        }

    suspend fun classifyNotification(title: String, text: String): Result<NotificationAiClassification> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(baseUrl.isNotBlank()) { "AI 알림 판별 서버 주소가 설정되지 않았어요." }
                classifyNotificationWithServer(title, text)
            }
        }

    private fun parseWithServer(text: String, today: LocalDate): AiTransactionCandidate {
        val endpoint = if (baseUrl.endsWith("/v1/parse-transaction")) {
            baseUrl
        } else {
            "${baseUrl.trimEnd('/')}/v1/parse-transaction"
        }
        val connection = (URL(endpoint).openConnection() as? HttpURLConnection)
            ?: throw IOException("AI 서버 주소를 확인해 주세요.")
        if (connection.url.protocol != "https") {
            connection.disconnect()
            throw IOException("AI 서버는 HTTPS 주소만 사용할 수 있어요.")
        }

        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            bearerTokenProvider()?.takeIf { it.isNotBlank() }?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            val request = JSONObject().apply {
                put("schemaVersion", 1)
                put("text", text)
                put("today", today.toString())
                put("timezone", ZoneId.systemDefault().id)
                put("allowedCategories", JSONArray(ALLOWED_CATEGORIES))
            }
            connection.outputStream.use { output -> output.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IOException("AI 서버 응답 오류($responseCode)")
            }
            parseResponse(responseText, today)
        } finally {
            connection.disconnect()
        }
    }

    private fun analyzeWithServer(state: LedgerUiState): SpendingAnalysis {
        val endpoint = if (baseUrl.endsWith("/v1/analyze-spending")) {
            baseUrl
        } else {
            "${baseUrl.trimEnd('/')}/v1/analyze-spending"
        }
        val connection = (URL(endpoint).openConnection() as? HttpURLConnection)
            ?: throw IOException("AI 서버 주소를 확인해 주세요.")
        if (connection.url.protocol != "https") {
            connection.disconnect()
            throw IOException("AI 서버는 HTTPS 주소만 사용할 수 있어요.")
        }
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12_000
            connection.readTimeout = 25_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            bearerTokenProvider()?.takeIf(String::isNotBlank)?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            val categories = JSONArray().apply {
                state.categoryTotals.take(7).forEach { total ->
                    put(JSONObject().apply {
                        put("categoryKey", total.key)
                        put("total", total.total)
                        put("count", total.count)
                    })
                }
            }
            val request = JSONObject().apply {
                put("schemaVersion", 1)
                put("month", state.month.toString())
                put("expenseTotal", state.expenseTotal)
                put("incomeTotal", state.incomeTotal)
                put("budgetAmount", state.budgetAmount ?: JSONObject.NULL)
                put("previousExpenseTotal", state.previousExpenseTotal)
                put("categories", categories)
            }
            connection.outputStream.use { output -> output.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) throw IOException("AI 분석 응답 오류($responseCode)")
            val root = JSONObject(responseText)
            val summary = root.optString("summary").trim()
            require(summary.isNotBlank()) { "AI가 분석 문장을 만들지 못했어요." }
            SpendingAnalysis(
                summary = summary,
                observations = root.optJSONArray("observations").toStringList(),
                suggestions = root.optJSONArray("suggestions").toStringList(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun classifyNotificationWithServer(title: String, text: String): NotificationAiClassification {
        require(title.isNotBlank() && title.length <= 200) { "알림 제목을 확인해 주세요." }
        require(text.isNotBlank() && text.length <= 2_000) { "알림 내용을 확인해 주세요." }
        val endpoint = "${baseUrl.trimEnd('/')}/v1/classify-notification"
        val connection = (URL(endpoint).openConnection() as? HttpURLConnection)
            ?: throw IOException("AI 서버 주소를 확인해 주세요.")
        if (connection.url.protocol != "https") {
            connection.disconnect()
            throw IOException("AI 서버는 HTTPS 주소만 사용할 수 있어요.")
        }
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            bearerTokenProvider()?.takeIf(String::isNotBlank)?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            val request = JSONObject().apply {
                put("title", title.take(200))
                put("text", text.take(2_000))
            }
            connection.outputStream.use { output -> output.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) throw IOException("AI 알림 판별 응답 오류($responseCode)")
            val root = JSONObject(responseText)
            val isFinancialTransaction = root.optBoolean("isFinancialTransaction", false)
            val transactionType = when (root.optString("type")) {
                TransactionType.EXPENSE.name -> TransactionType.EXPENSE
                TransactionType.INCOME.name -> TransactionType.INCOME
                else -> null
            }
            NotificationAiClassification(
                isFinancialTransaction = isFinancialTransaction && transactionType != null,
                transactionType = transactionType,
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun askSpendingWithServer(question: String, state: LedgerUiState): String {
        val endpoint = "${baseUrl.trimEnd('/')}/v1/ask-spending"
        val connection = (URL(endpoint).openConnection() as? HttpURLConnection)
            ?: throw IOException("AI 서버 주소를 확인해 주세요.")
        if (connection.url.protocol != "https") {
            connection.disconnect()
            throw IOException("AI 서버는 HTTPS 주소만 사용할 수 있어요.")
        }
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            bearerTokenProvider()?.takeIf(String::isNotBlank)?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            val categories = JSONArray().apply {
                state.categoryTotals.take(7).forEach { total ->
                    put(JSONObject().apply {
                        put("categoryKey", total.key)
                        put("total", total.total)
                        put("count", total.count)
                    })
                }
            }
            val request = JSONObject().apply {
                put("schemaVersion", 1)
                put("question", question.take(200))
                put("month", state.month.toString())
                put("expenseTotal", state.expenseTotal)
                put("incomeTotal", state.incomeTotal)
                put("budgetAmount", state.budgetAmount ?: JSONObject.NULL)
                put("previousExpenseTotal", state.previousExpenseTotal)
                put("categories", categories)
            }
            connection.outputStream.use { output -> output.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) throw IOException("AI 질문 응답 오류($responseCode)")
            val answer = JSONObject(responseText).optString("answer").trim()
            require(answer.isNotBlank()) { "AI가 답변을 만들지 못했어요." }
            answer.take(600)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseResponse(responseText: String, today: LocalDate): AiTransactionCandidate {
        val root = JSONObject(responseText)
        val json = root.optJSONObject("data") ?: root
        val amount = json.optLong("amount", 0L)
        val occurredDate = json.optString("occurredDate").takeIf(String::isNotBlank)?.let(LocalDate::parse) ?: today
        val merchant = json.optString("merchant").trim()
        require(amount > 0L) { "AI가 금액을 찾지 못했어요." }
        require(merchant.isNotBlank()) { "AI가 가맹점을 찾지 못했어요." }

        val confidence = json.optJSONObject("confidence")
        val needsConfirmation = buildList {
            val values = json.optJSONArray("needsConfirmation") ?: return@buildList
            for (index in 0 until values.length()) add(values.optString(index))
        }.filter(String::isNotBlank)
        val type = if (json.optString("type").uppercase(Locale.ROOT) == TransactionType.INCOME.name) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }
        val category = json.optString("categoryKey").takeIf { it in ALLOWED_CATEGORIES } ?: "OTHER"
        return AiTransactionCandidate(
            type = type,
            amount = amount,
            occurredDate = occurredDate,
            categoryKey = category,
            merchant = merchant,
            memo = json.optString("memo").trim(),
            source = AiCandidateSource.SERVER,
            amountConfidence = confidence?.optDouble("amount", 0.8) ?: 0.8,
            dateConfidence = confidence?.optDouble("date", 0.7) ?: 0.7,
            categoryConfidence = confidence?.optDouble("category", 0.7) ?: 0.7,
            needsConfirmation = needsConfirmation,
        )
    }

    companion object {
        val ALLOWED_CATEGORIES = listOf("FOOD", "TRANSPORT", "SHOPPING", "LIVING", "HEALTH", "LEISURE", "OTHER")
    }
}

private object LocalSpendingAnswer {
    private val categoryLabels = mapOf(
        "FOOD" to "식비",
        "TRANSPORT" to "교통",
        "SHOPPING" to "쇼핑",
        "LIVING" to "생활",
        "HEALTH" to "건강",
        "LEISURE" to "여가",
        "OTHER" to "기타",
    )

    fun answer(question: String, state: LedgerUiState): String {
        val lower = question.lowercase(Locale.KOREAN)
        return when {
            lower.contains("예산") -> state.budgetAmount?.let { budget ->
                "${state.month} 예산은 ${com.moasseum.app.domain.formatWon(budget)}이고, 현재 ${com.moasseum.app.domain.formatWon(state.expenseTotal)}를 썼어요."
            } ?: "아직 월 예산을 설정하지 않았어요. 관리 화면에서 목표 지출을 정해 보세요."
            lower.contains("가장") || lower.contains("많이") || lower.contains("카테고리") ->
                state.categoryTotals.firstOrNull()?.let { total ->
                    "이번 달에는 ${categoryLabels[total.key] ?: total.key} 지출이 ${com.moasseum.app.domain.formatWon(total.total)}로 가장 커요."
                }
                    ?: "아직 분석할 거래가 없어요."
            else -> "이번 달 지출은 ${com.moasseum.app.domain.formatWon(state.expenseTotal)}, 수입은 ${com.moasseum.app.domain.formatWon(state.incomeTotal)}예요."
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optString(index).takeIf(String::isNotBlank) }.take(3)
}

private object LocalNaturalLanguageParser {
    private val numericAmount = Regex("""(?<!\d)(\d{1,3}(?:,\d{3})+|\d{3,})\s*원?""")
    private val koreanAmount = Regex("""(?<!\d)(\d+(?:\.\d+)?)\s*(만|천)\s*원?""")
    private val datePattern = Regex("""(\d{1,2})월\s*(\d{1,2})일""")

    fun parse(text: String, today: LocalDate): AiTransactionCandidate {
        val amount = findAmount(text) ?: throw IllegalArgumentException("금액을 찾지 못했어요. 예: 점심 8천원")
        val date = findDate(text, today)
        val category = inferCategory(text)
        val merchant = inferMerchant(text)
        val memo = when {
            text.contains("친구") -> "친구와 함께"
            text.contains("회사") -> "회사에서"
            else -> ""
        }
        return AiTransactionCandidate(
            type = if (listOf("입금", "월급", "급여", "환급", "받았").any(text::contains)) TransactionType.INCOME else TransactionType.EXPENSE,
            amount = amount,
            occurredDate = date,
            categoryKey = category,
            merchant = merchant,
            memo = memo,
            source = AiCandidateSource.LOCAL,
            amountConfidence = 0.98,
            dateConfidence = if (date != today) 0.9 else 0.75,
            categoryConfidence = if (category == "OTHER") 0.45 else 0.72,
            needsConfirmation = buildList {
                if (category == "OTHER") add("category")
                if (merchant == "알 수 없음") add("merchant")
            },
        )
    }

    private fun findAmount(text: String): Long? {
        koreanAmount.find(text)?.let { match ->
            val base = match.groupValues[1].toDoubleOrNull() ?: return@let
            return when (match.groupValues[2]) {
                "만" -> (base * 10_000).toLong()
                "천" -> (base * 1_000).toLong()
                else -> null
            }
        }
        return numericAmount.find(text)?.groupValues?.getOrNull(1)?.replace(",", "")?.toLongOrNull()
    }

    private fun findDate(text: String, today: LocalDate): LocalDate {
        return when {
            text.contains("그저께") || text.contains("그제") -> today.minusDays(2)
            text.contains("어제") -> today.minusDays(1)
            text.contains("내일") -> today.plusDays(1)
            else -> datePattern.find(text)?.let { match ->
                val month = match.groupValues[1].toIntOrNull() ?: return@let today
                val day = match.groupValues[2].toIntOrNull() ?: return@let today
                runCatching { LocalDate.of(today.year, month, day) }.getOrDefault(today)
            } ?: today
        }
    }

    private fun inferMerchant(text: String): String {
        val cleaned = text
            .replace(numericAmount, " ")
            .replace(koreanAmount, " ")
            .replace(datePattern, " ")
            .replace(Regex("오늘|어제|그저께|그제|내일|친구랑|친구와|친구|점심|저녁|아침|결제|사용|샀어|샀어요|입금|받았어|받았어요"), " ")
            .split(Regex("\\s+"))
            .map { token -> token.trim().replace(Regex("(에서|으로|에게|랑|와|과)$"), "") }
            .filter { it.length >= 2 }
            .firstOrNull()
        return cleaned ?: "알 수 없음"
    }

    private fun inferCategory(text: String): String {
        val lower = text.lowercase(Locale.KOREAN)
        return when {
            listOf("식당", "점심", "저녁", "아침", "치킨", "커피", "카페", "마트", "편의점", "배달", "food").any { lower.contains(it) } -> "FOOD"
            listOf("버스", "지하철", "택시", "주유", "교통", "transport").any { lower.contains(it) } -> "TRANSPORT"
            listOf("쇼핑", "쿠팡", "무신사", "온라인", "shopping").any { lower.contains(it) } -> "SHOPPING"
            listOf("월세", "전기", "가스", "생활", "주거", "living").any { lower.contains(it) } -> "LIVING"
            listOf("병원", "약국", "건강", "health").any { lower.contains(it) } -> "HEALTH"
            listOf("영화", "게임", "넷플릭스", "유튜브", "여가", "leisure").any { lower.contains(it) } -> "LEISURE"
            else -> "OTHER"
        }
    }
}
