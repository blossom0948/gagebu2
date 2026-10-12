package com.moasseum.app

import com.moasseum.app.data.convertForeignAmountToKrw
import com.moasseum.app.data.currencyFractionDigits
import com.moasseum.app.data.parseExchangeRateQuote
import com.moasseum.app.data.parseForeignAmount
import com.moasseum.app.data.ExchangeRateRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayInputStream
import java.util.concurrent.CopyOnWriteArrayList

class ExchangeRateTest {
    @Test fun parsesAndPreservesPublishedReferenceDate() {
        val quote = parseExchangeRateQuote("""{"date":"2026-10-09","base":"USD","quote":"KRW","rate":1342.125} """)
        assertEquals("USD", quote.baseCurrency)
        assertEquals("KRW", quote.quoteCurrency)
        assertEquals("1342.125", quote.rate)
        assertEquals("2026-10-09", quote.date)
    }

    @Test fun foreignDecimalAmountUsesHalfUpWonRounding() {
        val quote = parseExchangeRateQuote("""{"date":"2026-10-09","base":"USD","quote":"KRW","rate":1350.5}""")
        val amount = requireNotNull(parseForeignAmount("10.25", "USD"))
        assertEquals(13_843L, convertForeignAmountToKrw(amount, quote))
    }

    @Test fun currencyFractionDigitsMatchCommonMinorUnits() {
        assertEquals(0, currencyFractionDigits("JPY"))
        assertEquals(2, currencyFractionDigits("USD"))
        assertEquals(3, currencyFractionDigits("KWD"))
        assertNull(parseForeignAmount("100.5", "JPY"))
        assertNull(parseForeignAmount("1.234", "USD"))
        assertNull(parseForeignAmount("-1", "USD"))
        assertEquals(BigDecimal("1200.5"), parseForeignAmount("1,200.50", "USD"))
        assertNull(parseForeignAmount("1,20", "USD"))
    }

    @Test fun rejectsInvalidOrNonKrwQuoteAndUnboundedRate() {
        assertTrue(runCatching { parseExchangeRateQuote("""{"date":"2026-10-09","base":"USD","quote":"EUR","rate":1}""") }.isFailure)
        assertTrue(runCatching { parseExchangeRateQuote("""{"date":"2026-10-09","base":"USD","quote":"KRW","rate":0}""") }.isFailure)
        assertNull(convertForeignAmountToKrw(BigDecimal("999999999999999999"), parseExchangeRateQuote("""{"date":"2026-10-09","base":"USD","quote":"KRW","rate":100000}""")))
    }

    @Test fun latestDisplayRateUsesTheLatestEndpointAndCachesWithinTheProcess() = runBlocking {
        val requestedUrls = CopyOnWriteArrayList<String>()
        val payload = """{"date":"2026-10-12","base":"USD","quote":"KRW","rate":1350.25}""".toByteArray()
        val repository = ExchangeRateRepository { url ->
            requestedUrls += url.toString()
            object : HttpURLConnection(url) {
                override fun connect() = Unit
                override fun disconnect() = Unit
                override fun usingProxy() = false
                override fun getResponseCode() = 200
                override fun getInputStream() = ByteArrayInputStream(payload)
            }
        }

        assertEquals("2026-10-12", repository.latestQuoteToKrw("USD").date)
        repository.latestQuoteToKrw("USD")

        assertEquals(1, requestedUrls.size)
        assertEquals("https://api.frankfurter.dev/v2/rate/usd/krw", requestedUrls.single())
    }
}
