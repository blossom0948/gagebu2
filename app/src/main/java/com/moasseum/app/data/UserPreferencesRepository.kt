package com.moasseum.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moasseum.app.domain.PaymentCard
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.NoSpendChallengeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.time.YearMonth
import java.time.LocalDate

val DEFAULT_PAYMENT_METHODS = listOf("카드", "현금", "이체", "기타")
val DEFAULT_CATEGORY_LABELS = linkedMapOf(
    "FOOD" to "식비",
    "CAFE" to "카페",
    "TRANSPORT" to "교통",
    "SHOPPING" to "쇼핑",
    "HOUSING" to "주거",
    "COMMUNICATION" to "통신",
    "HEALTH" to "의료",
    "EDUCATION" to "교육",
    "CULTURE" to "문화",
    "LEISURE" to "여가",
    "TRAVEL" to "여행",
    "GIFT" to "경조사",
    "BEAUTY" to "미용",
    "PET" to "반려동물",
    "SUBSCRIPTION" to "구독",
    "INSURANCE" to "보험",
    "FINANCE" to "금융",
    "LIVING" to "생활",
    "OTHER" to "기타",
)
val DEFAULT_CATEGORY_ORDER = DEFAULT_CATEGORY_LABELS.keys.toList()

private val Context.settingsDataStore by preferencesDataStore(name = "moasseum_settings")

