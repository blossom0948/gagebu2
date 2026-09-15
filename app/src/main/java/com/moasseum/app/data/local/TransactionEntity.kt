package com.moasseum.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["occurredAt"]),
        Index(value = ["deletedAt"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerId: String = "local-user",
    val ledgerId: String = "personal",
    val type: String,
    val amount: Long,
    val currency: String = "KRW",
    val occurredAt: Long,
    val timezone: String,
    val categoryKey: String,
    val merchant: String,
    val memo: String = "",
    val paymentMethod: String = "카드",
    val source: String = "MANUAL",
    val sharingScope: String = "PRIVATE",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
