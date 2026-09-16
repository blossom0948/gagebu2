package com.moasseum.app.domain

import java.time.LocalDate

fun firstRecurringOccurrence(today: LocalDate, dayOfMonth: Int): LocalDate {
    require(dayOfMonth in 1..31)
    val candidate = today.withDayOfMonth(dayOfMonth.coerceAtMost(today.lengthOfMonth()))
    if (!candidate.isBefore(today)) return candidate
    val nextMonth = today.plusMonths(1)
    return nextMonth.withDayOfMonth(dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
}

fun nextRecurringOccurrence(current: LocalDate, dayOfMonth: Int): LocalDate {
    require(dayOfMonth in 1..31)
    val nextMonth = current.plusMonths(1)
    return nextMonth.withDayOfMonth(dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
}
