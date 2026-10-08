package com.moasseum.app.data

import com.moasseum.app.data.local.*
import com.moasseum.app.domain.PaymentCard
import com.moasseum.app.domain.HomeDashboardCards
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.validateAccount
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class BackupSettings(
    val darkTheme: Boolean,
    val reduceMotion: Boolean,
    val paymentMethods: List<String>,
    val paymentCards: List<PaymentCard>,
    val categoryLabels: Map<String, String>,
    val categoryBudgets: Map<String, Long>,
    val categoryOrder: List<String> = categoryLabels.keys.toList(),
    val monthlyIncomeTargets: Map<String, Long> = emptyMap(),
    val homeDashboardCards: Set<String> = HomeDashboardCards.defaults,
    val noSpendChallengeGoalDays: Int = 7,
    val noSpendChallengeStartDate: LocalDate? = null,
)

data class FullBackup(
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val recurringRules: List<RecurringTransactionEntity>,
    val accounts: List<AccountEntity>,
    val settings: BackupSettings? = null,
    val legacy: Boolean = false,
)

object FullBackupCodec {
    const val MAX_BYTES = 10_000_000
    private const val MAX_ROWS = 50_000
    fun read(input: InputStream): String = input.use {
        val bytes = it.readBytesLimited()
        bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
    }
    private fun InputStream.readBytesLimited(): ByteArray {
        val result = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(result.size() + count <= MAX_BYTES) { "백업 파일은 10MB 이하만 가져올 수 있어요." }
            result.write(buffer, 0, count)
        }
        return result.toByteArray()
    }
    fun encode(backup: FullBackup): String = JSONObject().apply {
        put("app", "모아씀"); put("schemaVersion", 2); put("createdAt", System.currentTimeMillis())
        put("transactions", array(backup.transactions) { t -> JSONObject().apply {
            put("id", t.id); put("type", t.type); put("amount", t.amount); put("occurredAt", t.occurredAt)
            put("timezone", t.timezone); put("categoryKey", t.categoryKey); put("merchant", t.merchant)
            put("memo", t.memo); put("paymentMethod", t.paymentMethod); put("source", t.source)
            put("createdAt", t.createdAt); put("updatedAt", t.updatedAt); put("deletedAt", t.deletedAt ?: JSONObject.NULL)
            put("accountId", t.accountId ?: JSONObject.NULL); put("destinationAccountId", t.destinationAccountId ?: JSONObject.NULL)
            put("installmentGroupId", t.installmentGroupId ?: JSONObject.NULL)
            put("installmentNumber", t.installmentNumber ?: JSONObject.NULL)
            put("installmentCount", t.installmentCount ?: JSONObject.NULL)
        } })
        put("accounts", array(backup.accounts) { a -> JSONObject().apply {
            put("id", a.id); put("name", a.name); put("openingBalance", a.openingBalance); put("archived", a.archived); put("updatedAt", a.updatedAt)
        } })
        put("budgets", array(backup.budgets) { b -> JSONObject().apply {
            put("monthKey", b.monthKey); put("amount", b.amount); put("rollover", b.rollover); put("updatedAt", b.updatedAt)
        } })
        put("recurringRules", array(backup.recurringRules) { r -> JSONObject().apply {
            put("id", r.id); put("type", r.type); put("amount", r.amount); put("merchant", r.merchant); put("dayOfMonth", r.dayOfMonth)
            put("nextOccurrenceDate", r.nextOccurrenceDate); put("categoryKey", r.categoryKey); put("memo", r.memo); put("paymentMethod", r.paymentMethod)
            put("isActive", r.isActive); put("createdAt", r.createdAt); put("updatedAt", r.updatedAt)
        } })
        backup.settings?.let { s -> put("settings", JSONObject().apply {
            put("darkTheme", s.darkTheme); put("reduceMotion", s.reduceMotion); put("paymentMethods", JSONArray(s.paymentMethods))
            put("paymentCards", PaymentCardSettings.encode(s.paymentCards)); put("categoryLabels", JSONObject(s.categoryLabels)); put("categoryBudgets", JSONObject(s.categoryBudgets))
            put("categoryOrder", JSONArray(s.categoryOrder))
            put("monthlyIncomeTargets", JSONObject(s.monthlyIncomeTargets))
            put("homeDashboardCards", JSONArray(s.homeDashboardCards.sorted()))
            put("noSpendChallengeGoalDays", s.noSpendChallengeGoalDays)
            put("noSpendChallengeStartDate", s.noSpendChallengeStartDate?.toString() ?: JSONObject.NULL)
        }) }
    }.toString(2)

    fun decode(text: String): FullBackup {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JSONObject(text)
        require(root.getString("app") == "모아씀") { "모아씀 백업 파일이 아니에요." }
        val version = root.getInt("schemaVersion")
        require(version in 1..2) { "지원하지 않는 백업 버전이에요. 앱을 업데이트해 주세요." }
        val now = System.currentTimeMillis()
        val accounts = if (version == 1) emptyList() else objects(root, "accounts").map { a ->
            AccountEntity(a.getString("id").also { java.util.UUID.fromString(it) }, field(a, "name", 30), a.getLong("openingBalance"), a.getBoolean("archived"), a.getLong("updatedAt"))
                .also { validateAccount(it.name, it.openingBalance) }
        }
        require(accounts.map { it.id }.distinct().size == accounts.size) { "중복 계좌가 있는 백업이에요." }
        val accountIds = accounts.map { it.id }.toSet()
        val transactions = objects(root, "transactions").map { t ->
            val type = TransactionType.valueOf(t.getString("type"))
            val from = t.nullableString("accountId"); val to = t.nullableString("destinationAccountId")
            require(from == null || from in accountIds); require(to == null || to in accountIds)
            if (type == TransactionType.TRANSFER) require(from != null && to != null && from != to) { "이체의 계좌 연결이 올바르지 않아요." }
            else require(to == null)
            val timestamp = t.getLong("occurredAt")
            val zone = t.optString("timezone", ZoneId.systemDefault().id).also { ZoneId.of(it) }
            require(timestamp in 0..32_503_680_000_000L)
            val installmentGroupId = t.nullableString("installmentGroupId")?.also { java.util.UUID.fromString(it) }
            val installmentNumber = t.optInt("installmentNumber", -1).takeIf { it > 0 }
            val installmentCount = t.optInt("installmentCount", -1).takeIf { it > 0 }
            require((installmentGroupId == null && installmentNumber == null && installmentCount == null) ||
                (type == TransactionType.EXPENSE && installmentGroupId != null && installmentNumber != null && installmentCount != null && installmentCount in 2..60 && installmentNumber <= installmentCount)) {
                "할부 연결 정보가 올바르지 않은 백업이에요."
            }
            TransactionEntity(id = t.getLong("id").also { require(it > 0) }, type = type.name, amount = amount(t), occurredAt = timestamp,
                timezone = zone, categoryKey = field(t, "categoryKey", 64), merchant = field(t, "merchant", 300), memo = field(t, "memo", 4000, true),
                paymentMethod = field(t, "paymentMethod", 100), source = field(t, "source", 40), createdAt = t.optLong("createdAt", now),
                updatedAt = t.optLong("updatedAt", now), deletedAt = if (t.isNull("deletedAt") || !t.has("deletedAt")) null else t.getLong("deletedAt"), accountId = from, destinationAccountId = to,
                installmentGroupId = installmentGroupId, installmentNumber = installmentNumber, installmentCount = installmentCount)
        }
        require(transactions.map { it.id }.distinct().size == transactions.size) { "중복 거래 ID가 있어요." }
        require(transactions.filter { it.installmentGroupId != null }.groupBy { it.installmentGroupId }.values.all { rows ->
            val count = rows.first().installmentCount
            rows.size == count && rows.mapNotNull { it.installmentNumber }.toSet() == (1..requireNotNull(count)).toSet() && rows.all { it.installmentCount == count }
        }) { "할부 회차가 빠졌거나 중복된 백업이에요." }
        val budgets = if (version == 1) emptyList() else objects(root, "budgets").map { b ->
            BudgetEntity(b.getString("monthKey").also { YearMonth.parse(it) }, amount(b), b.getBoolean("rollover"), b.getLong("updatedAt"))
        }
        require(budgets.map { it.monthKey }.distinct().size == budgets.size)
        val rules = if (version == 1) emptyList() else objects(root, "recurringRules").map { r ->
            val type = TransactionType.valueOf(r.getString("type")); require(type != TransactionType.TRANSFER)
            RecurringTransactionEntity(r.getLong("id").also { require(it > 0) }, type.name, amount(r), field(r, "merchant", 300),
                r.getInt("dayOfMonth").also { require(it in 1..31) }, r.getString("nextOccurrenceDate").also { LocalDate.parse(it) },
                field(r, "categoryKey", 64), field(r, "memo", 4000, true), field(r, "paymentMethod", 100), r.getBoolean("isActive"), r.getLong("createdAt"), r.getLong("updatedAt"))
        }
        require(rules.map { it.id }.distinct().size == rules.size)
        val settings = root.optJSONObject("settings")?.let { s ->
            val methods = s.getJSONArray("paymentMethods").let { values -> (0 until values.length()).map { values.getString(it) } }
            require(methods.size in 1..36 && methods.distinct().size == methods.size && methods.all { it.length in 1..100 && '\n' !in it && '\r' !in it })
            val importedLabels = s.getJSONObject("categoryLabels").let { labels -> labels.keys().asSequence().associateWith { labels.getString(it) } }
            val legacyBuiltInKeys = setOf("FOOD", "TRANSPORT", "SHOPPING", "LIVING", "HEALTH", "LEISURE", "OTHER")
            require(importedLabels.size in legacyBuiltInKeys.size..(DEFAULT_CATEGORY_LABELS.size + 20) &&
                importedLabels.keys.all { it in DEFAULT_CATEGORY_LABELS || it.matches(Regex("CUSTOM_[A-F0-9]{12}")) })
            require(importedLabels.keys.containsAll(legacyBuiltInKeys) &&
                importedLabels.values.all { it.length in 1..16 && !it.contains('\n') && !it.contains('\r') })
            require(importedLabels.values.map { it.lowercase(java.util.Locale.ROOT) }.distinct().size == importedLabels.size)
            val labels = (DEFAULT_CATEGORY_LABELS + importedLabels).toMutableMap()
            val importedLabelValues = importedLabels.values.mapTo(mutableSetOf()) { it.lowercase(java.util.Locale.ROOT) }
            DEFAULT_CATEGORY_LABELS.forEach { (key, label) ->
                if (key !in importedLabels && label.lowercase(java.util.Locale.ROOT) in importedLabelValues) {
                    var uniqueLabel = "$label (기본)".take(16)
                    var suffix = 2
                    while (labels.any { (otherKey, otherLabel) -> otherKey != key && otherLabel.equals(uniqueLabel, ignoreCase = true) }) {
                        uniqueLabel = "$label (${suffix++})".take(16)
                    }
                    labels[key] = uniqueLabel
                }
            }
            require(labels.values.map { it.lowercase(java.util.Locale.ROOT) }.distinct().size == labels.size)
            val categoryBudgets = s.getJSONObject("categoryBudgets").let { b -> b.keys().asSequence().associateWith { b.getLong(it) } }
            require(categoryBudgets.keys.all { it in labels } && categoryBudgets.values.all { it in 1..1_000_000_000_000L })
            val categoryOrder = s.optJSONArray("categoryOrder")?.let { order ->
                (0 until order.length()).map { order.getString(it) }
            } ?: (DEFAULT_CATEGORY_LABELS.keys + importedLabels.keys.filterNot { it in DEFAULT_CATEGORY_LABELS }).toList()
            require(categoryOrder.size == importedLabels.size && categoryOrder.distinct().size == categoryOrder.size && categoryOrder.toSet() == importedLabels.keys) {
                "카테고리 순서가 올바르지 않은 백업이에요."
            }
            val upgradedCategoryOrder = categoryOrder + labels.keys.filterNot { it in categoryOrder }
            val monthlyIncomeTargets = s.optJSONObject("monthlyIncomeTargets")?.let { targets ->
                targets.keys().asSequence().associateWith { month -> targets.getLong(month) }
            } ?: emptyMap()
            require(monthlyIncomeTargets.size <= 120 && monthlyIncomeTargets.all { (month, target) ->
                runCatching { YearMonth.parse(month) }.isSuccess && target in 1..1_000_000_000_000L
            }) { "월 수입 목표가 올바르지 않은 백업이에요." }
            val homeDashboardCards = s.optJSONArray("homeDashboardCards")?.let { cards ->
                (0 until cards.length()).map { cards.getString(it) }.toSet()
            } ?: HomeDashboardCards.defaults
            require(homeDashboardCards.size <= HomeDashboardCards.defaults.size && homeDashboardCards.all { it in HomeDashboardCards.defaults }) {
                "홈 카드 설정이 올바르지 않은 백업이에요."
            }
            val noSpendChallengeGoalDays = s.optInt("noSpendChallengeGoalDays", 7)
            require(noSpendChallengeGoalDays in setOf(7, 14, 30)) { "무지출 챌린지 설정이 올바르지 않은 백업이에요." }
            val noSpendChallengeStartDate = if (s.has("noSpendChallengeStartDate") && !s.isNull("noSpendChallengeStartDate")) {
                runCatching { LocalDate.parse(s.getString("noSpendChallengeStartDate")) }.getOrElse {
                    throw IllegalArgumentException("무지출 챌린지 시작일이 올바르지 않은 백업이에요.")
                }
            } else null
            val cardText = s.getString("paymentCards")
            val cards = PaymentCardSettings.decode(cardText)
            require(PaymentCardSettings.encode(cards) == cardText) { "카드 설정이 손상된 백업이에요." }
            BackupSettings(
                s.getBoolean("darkTheme"), s.getBoolean("reduceMotion"), methods, cards, labels,
                categoryBudgets, upgradedCategoryOrder, monthlyIncomeTargets, homeDashboardCards,
                noSpendChallengeGoalDays, noSpendChallengeStartDate,
            )
        }
        if (version == 2) require(settings != null) { "앱 설정이 없는 백업이에요." }
        return FullBackup(transactions, budgets, rules, accounts, settings, legacy = version == 1)
    }
    private fun amount(o: JSONObject): Long = o.getLong("amount").also { require(it in 1..1_000_000_000_000L) { "금액 범위가 올바르지 않아요." } }
    private fun field(o: JSONObject, key: String, max: Int, empty: Boolean = false): String = o.getString(key).also { require(it.length <= max && (empty || it.isNotBlank())) { "$key 값이 올바르지 않아요." } }
    private fun JSONObject.nullableString(key: String): String? = if (!has(key) || isNull(key)) null else getString(key)
    private fun objects(o: JSONObject, key: String): List<JSONObject> = o.getJSONArray(key).let { a ->
        require(a.length() <= MAX_ROWS) { "백업 항목이 너무 많아요." }; (0 until a.length()).map { a.getJSONObject(it) }
    }
    private fun <T> array(values: List<T>, encode: (T) -> JSONObject): JSONArray = JSONArray().apply { values.forEach { put(encode(it)) } }
}
