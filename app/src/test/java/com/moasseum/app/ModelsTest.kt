package com.moasseum.app

import com.moasseum.app.domain.parseAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {
    @Test
    fun `amount parser removes currency punctuation and spaces`() {
        assertEquals(24000L, parseAmount("₩24,000"))
        assertEquals(8000L, parseAmount("8 000원"))
    }

    @Test
    fun `amount parser rejects zero empty and non numeric values`() {
        assertNull(parseAmount("0"))
        assertNull(parseAmount("   "))
        assertNull(parseAmount("원"))
    }
}
