package com.moasseum.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "moasseum_settings")

class UserPreferencesRepository(
    private val context: Context,
) {
    val isDarkTheme: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[DARK_THEME] ?: true }

    val reduceMotion: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[REDUCE_MOTION] ?: false }

    val notificationAccessPromptShown: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_ACCESS_PROMPT_SHOWN] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[DARK_THEME] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[REDUCE_MOTION] = enabled }
    }

    suspend fun setNotificationAccessPromptShown() {
        context.settingsDataStore.edit { preferences -> preferences[NOTIFICATION_ACCESS_PROMPT_SHOWN] = true }
    }

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val NOTIFICATION_ACCESS_PROMPT_SHOWN = booleanPreferencesKey("notification_access_prompt_shown")
    }
}
