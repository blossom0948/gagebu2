package com.moasseum.app

import com.moasseum.app.data.AiClient
import com.moasseum.app.domain.AiCandidateSource
import com.moasseum.app.domain.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AiClientTest {
    @Test
    fun `local parser creates a reviewable Korean transaction candidate`() = runBlocking {
        val result = AiClient(baseUrl = "").parseTransaction(
            text = "어제 친구랑 치킨 24000원",
            today = LocalDate.of(2026, 9, 16),
        )

        val candidate = result.getOrThrow()
        assertEquals(TransactionType.EXPENSE, candidate.type)
        assertEquals(24_000L, candidate.amount)
        assertEquals(LocalDate.of(2026, 9, 15), candidate.occurredDate)
        assertEquals("치킨", candidate.merchant)
        assertEquals(AiCandidateSource.LOCAL, candidate.source)
        assertTrue(candidate.needsConfirmation.isEmpty())
    }

    @Test
    fun `server outage falls back to on-device parser`() = runBlocking {
        val result = AiClient(baseUrl = "https://127.0.0.1:1").parseTransaction(
            text = "점심 8000원",
            today = LocalDate.of(2026, 10, 4),
        )

        val candidate = result.getOrThrow()
        assertEquals(8_000L, candidate.amount)
        assertEquals(AiCandidateSource.LOCAL, candidate.source)
    }

    @Test
    fun `local parser distinguishes cafe from food`() = runBlocking {
        val candidate = AiClient(baseUrl = "").parseTransaction("오늘 스타벅스 커피 5500원").getOrThrow()

        assertEquals("CAFE", candidate.categoryKey)
    }

    @Test
    fun `AI batch parsing requires an authenticated request and never silently changes records`() = runBlocking {
        val result = AiClient(
            baseUrl = "https://moasseum-ai-worker.example.workers.dev",
            bearerTokenProvider = { null },
        ).parseBatchCommand("점심 8천원 그리고 커피 4,500원", emptyList())

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("로그인"))
    }
}
