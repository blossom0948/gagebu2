package com.moasseum.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moasseum.app.domain.PaymentCard
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

    val notificationServiceConnectedAt: Flow<Long?> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_SERVICE_CONNECTED_AT]?.takeIf { it > 0L } }

    val notificationLastSeenAt: Flow<Long?> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_LAST_SEEN_AT]?.takeIf { it > 0L } }

    val notificationLastCandidateAt: Flow<Long?> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_LAST_CANDIDATE_AT]?.takeIf { it > 0L } }

    val notificationServiceDisconnectedAt: Flow<Long?> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_SERVICE_DISCONNECTED_AT]?.takeIf { it > 0L } }

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

    val paymentCards: Flow<List<PaymentCard>> = context.settingsDataStore.data.map { preferences ->
        preferences[PAYMENT_CARDS]
            .orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val fields = line.split('|')
                val id = fields.getOrNull(0)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val name = fields.getOrNull(1)?.trim()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val dueDay = fields.getOrNull(2)?.toIntOrNull()?.takeIf { it in 1..31 } ?: return@mapNotNull null
                PaymentCard(id = id, name = name, dueDay = dueDay)
            }
            .distinctBy(PaymentCard::id)
            .take(MAX_PAYMENT_CARDS)
            .toList()
    }

    val categoryBudgets: Flow<Map<String, Long>> = context.settingsDataStore.data.map { preferences ->
        preferences[CATEGORY_BUDGETS]
            .orEmpty()
            .lineSequence()
            .mapNotNull { line ->
                val key = line.substringBefore('=', "")
                val amount = line.substringAfter('=', "").toLongOrNull()
                if (isSupportedCategoryKey(key) && amount != null && amount > 0L) key to amount else null
            }
            .take(MAX_CATEGORY_BUDGETS)
            .toMap()
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

    suspend fun markNotificationServiceConnected(at: Long = System.currentTimeMillis()) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATION_SERVICE_CONNECTED_AT] = at
            preferences.remove(NOTIFICATION_SERVICE_DISCONNECTED_AT)
        }
    }

    suspend fun markNotificationServiceDisconnected(at: Long = System.currentTimeMillis()) {
        context.settingsDataStore.edit { preferences -> preferences[NOTIFICATION_SERVICE_DISCONNECTED_AT] = at }
    }

    suspend fun markNotificationSeen(at: Long = System.currentTimeMillis()) {
        context.settingsDataStore.edit { preferences -> preferences[NOTIFICATION_LAST_SEEN_AT] = at }
    }

    suspend fun markNotificationCandidateCreated(at: Long = System.currentTimeMillis()) {
        context.settingsDataStore.edit { preferences -> preferences[NOTIFICATION_LAST_CANDIDATE_AT] = at }
    }

    suspend fun savePaymentMethods(methods: List<String>) {
        val normalized = methods.map(String::trim).filter(String::isNotBlank).distinct().take(12)
        require(normalized.isNotEmpty()) { "결제수단은 한 개 이상 남겨야 해요." }
        context.settingsDataStore.edit { preferences -> preferences[PAYMENT_METHODS] = normalized.joinToString("\n") }
    }

    suspend fun savePaymentCards(cards: List<PaymentCard>) {
        val normalized = cards
            .mapNotNull { card ->
                val name = card.name.replace('|', '｜').trim().take(24).takeIf(String::isNotBlank) ?: return@mapNotNull null
                val dueDay = card.dueDay.coerceIn(1, 31)
                PaymentCard(id = card.id.take(48), name = name, dueDay = dueDay)
            }
            .distinctBy(PaymentCard::id)
            .take(MAX_PAYMENT_CARDS)
        context.settingsDataStore.edit { preferences ->
            preferences[PAYMENT_CARDS] = normalized.joinToString("\n") { card -> "${card.id}|${card.name}|${card.dueDay}" }
        }
    }

    suspend fun saveCategoryBudgets(budgets: Map<String, Long>) {
        val normalized = budgets.asSequence()
            .filter { (key, amount) -> isSupportedCategoryKey(key) && amount in 1..1_000_000_000_000L }
            .map { (key, amount) -> key to amount }
            .take(MAX_CATEGORY_BUDGETS)
            .toMap()
        context.settingsDataStore.edit { preferences ->
            preferences[CATEGORY_BUDGETS] = normalized.entries.joinToString("\n") { (key, amount) -> "$key=$amount" }
        }
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
        val NOTIFICATION_SERVICE_CONNECTED_AT = longPreferencesKey("notification_service_connected_at")
        val NOTIFICATION_LAST_SEEN_AT = longPreferencesKey("notification_last_seen_at")
        val NOTIFICATION_LAST_CANDIDATE_AT = longPreferencesKey("notification_last_candidate_at")
        val NOTIFICATION_SERVICE_DISCONNECTED_AT = longPreferencesKey("notification_service_disconnected_at")
        val PAYMENT_METHODS = stringPreferencesKey("payment_methods")
        val PAYMENT_CARDS = stringPreferencesKey("payment_cards")
        val CATEGORY_BUDGETS = stringPreferencesKey("category_budgets")
        val CATEGORY_LABELS = stringPreferencesKey("category_labels")
        val CUSTOM_CATEGORY_KEY = Regex("CUSTOM_[A-F0-9]{12}")
        const val MAX_CUSTOM_CATEGORIES = 20
        const val MAX_PAYMENT_CARDS = 12
        const val MAX_CATEGORY_BUDGETS = 27
    }
}
