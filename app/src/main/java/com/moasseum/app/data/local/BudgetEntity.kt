package com.moasseum.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val monthKey: String,
    val amount: Long,
    val rollover: Boolean = false,
    val updatedAt: Long,
)
