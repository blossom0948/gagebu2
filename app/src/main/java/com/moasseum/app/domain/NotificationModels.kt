package com.moasseum.app.domain

import com.moasseum.app.data.local.NotificationCandidateEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class NotificationCandidate(
    val id: Long,
    val packageName: String,
    val title: String,
    val preview: String,
    val merchant: String,
    val amount: Long,
    val type: TransactionType,
    val categoryKey: String,
    val postedAt: Long,
) {
    val occurredDate: LocalDate
        get() = Instant.ofEpochMilli(postedAt).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun NotificationCandidateEntity.toDomain(): NotificationCandidate =
    NotificationCandidate(
        id = id,
        packageName = packageName,
        title = title,
        preview = preview,
        merchant = merchant,
        amount = amount,
        type = if (type == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE,
        categoryKey = categoryKey,
        postedAt = postedAt,
    )
