package com.moasseum.app

import com.moasseum.app.domain.sharedMoneyNudge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedMoneyNudgeTest {
    @Test fun `empty shared ledger invites without judgment`() {
        assertEquals("함께 시작해요", sharedMoneyNudge(0, 100_000, 0).title)
    }

    @Test fun `over budget nudge suggests one joint adjustment instead of blame`() {
        val nudge = sharedMoneyNudge(120_000, 100_000, 3)

        assertEquals("다음 달을 위한 작은 대화", nudge.title)
        assertTrue(nudge.message.contains("함께"))
        assertTrue(!nudge.message.contains("누가"))
    }

    @Test fun `near budget nudge shows ratio`() {
        assertTrue(sharedMoneyNudge(80_000, 100_000, 2).message.contains("80%"))
    }
}
