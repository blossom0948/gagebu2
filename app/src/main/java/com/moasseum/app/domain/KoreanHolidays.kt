package com.moasseum.app.domain

import java.time.LocalDate

/** Korean public holidays for recent transaction-calendar years, plus recurring fixed-date holidays. */
object KoreanHolidays {
    private val datedHolidays: Map<LocalDate, String> = buildMap {
        fun day(date: String, name: String) { put(LocalDate.parse(date), name) }
        fun span(start: String, end: String, name: String) {
            var date = LocalDate.parse(start)
            val last = LocalDate.parse(end)
            while (!date.isAfter(last)) {
                put(date, name)
                date = date.plusDays(1)
            }
        }

        // KASI calendar data; 2027–2028 are the published advance calendar tables.
        span("2023-01-21", "2023-01-23", "설날 연휴")
        day("2023-01-24", "대체공휴일")
        day("2023-05-27", "부처님오신날")
        day("2023-05-29", "대체공휴일")
        span("2023-09-28", "2023-09-30", "추석 연휴")

        span("2024-02-09", "2024-02-11", "설날 연휴")
        day("2024-02-12", "대체공휴일")
        day("2024-05-06", "대체공휴일")
        day("2024-05-15", "부처님오신날")
        span("2024-09-16", "2024-09-18", "추석 연휴")

        span("2025-01-28", "2025-01-30", "설날 연휴")
        day("2025-03-03", "대체공휴일")
        day("2025-05-05", "어린이날·부처님오신날")
        day("2025-05-06", "대체공휴일")
        span("2025-10-05", "2025-10-07", "추석 연휴")
        day("2025-10-08", "대체공휴일")

        span("2026-02-16", "2026-02-18", "설날 연휴")
        day("2026-03-02", "대체공휴일")
        day("2026-05-24", "부처님오신날")
        day("2026-05-25", "대체공휴일")
        day("2026-06-03", "전국동시지방선거")
        day("2026-08-17", "대체공휴일")
        span("2026-09-24", "2026-09-26", "추석 연휴")
        day("2026-10-05", "대체공휴일")

        span("2027-02-06", "2027-02-08", "설날 연휴")
        day("2027-02-09", "대체공휴일")
        day("2027-05-03", "대체공휴일")
        day("2027-05-13", "부처님오신날")
        day("2027-08-16", "대체공휴일")
        span("2027-09-14", "2027-09-16", "추석 연휴")
        day("2027-10-04", "대체공휴일")
        day("2027-10-11", "대체공휴일")
        day("2027-07-19", "대체공휴일")
        day("2027-12-27", "대체공휴일")

        span("2028-01-26", "2028-01-28", "설날 연휴")
        day("2028-04-12", "국회의원선거")
        day("2028-05-02", "부처님오신날")
        span("2028-10-02", "2028-10-04", "추석 연휴")
        day("2028-10-05", "대체공휴일")
    }

    /** Returns the holiday label for a date, or null for an ordinary weekday/weekend. */
    fun label(date: LocalDate): String? = datedHolidays[date] ?: when {
        date.monthValue == 1 && date.dayOfMonth == 1 -> "신정"
        date.monthValue == 3 && date.dayOfMonth == 1 -> "삼일절"
        date.monthValue == 5 && date.dayOfMonth == 1 && date.year >= 2026 -> "노동절"
        date.monthValue == 5 && date.dayOfMonth == 5 -> "어린이날"
        date.monthValue == 6 && date.dayOfMonth == 6 -> "현충일"
        date.monthValue == 7 && date.dayOfMonth == 17 && date.year >= 2026 -> "제헌절"
        date.monthValue == 8 && date.dayOfMonth == 15 -> "광복절"
        date.monthValue == 10 && date.dayOfMonth == 3 -> "개천절"
        date.monthValue == 10 && date.dayOfMonth == 9 -> "한글날"
        date.monthValue == 12 && date.dayOfMonth == 25 -> "성탄절"
        else -> null
    }
}
