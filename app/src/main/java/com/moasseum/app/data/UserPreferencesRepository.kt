package com.moasseum.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

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

    val notificationPostPermissionPromptShown: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_POST_PERMISSION_PROMPT_SHOWN] ?: false }

    val aiNotificationClassificationEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[AI_NOTIFICATION_CLASSIFICATION_ENABLED] ?: false }

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
                if (isSupportedCategoryKey(key) && label.isNotBlank()) key to label.take(16) else null
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

    suspend fun setNotificationPostPermissionPromptShown() {
        context.settingsDataStore.edit { preferences -> preferences[NOTIFICATION_POST_PERMISSION_PROMPT_SHOWN] = true }
    }

    suspend fun setAiNotificationClassificationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[AI_NOTIFICATION_CLASSIFICATION_ENABLED] = enabled }
    }

    suspend fun savePaymentMethods(methods: List<String>) {
        val normalized = methods.map(String::trim).filter(String::isNotBlank).distinct().take(12)
        require(normalized.isNotEmpty()) { "결제수단은 한 개 이상 남겨야 해요." }
        context.settingsDataStore.edit { preferences -> preferences[PAYMENT_METHODS] = normalized.joinToString("\n") }
    }

    suspend fun saveCategoryLabels(labels: Map<String, String>) {
        val builtIn = DEFAULT_CATEGORY_LABELS.keys.associateWith { key ->
            labels[key]?.trim()?.take(16)?.takeIf(String::isNotBlank) ?: DEFAULT_CATEGORY_LABELS.getValue(key)
        }
        val custom = labels.entries
            .asSequence()
            .filter { (key, value) -> key.matches(CUSTOM_CATEGORY_KEY) && value.trim().isNotBlank() }
            .take(MAX_CUSTOM_CATEGORIES)
            .associate { (key, value) -> key to value.trim().take(16) }
        val normalized = builtIn + custom
        require(normalized.values.map { it.lowercase(Locale.ROOT) }.distinct().size == normalized.size) {
            "카테고리 이름은 서로 다르게 입력해 주세요."
        }
        context.settingsDataStore.edit { preferences ->
            preferences[CATEGORY_LABELS] = normalized.entries.joinToString("\n") { (key, value) -> "$key=$value" }
        }
    }

    private fun isSupportedCategoryKey(key: String): Boolean =
        key in DEFAULT_CATEGORY_LABELS || key.matches(CUSTOM_CATEGORY_KEY)

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val NOTIFICATION_ACCESS_PROMPT_SHOWN = booleanPreferencesKey("notification_access_prompt_shown")
        val NOTIFICATION_POST_PERMISSION_PROMPT_SHOWN = booleanPreferencesKey("notification_post_permission_prompt_shown")
        val AI_NOTIFICATION_CLASSIFICATION_ENABLED = booleanPreferencesKey("ai_notification_classification_enabled")
        val PAYMENT_METHODS = stringPreferencesKey("payment_methods")
        val CATEGORY_LABELS = stringPreferencesKey("category_labels")
        val CUSTOM_CATEGORY_KEY = Regex("CUSTOM_[A-F0-9]{12}")
        const val MAX_CUSTOM_CATEGORIES = 20
    }
}
