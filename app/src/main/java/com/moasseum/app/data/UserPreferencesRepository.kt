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
import com.moasseum.app.domain.AiAdviceCards
import com.moasseum.app.domain.NoSpendChallengeSettings
import com.moasseum.app.domain.DisplayCurrencySetting
import com.moasseum.app.domain.DisplayCurrencies
import com.moasseum.app.domain.ExchangeRateQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.time.YearMonth
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

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

data class LocalSharedAnniversary(val id: String, val title: String, val dateKey: String)

class UserPreferencesRepository(
    private val context: Context,
) {
    suspend fun backupSettings(): BackupSettings {
        val challenge = noSpendChallenge.first()
        return BackupSettings(
            isDarkTheme.first(), reduceMotion.first(), paymentMethods.first(), paymentCards.first(),
            categoryLabels.first(), categoryBudgets.first(), categoryOrder.first(), monthlyIncomeTargets.first(),
            homeDashboardCards.first(), challenge.goalDays, challenge.startDate,
            yearEndGrossSalaries.first(), aiAdviceCards.first(), budgetPayday.first(), displayCurrencySetting.first().code,
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
            preferences[AI_ADVICE_CARDS] = settings.aiAdviceCards.filter { it in AiAdviceCards.defaults }.sorted().joinToString("\n")
            preferences[BUDGET_PAYDAY] = settings.budgetPayday.takeIf { it in 1..31 } ?: 1
            preferences[DISPLAY_CURRENCY] = settings.displayCurrencyCode.takeIf(DisplayCurrencies::isSupported) ?: DisplayCurrencies.KRW
            preferences.remove(DISPLAY_CURRENCY_QUOTE)
            preferences[NO_SPEND_CHALLENGE_GOAL] = settings.noSpendChallengeGoalDays.takeIf { it in SUPPORTED_CHALLENGE_GOALS } ?: 7
            settings.noSpendChallengeStartDate?.let { preferences[NO_SPEND_CHALLENGE_START] = it.toEpochDay() }
                ?: preferences.remove(NO_SPEND_CHALLENGE_START)
            preferences[YEAR_END_GROSS_SALARIES] = settings.yearEndGrossSalaries.entries
                .filter { (year, salary) -> year in 1900..9999 && salary in 1..100_000_000_000L }
                .sortedBy { it.key }.takeLast(10).joinToString("\n") { "${it.key}=${it.value}" }
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

    val profileDisplayName: Flow<String> =
        context.settingsDataStore.data.map { preferences -> preferences[PROFILE_DISPLAY_NAME].orEmpty() }

    val profilePhotoUri: Flow<String?> =
        context.settingsDataStore.data.map { preferences -> preferences[PROFILE_PHOTO_URI] }

    val readActivityNoticeIds: Flow<Set<String>> =
        context.settingsDataStore.data.map { preferences ->
            preferences[READ_ACTIVITY_NOTICE_IDS].orEmpty().lineSequence()
                .filter { it.isNotBlank() && it.length <= 120 }
                .toList()
                .takeLast(MAX_READ_ACTIVITY_NOTICES)
                .toSet()
        }

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

    val aiAdviceCards: Flow<Set<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[AI_ADVICE_CARDS]?.lineSequence()
            ?.filter { it in AiAdviceCards.defaults }
            ?.toSet()
            ?: AiAdviceCards.defaults
    }

    val budgetPayday: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[BUDGET_PAYDAY]?.takeIf { it in 1..31 } ?: 1
    }

    val displayCurrencySetting: Flow<DisplayCurrencySetting> = context.settingsDataStore.data.map { preferences ->
        val code = preferences[DISPLAY_CURRENCY]?.takeIf(DisplayCurrencies::isSupported) ?: DisplayCurrencies.KRW
        val parts = preferences[DISPLAY_CURRENCY_QUOTE].orEmpty().split('|')
        val quote = if (parts.size == 3 && parts[0] == code && code != DisplayCurrencies.KRW) {
            val rate = parts[1].toBigDecimalOrNull()?.takeIf { it.signum() > 0 && it.precision() <= 24 && it.scale() <= 12 }
            val date = parts[2].takeIf { runCatching { LocalDate.parse(it) }.isSuccess }
            if (rate != null && date != null) rate.stripTrailingZeros().toPlainString() to date else null
        } else null
        DisplayCurrencySetting(code, quote?.first, quote?.second)
    }

    suspend fun saveDisplayCurrency(code: String) {
        require(DisplayCurrencies.isSupported(code)) { "지원하지 않는 표시 통화예요." }
        context.settingsDataStore.edit { preferences ->
            if (preferences[DISPLAY_CURRENCY] != code) preferences.remove(DISPLAY_CURRENCY_QUOTE)
            preferences[DISPLAY_CURRENCY] = code
        }
    }

    suspend fun saveDisplayCurrencyQuote(quote: ExchangeRateQuote) {
        require(quote.baseCurrency in DisplayCurrencies.foreignCodes && quote.quoteCurrency == "KRW")
        val rate = quote.rate.toBigDecimalOrNull()?.takeIf { it.signum() > 0 && it.precision() <= 24 && it.scale() <= 12 }
            ?: throw IllegalArgumentException("환율 정보를 확인하지 못했어요.")
        val date = runCatching { LocalDate.parse(quote.date) }.getOrNull()
            ?: throw IllegalArgumentException("환율 날짜를 확인하지 못했어요.")
        context.settingsDataStore.edit { preferences ->
            if (preferences[DISPLAY_CURRENCY] == quote.baseCurrency) {
                preferences[DISPLAY_CURRENCY_QUOTE] = "${quote.baseCurrency}|${rate.stripTrailingZeros().toPlainString()}|$date"
            }
        }
    }

    val noSpendChallenge: Flow<NoSpendChallengeSettings> = context.settingsDataStore.data.map { preferences ->
        NoSpendChallengeSettings(
            startDate = preferences[NO_SPEND_CHALLENGE_START]?.let { epochDay ->
                runCatching { LocalDate.ofEpochDay(epochDay) }.getOrNull()
            },
            goalDays = preferences[NO_SPEND_CHALLENGE_GOAL]?.takeIf { it in SUPPORTED_CHALLENGE_GOALS } ?: 7,
        )
    }

    val yearEndGrossSalaries: Flow<Map<Int, Long>> = context.settingsDataStore.data.map { preferences ->
        preferences[YEAR_END_GROSS_SALARIES].orEmpty().lineSequence()
            .mapNotNull { line ->
                val year = line.substringBefore('=', "").toIntOrNull()
                val salary = line.substringAfter('=', "").toLongOrNull()
                if (year != null && year in 1900..9999 && salary != null && salary in 1..100_000_000_000L) year to salary else null
            }.toMap()
    }

    val sharedAnniversaries: Flow<List<LocalSharedAnniversary>> = context.settingsDataStore.data.map { preferences ->
        runCatching {
            val rows = JSONArray(preferences[SHARED_ANNIVERSARIES].orEmpty())
            (0 until rows.length()).mapNotNull { index ->
                val row = rows.optJSONObject(index) ?: return@mapNotNull null
                val id = row.optString("id").takeIf { it.isNotBlank() && it.length <= 120 } ?: return@mapNotNull null
                val title = row.optString("title").trim().takeIf { it.isNotBlank() && it.length <= 80 } ?: return@mapNotNull null
                val dateKey = row.optString("dateKey").takeIf { value ->
                    runCatching { java.time.MonthDay.parse("--$value") }.isSuccess
                } ?: return@mapNotNull null
                LocalSharedAnniversary(id, title, dateKey)
            }.take(100)
        }.getOrDefault(emptyList())
    }

    val hiddenQuickEntrySuggestions: Flow<Set<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[HIDDEN_QUICK_ENTRY_SUGGESTIONS].orEmpty().lineSequence()
            .filter { it.matches(Regex("[a-f0-9]{64}")) }
            .toList()
            .takeLast(MAX_HIDDEN_QUICK_ENTRY_SUGGESTIONS)
            .toSet()
    }

    suspend fun hideQuickEntrySuggestion(key: String) {
        require(key.matches(Regex("[a-f0-9]{64}")))
        context.settingsDataStore.edit { preferences ->
            val hidden = preferences[HIDDEN_QUICK_ENTRY_SUGGESTIONS].orEmpty().lineSequence()
                .filter { it.matches(Regex("[a-f0-9]{64}")) }
                .toMutableList()
            if (key !in hidden) hidden += key
            preferences[HIDDEN_QUICK_ENTRY_SUGGESTIONS] = hidden.takeLast(MAX_HIDDEN_QUICK_ENTRY_SUGGESTIONS).joinToString("\n")
        }
    }

    suspend fun saveHomeDashboardCards(cards: Set<String>) {
        val normalized = cards.filter { it in HomeDashboardCards.defaults }.toSet()
        require(normalized.size == cards.size) { "홈 카드 설정이 올바르지 않아요." }
        context.settingsDataStore.edit { preferences ->
            preferences[HOME_DASHBOARD_CARDS] = normalized.sorted().joinToString("\n")
        }
    }

    suspend fun saveAiAdviceCards(cards: Set<String>) {
        require(cards.all { it in AiAdviceCards.defaults }) { "AI 카드 설정이 올바르지 않아요." }
        context.settingsDataStore.edit { preferences ->
            preferences[AI_ADVICE_CARDS] = cards.sorted().joinToString("\n")
        }
    }

    suspend fun saveBudgetPayday(day: Int) {
        require(day in 1..31) { "급여일은 1일부터 31일까지예요." }
        context.settingsDataStore.edit { preferences -> preferences[BUDGET_PAYDAY] = day }
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

    suspend fun saveYearEndGrossSalary(year: Int, salary: Long?) {
        require(year in 1900..9999)
        require(salary == null || salary in 1..100_000_000_000L)
        val updated = yearEndGrossSalaries.first().toMutableMap().apply {
            if (salary == null) remove(year) else put(year, salary)
        }.toSortedMap().toList().takeLast(10).toMap()
        context.settingsDataStore.edit { preferences ->
            preferences[YEAR_END_GROSS_SALARIES] = updated.entries.joinToString("\n") { "${it.key}=${it.value}" }
        }
    }

    suspend fun saveSharedAnniversaries(items: List<SharedFinanceItem>) {
        val json = JSONArray().apply {
            items.asSequence().filter { it.kind == SharedFinanceKind.ANNIVERSARY }.take(100).forEach { item ->
                put(JSONObject().put("id", item.id.take(120)).put("title", item.title.trim().take(80)).put("dateKey", item.dateKey.orEmpty()))
            }
        }
        context.settingsDataStore.edit { preferences -> preferences[SHARED_ANNIVERSARIES] = json.toString() }
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

    suspend fun saveProfileDisplayName(name: String) {
        val normalized = name.trim()
        require(normalized.length <= 24) { "이름은 24자 이내로 입력해 주세요." }
        context.settingsDataStore.edit { preferences ->
            if (normalized.isBlank()) preferences.remove(PROFILE_DISPLAY_NAME)
            else preferences[PROFILE_DISPLAY_NAME] = normalized
        }
    }

    suspend fun saveProfilePhotoUri(uri: String?) {
        val normalized = uri?.trim()?.takeIf(String::isNotEmpty)
        require(normalized == null || (normalized.length <= 2048 && normalized.startsWith("content://"))) {
            "기기에서 선택한 사진만 프로필로 사용할 수 있어요."
        }
        context.settingsDataStore.edit { preferences ->
            if (normalized == null) preferences.remove(PROFILE_PHOTO_URI)
            else preferences[PROFILE_PHOTO_URI] = normalized
        }
    }

    suspend fun markActivityNoticesRead(ids: Set<String>) {
        val normalized = ids.filter { it.length in 1..120 && it.all { ch -> ch.isLetterOrDigit() || ch in ":-_ ." } }
        if (normalized.isEmpty()) return
        context.settingsDataStore.edit { preferences ->
            val previous = preferences[READ_ACTIVITY_NOTICE_IDS].orEmpty().lineSequence()
                .filter(String::isNotBlank).toList()
            preferences[READ_ACTIVITY_NOTICE_IDS] = (previous + normalized).distinct()
                .takeLast(MAX_READ_ACTIVITY_NOTICES).joinToString("\n")
        }
    }

    suspend fun shouldShowReleaseNotes(currentVersion: String, isUpgradeInstall: Boolean): Boolean {
        require(currentVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        val lastSeenVersion = context.settingsDataStore.data.first()[LAST_SEEN_RELEASE_NOTES_VERSION]
        return com.moasseum.app.update.ReleaseNotesPolicy.shouldShow(
            currentVersion = currentVersion,
            lastSeenVersion = lastSeenVersion,
            isUpgradeInstall = isUpgradeInstall,
        )
    }

    suspend fun markReleaseNotesSeen(version: String) {
        require(version.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        context.settingsDataStore.edit { preferences -> preferences[LAST_SEEN_RELEASE_NOTES_VERSION] = version }
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
        val HIDDEN_QUICK_ENTRY_SUGGESTIONS = stringPreferencesKey("hidden_quick_entry_suggestions")
        const val MAX_HIDDEN_QUICK_ENTRY_SUGGESTIONS = 500
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val NOTIFICATION_ACCESS_PROMPT_SHOWN = booleanPreferencesKey("notification_access_prompt_shown")
        val NOTIFICATION_POST_PERMISSION_PROMPT_SHOWN = booleanPreferencesKey("notification_post_permission_prompt_shown")
        val FIRST_RUN_GUIDE_COMPLETED = booleanPreferencesKey("first_run_guide_completed")
        val PROFILE_DISPLAY_NAME = stringPreferencesKey("profile_display_name")
        val PROFILE_PHOTO_URI = stringPreferencesKey("profile_photo_uri")
        val READ_ACTIVITY_NOTICE_IDS = stringPreferencesKey("read_activity_notice_ids")
        val LAST_SEEN_RELEASE_NOTES_VERSION = stringPreferencesKey("last_seen_release_notes_version")
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
        val AI_ADVICE_CARDS = stringPreferencesKey("ai_advice_cards")
        val BUDGET_PAYDAY = intPreferencesKey("budget_payday")
        val DISPLAY_CURRENCY = stringPreferencesKey("display_currency")
        val DISPLAY_CURRENCY_QUOTE = stringPreferencesKey("display_currency_quote")
        val NO_SPEND_CHALLENGE_START = longPreferencesKey("no_spend_challenge_start")
        val NO_SPEND_CHALLENGE_GOAL = intPreferencesKey("no_spend_challenge_goal")
        val YEAR_END_GROSS_SALARIES = stringPreferencesKey("year_end_gross_salaries")
        val SHARED_ANNIVERSARIES = stringPreferencesKey("shared_anniversaries_for_local_reminders")
        val CUSTOM_CATEGORY_KEY = Regex("CUSTOM_[A-F0-9]{12}")
        const val MAX_CUSTOM_CATEGORIES = 20
        const val MAX_CATEGORY_BUDGETS = 39
        const val MAX_INCOME_TARGET_MONTHS = 120
        const val MAX_READ_ACTIVITY_NOTICES = 200
        val SUPPORTED_CHALLENGE_GOALS = setOf(7, 14, 30)
    }
}
