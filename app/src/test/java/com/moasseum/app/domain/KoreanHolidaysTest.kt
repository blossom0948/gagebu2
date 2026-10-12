package com.moasseum.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class KoreanHolidaysTest {
    @Test
    fun `recurring holidays include new legal days from 2026`() {
        assertEquals("노동절", KoreanHolidays.label(LocalDate.parse("2026-05-01")))
        assertNull(KoreanHolidays.label(LocalDate.parse("2025-05-01")))
        assertEquals("제헌절", KoreanHolidays.label(LocalDate.parse("2026-07-17")))
    }

    @Test
    fun `fixed date holidays and historical replacement days are labeled`() {
        assertEquals("한글날", KoreanHolidays.label(LocalDate.parse("2026-10-09")))
        assertEquals("대체공휴일", KoreanHolidays.label(LocalDate.parse("2026-10-05")))
        assertEquals("대체공휴일", KoreanHolidays.label(LocalDate.parse("2023-05-29")))
        assertEquals("대체공휴일", KoreanHolidays.label(LocalDate.parse("2025-10-08")))
        assertNull(KoreanHolidays.label(LocalDate.parse("2026-10-10")))
    }

    @Test
    fun `advance calendar includes 2027 and 2028 holiday blocks`() {
        assertEquals("설날 연휴", KoreanHolidays.label(LocalDate.parse("2027-02-07")))
        assertEquals("대체공휴일", KoreanHolidays.label(LocalDate.parse("2027-02-09")))
        assertEquals("추석 연휴", KoreanHolidays.label(LocalDate.parse("2028-10-03")))
        assertEquals("대체공휴일", KoreanHolidays.label(LocalDate.parse("2028-10-05")))
    }
}
