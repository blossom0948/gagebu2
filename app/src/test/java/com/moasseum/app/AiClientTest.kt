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
}
