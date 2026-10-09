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
    private val bearerTokenProvider: suspend () -> String? = { null },
) {
    suspend fun parseTransaction(text: String, today: LocalDate = LocalDate.now()): Result<AiTransactionCandidate> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(text.isNotBlank()) { "기록할 문장을 입력해 주세요." }
                val trimmedText = text.trim()
                if (baseUrl.isBlank()) return@runCatching LocalNaturalLanguageParser.parse(trimmedText, today)

                // The server is an enhancement, not a prerequisite for recording a transaction.
                // Free-tier AI can briefly return 5xx or be unavailable, so keep the core input
                // flow usable with the on-device parser and preserve the server error only when
                // the local parser cannot understand the sentence either.
                runCatching { parseWithServer(trimmedText, today) }
                    .getOrElse { serverError ->
                        runCatching { LocalNaturalLanguageParser.parse(trimmedText, today) }
                            .getOrElse { throw serverError }
                    }
            }
        }

    suspend fun parseBatchCommand(
        text: String,
        transactions: List<com.moasseum.app.domain.Transaction>,
        today: LocalDate = LocalDate.now(),
    ): Result<BatchCommandPlan> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmedText = text.trim()
                require(trimmedText.isNotBlank()) { "문장을 입력해 주세요." }
                require(trimmedText.length <= 500) { "문장은 500자 이내로 입력해 주세요." }
                require(baseUrl.isNotBlank()) { "AI 서버 주소가 설정되지 않았어요." }
                val token = bearerTokenProvider()?.takeIf(String::isNotBlank)
                    ?: error("AI로 여러 거래를 해석하려면 로그인해 주세요.")
                val endpoint = "${baseUrl.trimEnd('/')}/v1/parse-batch-command"
                val connection = (URL(endpoint).openConnection() as? HttpURLConnection)
                    ?: throw IOException("AI 서버 주소를 확인해 주세요.")
                if (connection.url.protocol != "https") {
                    connection.disconnect()
                    throw IOException("AI 서버는 HTTPS 주소만 사용할 수 있어요.")
                }
                try {
                    connection.requestMethod = "POST"
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 12_000
                    connection.readTimeout = 35_000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    val request = JSONObject().apply {
                        put("schemaVersion", 1)
                        put("text", trimmedText)
                        put("today", today.toString())
                        put("timezone", ZoneId.systemDefault().id)
                    }
                    connection.outputStream.use { output -> output.write(request.toString().toByteArray(Charsets.UTF_8)) }
                    val responseCode = connection.responseCode
                    val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                    val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (responseCode !in 200..299) throw serverError(responseCode, "AI 여러 거래 해석")
                    val root = JSONObject(responseText)
                    require(root.optInt("schemaVersion") == 1) { "AI 응답 버전을 확인하지 못했어요." }
                    when (root.optString("action")) {
                        "ADD" -> {
                            val candidates = root.optJSONArray("candidates") ?: error("AI가 거래 후보를 만들지 못했어요.")
                            require(candidates.length() in 1..30) { "AI가 만든 거래 후보 수를 확인해 주세요." }
                            BatchCommandPlan.Add((0 until candidates.length()).map { index ->
                                parseCandidate(candidates.getJSONObject(index), today)
                            })
                        }
                        "UPDATE", "DELETE" -> {
                            val action = if (root.optString("action") == "UPDATE") BatchCommandAction.UPDATE else BatchCommandAction.DELETE
                            val targetJson = root.optJSONObject("target") ?: error("AI가 검색 조건을 만들지 못했어요.")
                            val type = when (targetJson.optString("type")) {
                                "EXPENSE" -> TransactionType.EXPENSE
                                "INCOME" -> TransactionType.INCOME
                                "ANY" -> null
                                else -> error("AI 검색 유형을 확인해 주세요.")
                            }
                            val merchantTokensJson = targetJson.optJSONArray("merchantTokens") ?: JSONArray()
                            val merchantTokens = (0 until merchantTokensJson.length()).map { index ->
                                merchantTokensJson.getString(index).trim().also { require(it.length in 2..60) }
                            }.distinct().also { require(it.size <= 4) }
                            val categoryKey = targetJson.optString("categoryKey").takeIf(String::isNotBlank)
                            require(categoryKey == null || categoryKey in ALLOWED_CATEGORIES) { "AI 검색 카테고리를 확인해 주세요." }
                            val target = BatchTarget(
                                type = type,
                                amount = targetJson.optLong("amount").let { if (it == 0L) null else it },
                                categoryKey = categoryKey,
                                merchantTokens = merchantTokens,
                                from = targetJson.optString("from").takeIf(String::isNotBlank)?.let(LocalDate::parse),
                                through = targetJson.optString("through").takeIf(String::isNotBlank)?.let(LocalDate::parse),
                            )
                            val edit = if (action == BatchCommandAction.UPDATE) {
                                val editJson = root.optJSONObject("edit") ?: error("AI가 변경할 항목을 만들지 못했어요.")
                                val editType = when (editJson.optString("type")) {
                                    "EXPENSE" -> TransactionType.EXPENSE
                                    "INCOME" -> TransactionType.INCOME
                                    "NONE" -> null
                                    else -> error("AI 변경 유형을 확인해 주세요.")
                                }
                                val editCategory = editJson.optString("categoryKey").takeIf(String::isNotBlank)
                                require(editCategory == null || editCategory in ALLOWED_CATEGORIES) { "AI 변경 카테고리를 확인해 주세요." }
                                BatchEditValues(
                                    amount = editJson.optLong("amount").let { if (it == 0L) null else it },
                                    merchant = editJson.optString("merchant").trim().takeIf(String::isNotBlank),
                                    categoryKey = editCategory,
                                    type = editType,
                                    occurredDate = editJson.optString("occurredDate").takeIf(String::isNotBlank)?.let(LocalDate::parse),
                                    memo = editJson.optString("memo").takeIf(String::isNotBlank),
                                )
                            } else null
                            BatchCommandParser.previewChange(action, target, edit, transactions)
                        }
                        "UNSUPPORTED" -> error("AI가 안전하게 해석하지 못했어요. 기기에서 문장을 바꿔 다시 확인해 주세요.")
                        else -> error("AI가 요청 종류를 확인하지 못했어요.")
                    }
                } finally {
                    connection.disconnect()
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

    private suspend fun parseWithServer(text: String, today: LocalDate): AiTransactionCandidate {
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
            connection.instanceFollowRedirects = false
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
                throw serverError(responseCode, "AI 서버")
            }
            parseResponse(responseText, today)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun analyzeWithServer(state: LedgerUiState): SpendingAnalysis {
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
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12_000
            connection.readTimeout = 25_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            bearerTokenProvider()?.takeIf(String::isNotBlank)?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            val categories = JSONArray().apply {
                state.categoryTotals.take(19).forEach { total ->
                    put(JSONObject().apply {
                        put("categoryKey", total.key)
                        put("total", total.total)
                        put("count", total.count)
                    })
                }
            }
            val history = spendingHistoryJson(state)
            val request = JSONObject().apply {
                put("schemaVersion", 1)
                put("month", state.month.toString())
                put("expenseTotal", state.expenseTotal)
                put("incomeTotal", state.incomeTotal)
                put("budgetAmount", state.budgetAmount ?: JSONObject.NULL)
                put("previousExpenseTotal", state.previousExpenseTotal)
                put("categories", categories)
                put("history", history)
            }
            connection.outputStream.use { output -> output.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) throw serverError(responseCode, "AI 분석")
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

    private suspend fun classifyNotificationWithServer(title: String, text: String): NotificationAiClassification {
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
            connection.instanceFollowRedirects = false
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
            if (responseCode !in 200..299) throw serverError(responseCode, "AI 알림 판별")
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

    private suspend fun askSpendingWithServer(question: String, state: LedgerUiState): String {
        val endpoint = "${baseUrl.trimEnd('/')}/v1/ask-spending"
        val connection = (URL(endpoint).openConnection() as? HttpURLConnection)
            ?: throw IOException("AI 서버 주소를 확인해 주세요.")
        if (connection.url.protocol != "https") {
            connection.disconnect()
            throw IOException("AI 서버는 HTTPS 주소만 사용할 수 있어요.")
        }
        return try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            bearerTokenProvider()?.takeIf(String::isNotBlank)?.let { token ->
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            val categories = JSONArray().apply {
                state.categoryTotals.take(19).forEach { total ->
                    put(JSONObject().apply {
                        put("categoryKey", total.key)
                        put("total", total.total)
                        put("count", total.count)
                    })
                }
            }
            val history = spendingHistoryJson(state)
            val request = JSONObject().apply {
                put("schemaVersion", 1)
                put("question", question.take(200))
                put("month", state.month.toString())
                put("expenseTotal", state.expenseTotal)
                put("incomeTotal", state.incomeTotal)
                put("budgetAmount", state.budgetAmount ?: JSONObject.NULL)
                put("previousExpenseTotal", state.previousExpenseTotal)
                put("categories", categories)
                put("history", history)
            }
            connection.outputStream.use { output -> output.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) throw serverError(responseCode, "AI 질문")
            val answer = JSONObject(responseText).optString("answer").trim()
            require(answer.isNotBlank()) { "AI가 답변을 만들지 못했어요." }
            answer.take(600)
        } finally {
            connection.disconnect()
        }
    }

    private fun serverError(status: Int, label: String) = IOException(
        when (status) {
            401 -> "로그인이 만료됐어요. 관리 → 로그인·계정에서 다시 로그인해 주세요."
            422 -> "AI가 안전하게 해석하지 못했어요. 문장을 짧게 바꾸거나 기기에서 다시 해석해 주세요."
            else -> "$label 응답 오류($status)"
        },
    )

    private fun spendingHistoryJson(state: LedgerUiState): JSONArray = JSONArray().apply {
        state.recentMonthlySummaries(monthCount = 4).dropLast(1).forEach { summary ->
            put(JSONObject().apply {
                put("month", summary.month.toString())
                put("expenseTotal", summary.expenseTotal)
                put("incomeTotal", summary.incomeTotal)
                put("expenseCount", summary.expenseCount)
                put("incomeCount", summary.incomeCount)
                put("categories", JSONArray().apply {
                    summary.categories.take(7).forEach { total ->
                        put(JSONObject().apply {
                            put("categoryKey", total.key)
                            put("total", total.total)
                            put("count", total.count)
                        })
                    }
                })
            })
        }
    }

    private fun parseResponse(responseText: String, today: LocalDate): AiTransactionCandidate {
        val root = JSONObject(responseText)
        val json = root.optJSONObject("data") ?: root
        return parseCandidate(json, today)
    }

    private fun parseCandidate(json: JSONObject, today: LocalDate): AiTransactionCandidate {
        val amount = json.optLong("amount", 0L)
        val occurredDate = json.optString("occurredDate").takeIf(String::isNotBlank)?.let(LocalDate::parse) ?: today
        val merchant = json.optString("merchant").trim()
        require(amount in 1..1_000_000_000_000L) { "AI가 금액을 찾지 못했어요." }
        require(merchant.isNotBlank()) { "AI가 가맹점을 찾지 못했어요." }

        val confidence = json.optJSONObject("confidence")
        val needsConfirmation = buildList {
            val values = json.optJSONArray("needsConfirmation") ?: return@buildList
            for (index in 0 until values.length()) add(values.optString(index))
        }.filter(String::isNotBlank)
        val type = when (json.optString("type").uppercase(Locale.ROOT)) {
            TransactionType.INCOME.name -> TransactionType.INCOME
            TransactionType.EXPENSE.name -> TransactionType.EXPENSE
            else -> error("AI가 거래 유형을 확인하지 못했어요.")
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
        val ALLOWED_CATEGORIES = listOf(
            "FOOD", "CAFE", "TRANSPORT", "SHOPPING", "HOUSING", "COMMUNICATION", "HEALTH", "EDUCATION", "CULTURE",
            "LEISURE", "TRAVEL", "GIFT", "BEAUTY", "PET", "SUBSCRIPTION", "INSURANCE", "FINANCE", "LIVING", "OTHER",
        )
    }
}

private object LocalSpendingAnswer {
    private val categoryLabels = mapOf(
        "FOOD" to "식비",
        "CAFE" to "카페",
        "TRANSPORT" to "교통",
        "SHOPPING" to "쇼핑",
        "HOUSING" to "주거",
        "COMMUNICATION" to "통신",
        "EDUCATION" to "교육",
        "CULTURE" to "문화",
        "TRAVEL" to "여행",
        "GIFT" to "경조사",
        "BEAUTY" to "미용",
        "PET" to "반려동물",
        "SUBSCRIPTION" to "구독",
        "INSURANCE" to "보험",
        "FINANCE" to "금융",
        "LIVING" to "생활",
        "HEALTH" to "의료",
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

internal object LocalNaturalLanguageParser {
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

    fun findAmount(text: String): Long? {
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

    fun inferCategory(text: String): String {
        val lower = text.lowercase(Locale.KOREAN)
        return when {
            listOf("카페", "커피", "스타벅스", "아메리카노", "cafe", "coffee").any { lower.contains(it) } -> "CAFE"
            listOf("식당", "점심", "저녁", "아침", "치킨", "마트", "편의점", "배달", "food", "restaurant").any { lower.contains(it) } -> "FOOD"
            listOf("버스", "지하철", "택시", "주유", "교통", "transport").any { lower.contains(it) } -> "TRANSPORT"
            listOf("쇼핑", "쿠팡", "무신사", "온라인", "shopping").any { lower.contains(it) } -> "SHOPPING"
            listOf("월세", "전세", "관리비", "수도요금", "housing").any { lower.contains(it) } -> "HOUSING"
            listOf("통신비", "휴대폰 요금", "인터넷 요금", "핸드폰 요금", "communication").any { lower.contains(it) } -> "COMMUNICATION"
            listOf("병원", "약국", "건강", "health").any { lower.contains(it) } -> "HEALTH"
            listOf("학원", "교재", "수강료", "교육", "education").any { lower.contains(it) } -> "EDUCATION"
            listOf("전시", "공연", "박물관", "문화", "culture").any { lower.contains(it) } -> "CULTURE"
            listOf("여행", "항공권", "숙박", "호텔", "travel").any { lower.contains(it) } -> "TRAVEL"
            listOf("경조사", "축의금", "조의금", "선물", "gift").any { lower.contains(it) } -> "GIFT"
            listOf("미용실", "헤어", "네일", "화장품", "beauty").any { lower.contains(it) } -> "BEAUTY"
            listOf("반려견", "반려묘", "동물병원", "펫", "pet").any { lower.contains(it) } -> "PET"
            listOf("구독", "넷플릭스", "유튜브 프리미엄", "멤버십", "subscription").any { lower.contains(it) } -> "SUBSCRIPTION"
            listOf("보험료", "보험", "insurance").any { lower.contains(it) } -> "INSURANCE"
            listOf("이자", "수수료", "금융", "finance").any { lower.contains(it) } -> "FINANCE"
            listOf("전기", "가스", "생필품", "생활용품", "living").any { lower.contains(it) } -> "LIVING"
            listOf("영화", "게임", "여가", "leisure").any { lower.contains(it) } -> "LEISURE"
            else -> "OTHER"
        }
    }
}
