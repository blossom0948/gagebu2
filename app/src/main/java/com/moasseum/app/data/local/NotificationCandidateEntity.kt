package com.moasseum.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_candidates",
    indices = [Index(value = ["fingerprint"], unique = true)],
)
data class NotificationCandidateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String,
    val preview: String,
    val merchant: String,
    val amount: Long,
    val type: String,
    val categoryKey: String,
    val postedAt: Long,
    val fingerprint: String,
    val status: String = "PENDING",
)
