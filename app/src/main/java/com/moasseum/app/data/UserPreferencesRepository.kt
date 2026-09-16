package com.moasseum.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val DEFAULT_PAYMENT_METHODS = listOf("카드", "현금", "이체", "기타")
val DEFAULT_CATEGORY_LABELS = linkedMapOf(
    "FOOD" to "식비",
    "TRANSPORT" to "교통",
    "SHOPPING" to "쇼핑",
    "LIVING" to "생활",
    "HEALTH" to "건강",
    "LEISURE" to "여가",
    "OTHER" to "기타",
)

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

    val paymentMethods: Flow<List<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[PAYMENT_METHODS]
            ?.split('\n')
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.distinct()
            ?.take(12)
            ?.takeIf(List<String>::isNotEmpty)
            ?: DEFAULT_PAYMENT_METHODS
    }

    val categoryLabels: Flow<Map<String, String>> = context.settingsDataStore.data.map { preferences ->
        val stored = preferences[CATEGORY_LABELS].orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val key = line.substringBefore('=', "")
                val label = line.substringAfter('=', "").trim()
                if (key in DEFAULT_CATEGORY_LABELS && label.isNotBlank()) key to label else null
            }
            .toMap()
        DEFAULT_CATEGORY_LABELS + stored
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[DARK_THEME] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[REDUCE_MOTION] = enabled }
    }

    suspend fun setNotificationAccessPromptShown() {
        context.settingsDataStore.edit { preferences -> preferences[NOTIFICATION_ACCESS_PROMPT_SHOWN] = true }
    }

    suspend fun savePaymentMethods(methods: List<String>) {
        val normalized = methods.map(String::trim).filter(String::isNotBlank).distinct().take(12)
        require(normalized.isNotEmpty()) { "결제수단은 한 개 이상 남겨야 해요." }
        context.settingsDataStore.edit { preferences -> preferences[PAYMENT_METHODS] = normalized.joinToString("\n") }
    }

    suspend fun saveCategoryLabels(labels: Map<String, String>) {
        val normalized = DEFAULT_CATEGORY_LABELS.keys.associateWith { key ->
            labels[key]?.trim()?.take(16)?.takeIf(String::isNotBlank) ?: DEFAULT_CATEGORY_LABELS.getValue(key)
        }
        context.settingsDataStore.edit { preferences ->
            preferences[CATEGORY_LABELS] = normalized.entries.joinToString("\n") { (key, value) -> "$key=$value" }
        }
    }

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val NOTIFICATION_ACCESS_PROMPT_SHOWN = booleanPreferencesKey("notification_access_prompt_shown")
        val PAYMENT_METHODS = stringPreferencesKey("payment_methods")
        val CATEGORY_LABELS = stringPreferencesKey("category_labels")
    }
}