class UserPreferencesRepository(
    private val context: Context,
) {
    suspend fun backupSettings(): BackupSettings {
        val challenge = noSpendChallenge.first()
        return BackupSettings(
            isDarkTheme.first(), reduceMotion.first(), paymentMethods.first(), paymentCards.first(),
            categoryLabels.first(), categoryBudgets.first(), categoryOrder.first(), monthlyIncomeTargets.first(),
            homeDashboardCards.first(), challenge.goalDays, challenge.startDate,
        )
    }

    suspend fun restoreSettings(settings: BackupSettings) {
        // One DataStore edit keeps presentation settings consistent. Consent and permissions are never restored.
        val cards = PaymentCardSettings.encode(settings.paymentCards)
        context.settingsDataStore.edit { preferences ->
            preferences[DARK_THEME] = settings.darkTheme
            preferences[REDUCE_MOTION] = settings.reduceMotion
            preferences[PAYMENT_METHODS] = settings.paymentMethods.joinToString("\n")
            preferences[PAYMENT_CARDS] = cards
            preferences[CATEGORY_LABELS] = settings.categoryLabels.entries.joinToString("\n") { "${it.key}=${it.value}" }
            preferences[CATEGORY_BUDGETS] = settings.categoryBudgets.entries.joinToString("\n") { "${it.key}=${it.value}" }
            preferences[CATEGORY_ORDER] = (settings.categoryOrder.ifEmpty { settings.categoryLabels.keys.toList() })
                .filter { it in settings.categoryLabels }.distinct().joinToString("\n")
            preferences[MONTHLY_INCOME_TARGETS] = settings.monthlyIncomeTargets.entries
                .sortedBy { it.key }.takeLast(MAX_INCOME_TARGET_MONTHS).joinToString("\n") { "${it.key}=${it.value}" }
            preferences[HOME_DASHBOARD_CARDS] = settings.homeDashboardCards.filter { it in HomeDashboardCards.defaults }.sorted().joinToString("\n")
            preferences[NO_SPEND_CHALLENGE_GOAL] = settings.noSpendChallengeGoalDays.takeIf { it in SUPPORTED_CHALLENGE_GOALS } ?: 7
            settings.noSpendChallengeStartDate?.let { preferences[NO_SPEND_CHALLENGE_START] = it.toEpochDay() }
                ?: preferences.remove(NO_SPEND_CHALLENGE_START)
        }
    }
    val isDarkTheme: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[DARK_THEME] ?: true }

    val reduceMotion: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[REDUCE_MOTION] ?: false }

    val notificationAccessPromptShown: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_ACCESS_PROMPT_SHOWN] ?: false }

    val notificationPostPermissionPromptShown: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[NOTIFICATION_POST_PERMISSION_PROMPT_SHOWN] ?: false }

    val firstRunGuideCompleted: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[FIRST_RUN_GUIDE_COMPLETED] ?: false }

    val aiNotificationClassificationEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[AI_NOTIFICATION_CLASSIFICATION_ENABLED] ?: false }

    val appLockEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[APP_LOCK_ENABLED] ?: false }

    val financeRemindersEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[FINANCE_REMINDERS_ENABLED] ?: true }

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
            ?.take(36)
            ?.takeIf(List<String>::isNotEmpty)
            ?: DEFAULT_PAYMENT_METHODS
    }

    val paymentCards: Flow<List<PaymentCard>> = context.settingsDataStore.data.map { preferences ->
        PaymentCardSettings.decode(preferences[PAYMENT_CARDS].orEmpty())
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
        val merged = (DEFAULT_CATEGORY_LABELS + stored).toMutableMap()
        val storedLabels = stored.values.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
        DEFAULT_CATEGORY_LABELS.forEach { (key, label) ->
            if (key !in stored && label.lowercase(Locale.ROOT) in storedLabels) {
                var uniqueLabel = "$label (기본)".take(16)
                var suffix = 2
                while (merged.any { (otherKey, otherLabel) -> otherKey != key && otherLabel.equals(uniqueLabel, ignoreCase = true) }) {
                    uniqueLabel = "$label (${suffix++})".take(16)
                }
                merged[key] = uniqueLabel
            }
        }
        merged
    }

    val categoryOrder: Flow<List<String>> = context.settingsDataStore.data.map { preferences ->
        val customKeys = preferences[CATEGORY_LABELS].orEmpty().lineSequence()
            .map { it.substringBefore('=', "") }
            .filter { it.matches(CUSTOM_CATEGORY_KEY) }
            .toList()
        val validKeys = DEFAULT_CATEGORY_ORDER + customKeys
        val stored = preferences[CATEGORY_ORDER].orEmpty().lineSequence()
            .filter { it in validKeys }
            .distinct()
            .toList()
        stored + validKeys.filterNot { it in stored }
    }

    val monthlyIncomeTargets: Flow<Map<String, Long>> = context.settingsDataStore.data.map { preferences ->
        preferences[MONTHLY_INCOME_TARGETS].orEmpty().lineSequence()
            .mapNotNull { line ->
                val month = line.substringBefore('=', "")
                val amount = line.substringAfter('=', "").toLongOrNull()
                if (runCatching { YearMonth.parse(month) }.isSuccess && amount != null && amount in 1..1_000_000_000_000L) month to amount else null
            }
            .toMap()
            .toSortedMap()
            .toList().takeLast(MAX_INCOME_TARGET_MONTHS)
            .toMap()
    }

    val homeDashboardCards: Flow<Set<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[HOME_DASHBOARD_CARDS]?.lineSequence()
            ?.filter { it in HomeDashboardCards.defaults }
            ?.toSet()
            ?: HomeDashboardCards.defaults
    }

    val noSpendChallenge: Flow<NoSpendChallengeSettings> = context.settingsDataStore.data.map { preferences ->
        NoSpendChallengeSettings(
            startDate = preferences[NO_SPEND_CHALLENGE_START]?.let { epochDay ->
                runCatching { LocalDate.ofEpochDay(epochDay) }.getOrNull()
            },
            goalDays = preferences[NO_SPEND_CHALLENGE_GOAL]?.takeIf { it in SUPPORTED_CHALLENGE_GOALS } ?: 7,
        )
    }

    suspend fun saveHomeDashboardCards(cards: Set<String>) {
        val normalized = cards.filter { it in HomeDashboardCards.defaults }.toSet()
        require(normalized.size == cards.size) { "홈 카드 설정이 올바르지 않아요." }
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_DASHBOARD_CARDS] = normalized.sorted().joinToString("\n")
        }
    }

    suspend fun configureNoSpendChallenge(enabled: Boolean, goalDays: Int) {
        require(goalDays in SUPPORTED_CHALLENGE_GOALS) { "챌린지 기간은 7일, 14일, 30일 중에서 선택해 주세요." }
        context.settingsDataStore.edit { preferences ->
            preferences[NO_SPEND_CHALLENGE_GOAL] = goalDays
            if (enabled) {
                if (preferences[NO_SPEND_CHALLENGE_START] == null) {
                    preferences[NO_SPEND_CHALLENGE_START] = LocalDate.now().toEpochDay()
                }
            } else {
                preferences.remove(NO_SPEND_CHALLENGE_START)
            }
        }
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

    suspend fun setFirstRunGuideCompleted() {
        context.settingsDataStore.edit { preferences -> preferences[FIRST_RUN_GUIDE_COMPLETED] = true }
    }

    suspend fun setAiNotificationClassificationEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[AI_NOTIFICATION_CLASSIFICATION_ENABLED] = enabled }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setFinanceRemindersEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences -> preferences[FINANCE_REMINDERS_ENABLED] = enabled }
    }

    suspend fun claimFinanceReminder(key: String): Boolean {
        if (key.isBlank() || key.length > 160) return false
        var claimed = false
        context.settingsDataStore.edit { preferences ->
            val sent = preferences[FINANCE_REMINDERS].orEmpty().lineSequence().filter(String::isNotBlank).toMutableList()
            if (key !in sent) {
                sent += key
                preferences[FINANCE_REMINDERS] = sent.takeLast(200).joinToString("\n")
                claimed = true
            }
        }
        return claimed
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
        val normalized = methods.map(String::trim).filter(String::isNotBlank).distinct().take(36)
        require(normalized.isNotEmpty()) { "결제수단은 한 개 이상 남겨야 해요." }
        context.settingsDataStore.edit { preferences -> preferences[PAYMENT_METHODS] = normalized.joinToString("\n") }
    }

    suspend fun savePaymentCards(cards: List<PaymentCard>) {
        val encoded = PaymentCardSettings.encode(cards.distinctBy(PaymentCard::id))
        context.settingsDataStore.edit { preferences ->
            preferences[PAYMENT_CARDS] = encoded
            val methods = preferences[PAYMENT_METHODS]?.split('\n') ?: DEFAULT_PAYMENT_METHODS
            preferences[PAYMENT_METHODS] = (methods + cards.map { it.paymentMethod }).map(String::trim).filter(String::isNotBlank).distinct().take(36).joinToString("\n")
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
            preferences[CATEGORY_BUDGETS] = preferences[CATEGORY_BUDGETS].orEmpty().lineSequence()
                .filter { it.substringBefore('=') in normalized }.joinToString("\n")
            val oldOrder = preferences[CATEGORY_ORDER].orEmpty().lineSequence().filter { it in normalized }.distinct().toList()
            preferences[CATEGORY_ORDER] = (oldOrder + normalized.keys.filterNot { it in oldOrder }).joinToString("\n")
        }
    }

    suspend fun saveCategoryOrder(order: List<String>) {
        val validKeys = categoryLabels.first().keys
        val normalized = order.distinct()
        require(normalized.size == order.size && normalized.toSet() == validKeys) {
            "카테고리 순서를 저장하지 못했어요. 화면을 새로고침한 뒤 다시 시도해 주세요."
        }
        context.settingsDataStore.edit { preferences -> preferences[CATEGORY_ORDER] = normalized.joinToString("\n") }
    }

    suspend fun saveMonthlyIncomeTarget(monthKey: String, amount: Long?) {
        YearMonth.parse(monthKey)
        require(amount == null || amount in 1..1_000_000_000_000L) { "월 수입 목표는 비우거나 1원 이상 입력해 주세요." }
        val updated = monthlyIncomeTargets.first().toMutableMap().apply {
            if (amount == null) remove(monthKey) else put(monthKey, amount)
        }.toSortedMap().toList().takeLast(MAX_INCOME_TARGET_MONTHS).toMap()
        context.settingsDataStore.edit { preferences ->
            preferences[MONTHLY_INCOME_TARGETS] = updated.entries.joinToString("\n") { "${it.key}=${it.value}" }
        }
    }

    private fun isSupportedCategoryKey(key: String): Boolean =
        key in DEFAULT_CATEGORY_LABELS || key.matches(CUSTOM_CATEGORY_KEY)

    private companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val NOTIFICATION_ACCESS_PROMPT_SHOWN = booleanPreferencesKey("notification_access_prompt_shown")
        val NOTIFICATION_POST_PERMISSION_PROMPT_SHOWN = booleanPreferencesKey("notification_post_permission_prompt_shown")
        val FIRST_RUN_GUIDE_COMPLETED = booleanPreferencesKey("first_run_guide_completed")
        val AI_NOTIFICATION_CLASSIFICATION_ENABLED = booleanPreferencesKey("ai_notification_classification_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val FINANCE_REMINDERS_ENABLED = booleanPreferencesKey("finance_reminders_enabled")
        val FINANCE_REMINDERS = stringPreferencesKey("finance_reminders_sent")
        val NOTIFICATION_SERVICE_CONNECTED_AT = longPreferencesKey("notification_service_connected_at")
        val NOTIFICATION_LAST_SEEN_AT = longPreferencesKey("notification_last_seen_at")
        val NOTIFICATION_LAST_CANDIDATE_AT = longPreferencesKey("notification_last_candidate_at")
        val NOTIFICATION_SERVICE_DISCONNECTED_AT = longPreferencesKey("notification_service_disconnected_at")
        val PAYMENT_METHODS = stringPreferencesKey("payment_methods")
        val PAYMENT_CARDS = stringPreferencesKey("payment_cards")
        val CATEGORY_BUDGETS = stringPreferencesKey("category_budgets")
        val CATEGORY_LABELS = stringPreferencesKey("category_labels")
        val CATEGORY_ORDER = stringPreferencesKey("category_order")
        val MONTHLY_INCOME_TARGETS = stringPreferencesKey("monthly_income_targets")
        val HOME_DASHBOARD_CARDS = stringPreferencesKey("home_dashboard_cards")
        val NO_SPEND_CHALLENGE_START = longPreferencesKey("no_spend_challenge_start")
        val NO_SPEND_CHALLENGE_GOAL = intPreferencesKey("no_spend_challenge_goal")
        val CUSTOM_CATEGORY_KEY = Regex("CUSTOM_[A-F0-9]{12}")
        const val MAX_CUSTOM_CATEGORIES = 20
        const val MAX_CATEGORY_BUDGETS = 39
        const val MAX_INCOME_TARGET_MONTHS = 120
        val SUPPORTED_CHALLENGE_GOALS = setOf(7, 14, 30)
    }
}
