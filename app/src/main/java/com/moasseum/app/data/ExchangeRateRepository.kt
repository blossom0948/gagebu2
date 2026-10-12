package com.moasseum.app.data

import com.moasseum.app.domain.ExchangeRateQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/** Daily, no-key reference rates from central-bank sources via Frankfurter. */
class ExchangeRateRepository(
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) {
    private val cache = ConcurrentHashMap<String, ExchangeRateQuote>()

    suspend fun quoteToKrw(currencyCode: String, date: LocalDate): ExchangeRateQuote = withContext(Dispatchers.IO) {
        fetchQuote(currencyCode, date.toString())
    }

    suspend fun latestQuoteToKrw(currencyCode: String): ExchangeRateQuote = withContext(Dispatchers.IO) {
        fetchQuote(currencyCode, null)
    }

    private fun fetchQuote(currencyCode: String, requestedDate: String?): ExchangeRateQuote {
        val base = currencyCode.uppercase()
        require(base.matches(Regex("[A-Z]{3}")) && base != "KRW") { "지원하지 않는 통화입니다." }
        val cacheKey = "$base:${requestedDate ?: "latest"}"
        cache[cacheKey]?.let { return it }

        val dateQuery = requestedDate?.let { "?date=$it" }.orEmpty()
        val endpoint = URL("https://api.frankfurter.dev/v2/rate/${base.lowercase()}/krw$dateQuery")
        val connection = openConnection(endpoint)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 12_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 32_768) { "환율 응답이 너무 큽니다." }
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }.orEmpty()
            if (status !in 200..299) throw java.io.IOException("선택한 날짜의 환율을 불러오지 못했어요. 인터넷 연결을 확인해 주세요.")
            val quote = parseExchangeRateQuote(body)
            require(quote.baseCurrency == base && quote.quoteCurrency == "KRW") { "환율 응답의 통화가 일치하지 않아요." }
            cache[cacheKey] = quote
            return quote
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: java.io.IOException) {
            throw error
        } catch (_: Exception) {
            throw java.io.IOException("환율 정보를 확인하지 못했어요. 다시 시도해 주세요.")
        } finally {
            connection.disconnect()
        }
    }
}

internal fun parseExchangeRateQuote(json: String): ExchangeRateQuote {
    require(json.length <= 32_768)
    val value = JSONObject(json)
    val base = value.getString("base").uppercase()
    val quote = value.getString("quote").uppercase()
    val rate = BigDecimal(value.get("rate").toString())
    val date = LocalDate.parse(value.getString("date"))
    require(base.matches(Regex("[A-Z]{3}")) && quote == "KRW")
    require(rate.signum() > 0 && rate.scale() <= 12 && rate.precision() <= 24)
    return ExchangeRateQuote(base, quote, rate.stripTrailingZeros().toPlainString(), date.toString())
}

internal fun parseForeignAmount(input: String, currencyCode: String): BigDecimal? {
    val trimmed = input.trim()
    if (',' in trimmed && !trimmed.matches(Regex("[0-9]{1,3}(,[0-9]{3})+(\\.[0-9]{1,3})?"))) return null
    val normalized = trimmed.replace(",", "")
    if (!normalized.matches(Regex("[0-9]{1,15}(\\.[0-9]{1,3})?"))) return null
    val value = runCatching { BigDecimal(normalized) }.getOrNull() ?: return null
    val decimals = currencyFractionDigits(currencyCode)
    if (value.signum() <= 0 || value.scale().coerceAtLeast(0) > decimals || value.precision() > 18) return null
    return value.stripTrailingZeros()
}

internal fun currencyFractionDigits(currencyCode: String): Int = when (currencyCode.uppercase()) {
    "JPY", "KRW", "CLP", "ISK", "VND" -> 0
    "BHD", "IQD", "JOD", "KWD", "OMR", "TND" -> 3
    else -> 2
}

internal fun convertForeignAmountToKrw(amount: BigDecimal, quote: ExchangeRateQuote): Long? =
    runCatching {
        amount.multiply(BigDecimal(quote.rate)).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()?.takeIf { it in 1..1_000_000_000_000L }

val supportedForeignCurrencies = listOf(
    "USD", "JPY", "EUR", "CNY", "GBP", "TWD", "HKD", "THB", "VND", "SGD",
    "AUD", "CAD", "CHF", "NZD", "PHP", "IDR", "MYR", "INR", "AED", "KWD",
)
