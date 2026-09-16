package com.moasseum.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [Index(value = ["isActive", "nextOccurrenceDate"])],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amount: Long,
    val merchant: String,
    val dayOfMonth: Int,
    val nextOccurrenceDate: String,
    val categoryKey: String,
    val memo: String,
    val paymentMethod: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
