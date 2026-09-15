package com.moasseum.app

import android.app.Application
import com.moasseum.app.data.FinanceRepository
import com.moasseum.app.data.UserPreferencesRepository
import com.moasseum.app.data.local.FinanceDatabase

class FinanceApplication : Application() {
    val database by lazy { FinanceDatabase.create(this) }
    val financeRepository by lazy { FinanceRepository(database.financeDao()) }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
}
