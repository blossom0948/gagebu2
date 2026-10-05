package com.moasseum.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val openingBalance: Long,
    val archived: Boolean = false,
    val updatedAt: Long,
)
