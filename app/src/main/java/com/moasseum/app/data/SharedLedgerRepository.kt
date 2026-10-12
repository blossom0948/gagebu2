package com.moasseum.app.data

import com.moasseum.app.BuildConfig
import com.moasseum.app.auth.AuthRepository
import com.moasseum.app.data.local.FinanceDao
import com.moasseum.app.data.local.TransactionEntity
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import com.moasseum.app.domain.SharedLedgerSettings
import com.moasseum.app.domain.SharedMoneyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

data class SharedLedgerInvite(val code: String, val expiresAt: String)
data class SharedLedgerGoal(
    val id: String,
    val ownerId: String,
    val title: String,
    val targetAmount: Long,
    val currentAmount: Long,
    val monthKey: String,
)
enum class SharedFinanceKind(val label: String) {
    MONTHLY_INCOME("공동 수입"),
    FIXED_EXPENSE("고정 지출"),
    LIVING_BUDGET("생활비 예산"),
    ALLOWANCE("용돈"),
    ANNIVERSARY("기념일"),
}
data class SharedFinanceItem(
    val id: String,
    val ownerId: String,
    val kind: SharedFinanceKind,
    val title: String,
    val amount: Long? = null,
    val dueDay: Int? = null,
    val monthKey: String? = null,
    val dateKey: String? = null,
    val memo: String = "",
) {
    init {
        require(title.trim().length in 1..80)
        require(memo.length <= 300)
        when (kind) {
            SharedFinanceKind.ANNIVERSARY -> require(
                amount == null && dueDay == null && monthKey == null &&
                    dateKey?.let { runCatching { java.time.MonthDay.parse("--$it") }.isSuccess } == true,
            )
            SharedFinanceKind.LIVING_BUDGET -> require(
                amount?.let { it in 1..1_000_000_000_000L } == true && dueDay == null &&
                    monthKey?.matches(Regex("[0-9]{4}-(0[1-9]|1[0-2])")) == true && dateKey == null,
            )
            else -> require(
                amount?.let { it in 1..1_000_000_000_000L } == true && dueDay?.let { it in 1..31 } == true &&
                    monthKey == null && dateKey == null,
            )
        }
    }
}
data class SharedMemoryPhoto(
    val id: String,
    val ledgerId: String,
    val ownerId: String?,
    val title: String,
    val memo: String,
    val photoDate: LocalDate,
    val objectPath: String,
    val sizeBytes: Long,
)
data class SharedLedgerSnapshot(
    val ledgerId: String? = null,
    val memberCount: Int = 0,
    val transactions: List<Transaction> = emptyList(),
    val invite: SharedLedgerInvite? = null,
    val goals: List<SharedLedgerGoal> = emptyList(),
    val financeItems: List<SharedFinanceItem> = emptyList(),
    val financeSyncAvailable: Boolean = true,
    val settings: SharedLedgerSettings = SharedLedgerSettings(),
    val settingsSyncAvailable: Boolean = true,
    val memories: List<SharedMemoryPhoto> = emptyList(),
    val memorySyncAvailable: Boolean = true,
)

class SharedApiException(val statusCode: Int, message: String) : java.io.IOException(message)

/** Narrow server boundary so two independent signed-in clients can be exercised end to end. */
interface SharedLedgerGateway {
    suspend fun findLedger(token: String, userId: String): String?
    suspend fun countMembers(token: String, ledgerId: String): Int
    suspend fun fetchGoals(token: String, ledgerId: String): List<SharedLedgerGoal>
    suspend fun saveGoal(token: String, ledgerId: String, ownerId: String, goalId: String?, title: String, targetAmount: Long, currentAmount: Long, monthKey: String)
    suspend fun deleteGoal(token: String, ledgerId: String, goalId: String)
    suspend fun fetchFinanceItems(token: String, ledgerId: String): List<SharedFinanceItem>
    suspend fun saveFinanceItem(token: String, ledgerId: String, ownerId: String, item: SharedFinanceItem)
    suspend fun deleteFinanceItem(token: String, ledgerId: String, itemId: String)
    suspend fun fetchMemories(token: String, ledgerId: String): List<SharedMemoryPhoto>
    suspend fun reserveMemory(token: String, ledgerId: String, ownerId: String, id: String, title: String, memo: String, date: LocalDate, path: String, sizeBytes: Long)
    suspend fun uploadMemoryObject(token: String, path: String, bytes: ByteArray)
    suspend fun markMemoryUploaded(token: String, ledgerId: String, id: String)
    suspend fun downloadMemoryObject(token: String, path: String): ByteArray
    suspend fun deleteMemoryObject(token: String, path: String)
    suspend fun deleteMemory(token: String, ledgerId: String, id: String)
    suspend fun fetchSettings(token: String, ledgerId: String): SharedLedgerSettings
    suspend fun saveSettings(token: String, ledgerId: String, userId: String, settings: SharedLedgerSettings)
    suspend fun leave(token: String): String?
    suspend fun createInvite(token: String): SharedLedgerInvite
    suspend fun join(token: String, code: String)
    suspend fun upsertTransactions(token: String, ledgerId: String, ownerId: String, rows: List<TransactionEntity>)
    suspend fun fetchTransactions(token: String, ledgerId: String): List<RemoteSharedTransaction>
    suspend fun deleteTransaction(token: String, ledgerId: String, transactionId: String, ownerId: String)
}

/**
 * The app sends only explicitly shared transaction fields. This repository never
 * uploads personal rows, accounts, budgets, or notification previews.
 */
class SharedLedgerRepository(
    private val dao: FinanceDao,
    private val auth: AuthRepository,
    private val api: SharedLedgerGateway = SharedLedgerApi(),
) {
    suspend fun refresh(): SharedLedgerSnapshot {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId) ?: return SharedLedgerSnapshot()
        return sync(identity, ledgerId)
    }

    suspend fun createInvite(): SharedLedgerSnapshot {
        val identity = identity()
        val invite = api.createInvite(identity.token)
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("공유 장부를 만들지 못했어요. 잠시 후 다시 시도해 주세요.")
        return sync(identity, ledgerId).copy(invite = invite)
    }

    suspend fun join(code: String): SharedLedgerSnapshot {
        val identity = identity()
        api.join(identity.token, code.trim().uppercase())
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("초대에 참여한 장부를 찾지 못했어요. 다시 새로고침해 주세요.")
        return sync(identity, ledgerId)
    }

    suspend fun saveGoal(goalId: String?, title: String, targetAmount: Long, currentAmount: Long, monthKey: String): SharedLedgerSnapshot {
        require(monthKey.matches(Regex("[0-9]{4}-(0[1-9]|1[0-2])"))) { "목표 월을 확인해 주세요." }
        require(title.trim().length in 1..80) { "목표 이름은 1~80자로 입력해 주세요." }
        require(targetAmount in 1..1_000_000_000_000L) { "목표 금액은 1원 이상 입력해 주세요." }
        require(currentAmount in 0..targetAmount) { "현재 모은 금액은 목표 금액을 넘을 수 없어요." }
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        api.saveGoal(identity.token, ledgerId, identity.userId, goalId, title.trim(), targetAmount, currentAmount, monthKey)
        return sync(identity, ledgerId)
    }

    suspend fun deleteGoal(goalId: String): SharedLedgerSnapshot {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        api.deleteGoal(identity.token, ledgerId, goalId)
        return sync(identity, ledgerId)
    }

    suspend fun saveFinanceItem(item: SharedFinanceItem): SharedLedgerSnapshot {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        api.saveFinanceItem(identity.token, ledgerId, identity.userId, item)
        return sync(identity, ledgerId)
    }

    suspend fun deleteFinanceItem(itemId: String): SharedLedgerSnapshot {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        api.deleteFinanceItem(identity.token, ledgerId, itemId)
        return sync(identity, ledgerId)
    }

    suspend fun addMemoryPhoto(title: String, memo: String, date: LocalDate, jpegBytes: ByteArray): SharedLedgerSnapshot {
        require(title.trim().length in 1..60) { "사진 제목은 1~60자로 입력해 주세요." }
        require(memo.length <= 300) { "메모는 300자까지 입력할 수 있어요." }
        require(jpegBytes.size in 4..MAX_MEMORY_PHOTO_BYTES) { "사진은 1MB 이하만 공유할 수 있어요." }
        require(jpegBytes[0] == 0xFF.toByte() && jpegBytes[1] == 0xD8.toByte()) { "JPEG 사진만 공유할 수 있어요." }
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        val photoId = UUID.randomUUID().toString()
        val path = "$ledgerId/$photoId.jpg"
        api.reserveMemory(identity.token, ledgerId, identity.userId, photoId, title.trim(), memo.trim(), date, path, jpegBytes.size.toLong())
        try {
            api.uploadMemoryObject(identity.token, path, jpegBytes)
            api.markMemoryUploaded(identity.token, ledgerId, photoId)
        } catch (error: Exception) {
            runCatching { api.deleteMemoryObject(identity.token, path) }
            runCatching { api.deleteMemory(identity.token, ledgerId, photoId) }
            throw error
        }
        return sync(identity, ledgerId)
    }

    suspend fun downloadMemoryPhoto(photo: SharedMemoryPhoto): ByteArray {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        require(photo.ledgerId == ledgerId && photo.objectPath.startsWith("$ledgerId/")) {
            "다른 공유 장부의 사진은 열 수 없어요."
        }
        return api.downloadMemoryObject(identity.token, photo.objectPath)
    }

    suspend fun deleteMemoryPhoto(photo: SharedMemoryPhoto): SharedLedgerSnapshot {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        require(photo.ledgerId == ledgerId && photo.objectPath.startsWith("$ledgerId/"))
        api.deleteMemoryObject(identity.token, photo.objectPath)
        api.deleteMemory(identity.token, ledgerId, photo.id)
        return sync(identity, ledgerId)
    }

    suspend fun saveSettings(settings: SharedLedgerSettings): SharedLedgerSnapshot {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 파트너와 공유 장부를 연결해 주세요.")
        api.saveSettings(identity.token, ledgerId, identity.userId, settings)
        return sync(identity, ledgerId)
    }

    suspend fun leaveSharedLedger(): SharedLedgerSnapshot {
        val identity = identity()
        val currentLedgerId = api.findLedger(identity.token, identity.userId)
        val ledgerId = api.leave(identity.token) ?: currentLedgerId
        if (ledgerId != null) dao.detachFromSharedLedger(ledgerId, identity.userId, System.currentTimeMillis())
        return SharedLedgerSnapshot()
    }

    suspend fun setShared(transactionId: Long, shared: Boolean): SharedLedgerSnapshot {
        if (shared) return shareTransactions(listOf(transactionId))
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 함께 쓰기 초대장을 만들거나 초대 코드를 입력해 주세요.")
        val transaction = dao.getTransaction(transactionId)
            ?: error("거래를 찾지 못했어요. 목록을 새로고침해 주세요.")
        require(transaction.type in setOf("EXPENSE", "INCOME")) { "계좌 이체는 공유할 수 없어요." }
        val current = dao.getTransaction(transactionId) ?: error("거래를 찾지 못했어요.")
        current.cloudId?.let { api.deleteTransaction(identity.token, ledgerId, it, identity.userId) }
        dao.unshareTransaction(transactionId, System.currentTimeMillis())
        return sync(identity, ledgerId)
    }

    suspend fun shareTransactions(transactionIds: List<Long>): SharedLedgerSnapshot {
        require(transactionIds.isNotEmpty() && transactionIds.size <= 500 && transactionIds.distinct().size == transactionIds.size)
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 함께 쓰기 초대장을 만들거나 초대 코드를 입력해 주세요.")
        // Only explicitly selected transaction IDs are marked for upload.
        dao.shareTransactions(transactionIds, identity.userId, System.currentTimeMillis())
        return sync(identity, ledgerId)
    }

    private suspend fun identity(): Identity {
        val user = auth.state.value.user ?: error("함께 쓰기는 로그인 후 사용할 수 있어요.")
        val token = auth.validAccessToken() ?: error("로그인이 만료됐어요. 다시 로그인해 주세요.")
        return Identity(user.id, token)
    }

    private suspend fun sync(identity: Identity, ledgerId: String): SharedLedgerSnapshot {
        val pending = dao.getSharedOutbox(identity.userId).filter { it.type == "EXPENSE" || it.type == "INCOME" }
        api.upsertTransactions(identity.token, ledgerId, identity.userId, pending)

        val remoteRows = api.fetchTransactions(identity.token, ledgerId)
        remoteRows.forEach { remote ->
            require(remote.ledgerId == ledgerId) { "다른 공유 장부의 응답을 거부했어요." }
            val previous = dao.getTransactionByCloudId(remote.transactionId)
            // A newer offline edit is kept locally and will be uploaded on the next sync.
            if (previous != null && previous.updatedAt > remote.updatedAt) return@forEach
            dao.upsertSharedTransaction(remote.toEntity(previous, identity.userId))
        }

        val all = dao.getAllSharedTransactions()
            .filter { it.deletedAt == null && (it.ledgerId == "personal" || it.ledgerId == "shared:$ledgerId") }
            .map(TransactionEntity::toSharedDomain)
        val financeResult = runCatching { api.fetchFinanceItems(identity.token, ledgerId) }
        val financeItems = financeResult.getOrElse { error ->
            if (error is SharedApiException && error.statusCode == 404) emptyList() else throw error
        }
        val settingsResult = runCatching { api.fetchSettings(identity.token, ledgerId) }
        val settings = settingsResult.getOrElse { error ->
            if (error is SharedApiException && error.statusCode == 404) SharedLedgerSettings() else throw error
        }
        val memoryResult = runCatching { api.fetchMemories(identity.token, ledgerId) }
        val memories = memoryResult.getOrElse { error ->
            if (error is SharedApiException && error.statusCode in setOf(400, 404)) emptyList() else throw error
        }
        return SharedLedgerSnapshot(
            ledgerId = ledgerId,
            memberCount = api.countMembers(identity.token, ledgerId),
            transactions = all,
            goals = api.fetchGoals(identity.token, ledgerId),
            financeItems = financeItems,
            financeSyncAvailable = financeResult.isSuccess,
            settings = settings,
            settingsSyncAvailable = settingsResult.isSuccess,
            memories = memories,
            memorySyncAvailable = memoryResult.isSuccess,
        )
    }

    companion object {
        const val MAX_MEMORY_PHOTO_BYTES = 1_048_576
        const val MAX_MEMORY_BYTES = 15L * 1_048_576
        const val MAX_MEMORY_PHOTOS = 30
    }

    private data class Identity(val userId: String, val token: String)
}

data class RemoteSharedTransaction(
    val ledgerId: String,
    val transactionId: String,
    val ownerId: String,
    val type: String,
    val amount: Long,
    val occurredAt: Long,
    val timezone: String,
    val categoryKey: String,
    val merchant: String,
    val memo: String,
    val paymentMethod: String,
    val updatedAt: Long,
    val deletedAt: Long?,
    val currency: String = "KRW",
    val originalAmount: String? = null,
    val exchangeRate: String? = null,
    val exchangeRateDate: String? = null,
) {
    fun toEntity(existing: TransactionEntity?, signedInUserId: String) = TransactionEntity(
        id = existing?.id ?: 0,
        ownerId = ownerId,
        ledgerId = if (ownerId == signedInUserId) "personal" else "shared:$ledgerId",
        type = type,
        amount = amount,
        occurredAt = occurredAt,
        timezone = timezone,
        categoryKey = categoryKey,
        merchant = merchant,
        memo = memo,
        paymentMethod = paymentMethod,
        source = existing?.source ?: "SHARED",
        sharingScope = "SHARED",
        createdAt = existing?.createdAt ?: updatedAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
        accountId = existing?.accountId,
        destinationAccountId = existing?.destinationAccountId,
        cloudId = transactionId,
        currency = currency,
        originalAmount = originalAmount,
        exchangeRate = exchangeRate,
        exchangeRateDate = exchangeRateDate,
    )
}

private fun TransactionEntity.toSharedDomain() = Transaction(
    id = id,
    type = TransactionType.valueOf(type),
    amount = amount,
    occurredAt = occurredAt,
    categoryKey = categoryKey,
    merchant = merchant,
    memo = memo,
    paymentMethod = paymentMethod,
    source = source,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    timezone = timezone,
    ownerId = ownerId,
    ledgerId = ledgerId,
    sharingScope = sharingScope,
    cloudId = cloudId,
    currency = currency,
    originalAmount = originalAmount,
    exchangeRateToKrw = exchangeRate,
    exchangeRateDate = exchangeRateDate,
)

class SharedLedgerApi(
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
) : SharedLedgerGateway {
    override suspend fun findLedger(token: String, userId: String): String? {
        val result = request("GET", "shared_ledger_members?select=ledger_id&user_id=eq.${enc(userId)}&limit=1", token)
        return JSONArray(result).optJSONObject(0)?.optString("ledger_id")?.takeIf(String::isNotBlank)
    }

    override suspend fun countMembers(token: String, ledgerId: String): Int = JSONArray(
        request("GET", "shared_ledger_members?select=user_id&ledger_id=eq.${enc(ledgerId)}&limit=2", token),
    ).length()

    override suspend fun fetchGoals(token: String, ledgerId: String): List<SharedLedgerGoal> {
        val rows = try {
            fetchPagedRows(
                token,
                "shared_goals?select=id,owner_id,title,target_amount,current_amount,month_key&ledger_id=eq.${enc(ledgerId)}&order=month_key.desc,id.asc",
                pageSize = 120,
                maxRows = 5_000,
            )
        } catch (error: SharedApiException) {
            if (error.statusCode != 400) throw error
            // Keep the rest of a shared ledger usable when the additive goal
            // progress migration has not reached the server yet.
            fetchPagedRows(
                token,
                "shared_goals?select=id,owner_id,title,target_amount,month_key&ledger_id=eq.${enc(ledgerId)}&order=month_key.desc,id.asc",
                pageSize = 120,
                maxRows = 5_000,
            )
        }
        return rows.map { row ->
            SharedLedgerGoal(
                id = UUID.fromString(row.getString("id")).toString(),
                ownerId = UUID.fromString(row.getString("owner_id")).toString(),
                title = row.getString("title").take(80),
                targetAmount = row.getLong("target_amount").also { require(it in 1..1_000_000_000_000L) },
                currentAmount = row.optLong("current_amount", 0L).also { require(it in 0..1_000_000_000_000L) },
                monthKey = row.getString("month_key").also { require(it.matches(Regex("[0-9]{4}-(0[1-9]|1[0-2])"))) },
            )
        }
    }

    override suspend fun saveGoal(
        token: String,
        ledgerId: String,
        ownerId: String,
        goalId: String?,
        title: String,
        targetAmount: Long,
        currentAmount: Long,
        monthKey: String,
    ) {
        val body = JSONObject()
            .put("title", title)
            .put("target_amount", targetAmount)
            .put("current_amount", currentAmount)
            .put("month_key", monthKey)
            .put("updated_at", Instant.now().toString())
        if (goalId != null) {
            UUID.fromString(goalId)
            val updated = JSONArray(request(
                "PATCH",
                "shared_goals?id=eq.${enc(goalId)}&ledger_id=eq.${enc(ledgerId)}",
                token,
                body,
                prefer = "return=representation",
            ))
            check(updated.length() == 1) { "공동 목표가 삭제되었거나 수정 권한이 없어요. 새로고침해 주세요." }
        } else {
            body.put("id", UUID.randomUUID().toString())
                .put("ledger_id", ledgerId)
                .put("owner_id", ownerId)
            request(
                "POST", "shared_goals",
                token,
                body,
                prefer = "return=representation",
            )
        }
    }

    override suspend fun fetchFinanceItems(token: String, ledgerId: String): List<SharedFinanceItem> {
        val rows = fetchPagedRows(
            token,
            "shared_finance_items?select=id,owner_id,kind,title,amount,due_day,month_key,date_key,memo&ledger_id=eq.${enc(ledgerId)}&order=kind.asc,title.asc,id.asc",
            pageSize = 200,
            maxRows = 3_000,
        )
        return rows.map { row ->
            SharedFinanceItem(
                id = UUID.fromString(row.getString("id")).toString(),
                ownerId = UUID.fromString(row.getString("owner_id")).toString(),
                kind = SharedFinanceKind.valueOf(row.getString("kind")),
                title = row.getString("title").take(80),
                amount = if (row.isNull("amount")) null else row.getLong("amount"),
                dueDay = if (row.isNull("due_day")) null else row.getInt("due_day"),
                monthKey = row.optString("month_key").takeIf(String::isNotBlank),
                dateKey = row.optString("date_key").takeIf(String::isNotBlank),
                memo = row.optString("memo").take(300),
            )
        }
    }

    override suspend fun fetchSettings(token: String, ledgerId: String): SharedLedgerSettings {
        val rows = JSONArray(request(
            "GET",
            "shared_ledger_settings?select=money_mode,relationship_start_date&ledger_id=eq.${enc(ledgerId)}&limit=1",
            token,
        ))
        val row = rows.optJSONObject(0)
        val mode = row?.optString("money_mode")
            ?.let { runCatching { SharedMoneyMode.valueOf(it) }.getOrNull() }
            ?: SharedMoneyMode.EQUAL_SPLIT
        val relationshipStartDate = row?.optString("relationship_start_date")
            ?.takeIf { it.isNotBlank() && it != "null" }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return SharedLedgerSettings(moneyMode = mode, relationshipStartDate = relationshipStartDate)
    }

    override suspend fun saveSettings(token: String, ledgerId: String, userId: String, settings: SharedLedgerSettings) {
        val body = JSONObject()
            .put("ledger_id", ledgerId)
            .put("money_mode", settings.moneyMode.name)
            .put("relationship_start_date", settings.relationshipStartDate?.toString() ?: JSONObject.NULL)
            .put("updated_by", userId)
            .put("updated_at", Instant.now().toString())
        val saved = JSONArray(request(
            "POST",
            "shared_ledger_settings?on_conflict=ledger_id",
            token,
            body,
            prefer = "resolution=merge-duplicates,return=representation",
        ))
        check(saved.length() == 1) { "공동 자금 방식을 저장하지 못했어요." }
    }

    override suspend fun leave(token: String): String? {
        val response = request("POST", "rpc/leave_shared_ledger", token, JSONObject())
        val value = runCatching { org.json.JSONTokener(response).nextValue() }.getOrNull()
        return (value as? String)?.let { runCatching { UUID.fromString(it).toString() }.getOrNull() }
    }

    override suspend fun saveFinanceItem(token: String, ledgerId: String, ownerId: String, item: SharedFinanceItem) {
        val itemId = runCatching { UUID.fromString(item.id).toString() }.getOrElse { UUID.randomUUID().toString() }
        val body = JSONObject()
            .put("kind", item.kind.name)
            .put("title", item.title.trim())
            .put("amount", item.amount ?: JSONObject.NULL)
            .put("due_day", item.dueDay ?: JSONObject.NULL)
            .put("month_key", item.monthKey ?: JSONObject.NULL)
            .put("date_key", item.dateKey ?: JSONObject.NULL)
            .put("memo", item.memo.take(300))
            .put("updated_at", Instant.now().toString())
        val exists = item.id.isNotBlank() && runCatching { UUID.fromString(item.id) }.isSuccess
        if (exists) {
            val updated = JSONArray(request(
                "PATCH", "shared_finance_items?id=eq.${enc(itemId)}&ledger_id=eq.${enc(ledgerId)}",
                token, body, prefer = "return=representation",
            ))
            check(updated.length() == 1) { "공동 재정 항목이 삭제되었거나 수정 권한이 없어요. 새로고침해 주세요." }
        } else {
            body.put("id", itemId).put("ledger_id", ledgerId).put("owner_id", ownerId)
            request("POST", "shared_finance_items", token, body, prefer = "return=representation")
        }
    }

    override suspend fun deleteFinanceItem(token: String, ledgerId: String, itemId: String) {
        UUID.fromString(itemId)
        val deleted = JSONArray(request(
            "DELETE", "shared_finance_items?id=eq.${enc(itemId)}&ledger_id=eq.${enc(ledgerId)}",
            token, prefer = "return=representation",
        ))
        check(deleted.length() == 1) { "공동 재정 항목이 이미 삭제되었어요." }
    }

    override suspend fun fetchMemories(token: String, ledgerId: String): List<SharedMemoryPhoto> =
        fetchPagedRows(
            token,
            "shared_memory_photos?select=id,ledger_id,owner_id,title,memo,photo_date,object_path,size_bytes,uploaded_at&ledger_id=eq.${enc(ledgerId)}&uploaded_at=not.is.null&order=photo_date.desc,created_at.desc",
            pageSize = 100,
            maxRows = 30,
        ).map { row ->
            val rowLedgerId = UUID.fromString(row.getString("ledger_id")).toString()
            require(rowLedgerId == ledgerId) { "다른 공유 장부의 사진 응답을 거부했어요." }
            val id = UUID.fromString(row.getString("id")).toString()
            val path = row.getString("object_path")
            require(path == "$ledgerId/$id.jpg") { "공유 사진 경로가 올바르지 않아요." }
            SharedMemoryPhoto(
                id = id,
                ledgerId = rowLedgerId,
                ownerId = row.optString("owner_id").takeIf { it.isNotBlank() && it != "null" }
                    ?.let { UUID.fromString(it).toString() },
                title = row.getString("title").take(60),
                memo = row.optString("memo").take(300),
                photoDate = LocalDate.parse(row.getString("photo_date")),
                objectPath = path,
                sizeBytes = row.getLong("size_bytes").also { require(it in 1..SharedLedgerRepository.MAX_MEMORY_PHOTO_BYTES.toLong()) },
            )
        }

    override suspend fun reserveMemory(
        token: String,
        ledgerId: String,
        ownerId: String,
        id: String,
        title: String,
        memo: String,
        date: LocalDate,
        path: String,
        sizeBytes: Long,
    ) {
        UUID.fromString(ledgerId); UUID.fromString(ownerId); UUID.fromString(id)
        require(path == "$ledgerId/$id.jpg")
        val row = JSONObject()
            .put("id", id)
            .put("ledger_id", ledgerId)
            .put("owner_id", ownerId)
            .put("title", title.trim())
            .put("memo", memo.trim())
            .put("photo_date", date.toString())
            .put("object_path", path)
            .put("size_bytes", sizeBytes)
        val response = JSONArray(request("POST", "shared_memory_photos", token, row, prefer = "return=representation"))
        check(response.length() == 1) {
            "공유 사진을 준비하지 못했어요. 커플 저장 한도(사진 30장·총 15MB)를 확인해 주세요."
        }
    }

    override suspend fun uploadMemoryObject(token: String, path: String, bytes: ByteArray) {
        require(bytes.size in 1..SharedLedgerRepository.MAX_MEMORY_PHOTO_BYTES)
        storageRequest("POST", "object/couple_memory/${storagePath(path)}", token, bytes, "image/jpeg")
    }

    override suspend fun markMemoryUploaded(token: String, ledgerId: String, id: String) {
        UUID.fromString(ledgerId); UUID.fromString(id)
        val response = request(
            "POST",
            "rpc/finalize_shared_memory_photo",
            token,
            JSONObject().put("p_photo_id", id),
        )
        val finalized = runCatching { org.json.JSONTokener(response).nextValue() }.getOrNull()
        check(finalized == true) { "사진을 올렸지만 앨범 등록을 확인하지 못했어요. 새로고침해 주세요." }
    }

    override suspend fun downloadMemoryObject(token: String, path: String): ByteArray {
        requireMemoryPath(path)
        return storageRequest("GET", "object/authenticated/couple_memory/${storagePath(path)}", token)
            .also { require(it.size in 1..SharedLedgerRepository.MAX_MEMORY_PHOTO_BYTES) { "공유 사진 크기가 허용 범위를 벗어났어요." } }
    }

    override suspend fun deleteMemoryObject(token: String, path: String) {
        requireMemoryPath(path)
        val body = JSONObject().put("prefixes", JSONArray().put(path)).toString().toByteArray(Charsets.UTF_8)
        try {
            storageRequest("DELETE", "object/couple_memory", token, body, "application/json")
        } catch (error: SharedApiException) {
            if (error.statusCode != 404) throw error
        }
    }

    override suspend fun deleteMemory(token: String, ledgerId: String, id: String) {
        UUID.fromString(ledgerId); UUID.fromString(id)
        val deleted = JSONArray(request(
            "DELETE", "shared_memory_photos?id=eq.${enc(id)}&ledger_id=eq.${enc(ledgerId)}",
            token, prefer = "return=representation",
        ))
        check(deleted.length() == 1) { "공유 사진 정보가 이미 삭제되었어요." }
    }

    private fun requireMemoryPath(path: String) {
        require(path.matches(Regex("[0-9a-fA-F-]{36}/[0-9a-fA-F-]{36}\\.jpg"))) {
            "공유 사진 경로가 올바르지 않아요."
        }
    }

    private fun storagePath(path: String): String = path.split('/').joinToString("/") { enc(it) }

    private suspend fun storageRequest(
        method: String,
        path: String,
        token: String,
        body: ByteArray? = null,
        contentType: String = "application/octet-stream",
    ): ByteArray = withContext(Dispatchers.IO) {
        check(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) {
            "공유 서버 설정이 준비되지 않았어요. 관리자 키는 앱에 포함하지 않습니다."
        }
        val endpoint = URL("${baseUrl.trimEnd('/')}/storage/v1/$path")
        require(endpoint.protocol == "https" && endpoint.userInfo == null)
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", contentType)
            if (method == "POST") connection.setRequestProperty("x-upsert", "false")
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val bytes = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > SharedLedgerRepository.MAX_MEMORY_PHOTO_BYTES + 16_384) {
                        throw java.io.IOException("공유 사진 응답이 너무 큽니다.")
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: ByteArray(0)
            if (status !in 200..299) {
                val detail = bytes.toString(Charsets.UTF_8).take(240)
                throw SharedApiException(status, when (status) {
                    401, 403 -> "공유 사진 접근 권한을 확인할 수 없어요. 로그인과 공유 연결을 확인해 주세요."
                    404 -> "공유 사진 또는 사진 서버 설정을 찾지 못했어요."
                    413 -> "사진은 1MB 이하만 공유할 수 있어요."
                    in 500..599 -> "공유 사진 서버가 잠시 응답하지 않아요."
                    else -> "사진 공유 요청을 완료하지 못했어요. ${detail.takeIf(String::isNotBlank).orEmpty()}"
                })
            }
            bytes
        } catch (error: java.io.IOException) {
            throw error
        } catch (error: Exception) {
            throw java.io.IOException("공유 사진 서버에 연결하지 못했어요. 네트워크를 확인해 주세요.", error)
        } finally {
            connection.disconnect()
        }
    }

    override suspend fun deleteGoal(token: String, ledgerId: String, goalId: String) {
        UUID.fromString(goalId)
        val deleted = JSONArray(request(
            "DELETE",
            "shared_goals?id=eq.${enc(goalId)}&ledger_id=eq.${enc(ledgerId)}",
            token,
            prefer = "return=representation",
        ))
        check(deleted.length() == 1) { "공동 목표가 이미 삭제되었어요. 새로고침해 주세요." }
    }

    override suspend fun createInvite(token: String): SharedLedgerInvite {
        val response = jsonObject(request("POST", "rpc/create_shared_ledger_invite", token, JSONObject()))
        val code = response.getString("invite_code")
        require(code.matches(Regex("[A-F0-9]{20}"))) { "초대 코드를 안전하게 만들지 못했어요." }
        return SharedLedgerInvite(code, response.optString("expires_at"))
    }

    override suspend fun join(token: String, code: String) {
        require(code.matches(Regex("[A-F0-9]{20}"))) { "초대 코드는 20자리 영문·숫자입니다." }
        request("POST", "rpc/join_shared_ledger", token, JSONObject().put("invite_code", code))
    }

    override suspend fun upsertTransactions(token: String, ledgerId: String, ownerId: String, rows: List<TransactionEntity>) {
        if (rows.isEmpty()) return
        val payload = JSONArray()
        rows.forEach { row ->
            val transactionId = row.cloudId?.also { UUID.fromString(it) } ?: return@forEach
            if (row.type !in setOf("EXPENSE", "INCOME")) return@forEach
            val item = JSONObject()
                .put("ledger_id", ledgerId)
                .put("transaction_id", transactionId)
                .put("owner_id", ownerId)
                .put("type", row.type)
                .put("amount", row.amount)
                .put("occurred_at", Instant.ofEpochMilli(row.occurredAt).toString())
                .put("timezone", row.timezone.take(80))
                .put("category_key", row.categoryKey.take(40))
                .put("merchant", row.merchant.trim().take(300))
                .put("memo", row.memo.take(1000))
                .put("payment_method", row.paymentMethod.take(80))
                .put("currency", row.currency)
                .put("original_amount", row.originalAmount ?: JSONObject.NULL)
                .put("exchange_rate", row.exchangeRate ?: JSONObject.NULL)
                .put("exchange_rate_date", row.exchangeRateDate ?: JSONObject.NULL)
                .put("updated_at", Instant.ofEpochMilli(row.updatedAt).toString())
            if (row.deletedAt == null) item.put("deleted_at", JSONObject.NULL)
            else item.put("deleted_at", Instant.ofEpochMilli(row.deletedAt).toString())
            payload.put(item)
        }
        if (payload.length() == 0) return
        try {
            request(
                "POST",
                "shared_transactions?on_conflict=ledger_id%2Ctransaction_id",
                token,
                payload,
                prefer = "resolution=merge-duplicates,return=minimal",
            )
        } catch (error: SharedApiException) {
            if (error.statusCode != 400 || rows.any { it.currency != "KRW" }) {
                if (error.statusCode == 400 && rows.any { it.currency != "KRW" }) {
                    throw SharedApiException(400, "외화 공유를 위한 서버 업데이트가 아직 적용되지 않았어요.")
                }
                throw error
            }
            for (index in 0 until payload.length()) {
                payload.getJSONObject(index).apply {
                    remove("currency"); remove("original_amount"); remove("exchange_rate"); remove("exchange_rate_date")
                }
            }
            request(
                "POST",
                "shared_transactions?on_conflict=ledger_id%2Ctransaction_id",
                token,
                payload,
                prefer = "resolution=merge-duplicates,return=minimal",
            )
        }
    }

    override suspend fun fetchTransactions(token: String, ledgerId: String): List<RemoteSharedTransaction> {
        val columns = "ledger_id,transaction_id,owner_id,type,amount,occurred_at,timezone,category_key,merchant,memo,payment_method,currency,original_amount,exchange_rate,exchange_rate_date,updated_at,deleted_at"
        val rows = try {
            fetchPagedRows(
                token,
                "shared_transactions?select=$columns&ledger_id=eq.${enc(ledgerId)}&order=transaction_id.asc",
                pageSize = 1_000,
                maxRows = 20_000,
            )
        } catch (error: SharedApiException) {
            if (error.statusCode != 400) throw error
            fetchPagedRows(
                token,
                "shared_transactions?select=ledger_id,transaction_id,owner_id,type,amount,occurred_at,timezone,category_key,merchant,memo,payment_method,updated_at,deleted_at&ledger_id=eq.${enc(ledgerId)}&order=transaction_id.asc",
                pageSize = 1_000,
                maxRows = 20_000,
            )
        }
        return rows.map { item ->
            RemoteSharedTransaction(
                ledgerId = item.getString("ledger_id"),
                transactionId = UUID.fromString(item.getString("transaction_id")).toString(),
                ownerId = UUID.fromString(item.getString("owner_id")).toString(),
                type = item.getString("type").also { require(it == "EXPENSE" || it == "INCOME") },
                amount = item.getLong("amount").also { require(it in 1..1_000_000_000_000L) },
                occurredAt = Instant.parse(item.getString("occurred_at")).toEpochMilli(),
                timezone = item.getString("timezone").take(80),
                categoryKey = item.getString("category_key").take(40),
                merchant = item.getString("merchant").take(300),
                memo = item.optString("memo").take(1000),
                paymentMethod = item.optString("payment_method", "카드").take(80),
                updatedAt = Instant.parse(item.getString("updated_at")).toEpochMilli(),
                deletedAt = item.optString("deleted_at").takeIf { it.isNotBlank() && it != "null" }?.let { Instant.parse(it).toEpochMilli() },
                currency = item.optString("currency").takeIf { it.matches(Regex("[A-Z]{3}")) } ?: "KRW",
                originalAmount = item.optString("original_amount").takeIf { it.isNotBlank() && it != "null" },
                exchangeRate = item.optString("exchange_rate").takeIf { it.isNotBlank() && it != "null" },
                exchangeRateDate = item.optString("exchange_rate_date").takeIf { it.isNotBlank() && it != "null" },
            )
        }
    }

    private suspend fun fetchPagedRows(
        token: String,
        basePath: String,
        pageSize: Int,
        maxRows: Int,
    ): List<JSONObject> {
        require(pageSize in 1..1_000 && maxRows >= pageSize)
        val result = ArrayList<JSONObject>()
        var offset = 0
        while (true) {
            val page = JSONArray(request("GET", "$basePath&limit=$pageSize&offset=$offset", token))
            require(result.size + page.length() <= maxRows) {
                "공유 기록이 너무 많아 한 번에 불러오지 못했어요. 기간별 조회가 필요합니다."
            }
            for (index in 0 until page.length()) result += page.getJSONObject(index)
            if (page.length() < pageSize) return result
            offset += pageSize
        }
    }

    override suspend fun deleteTransaction(token: String, ledgerId: String, transactionId: String, ownerId: String) {
        UUID.fromString(transactionId)
        request(
            "DELETE",
            "shared_transactions?ledger_id=eq.${enc(ledgerId)}&transaction_id=eq.${enc(transactionId)}&owner_id=eq.${enc(ownerId)}",
            token,
            prefer = "return=minimal",
        )
    }

    private suspend fun request(
        method: String,
        path: String,
        token: String,
        body: Any? = null,
        prefer: String? = null,
    ): String = withContext(Dispatchers.IO) {
        check(baseUrl.startsWith("https://") && publishableKey.startsWith("sb_publishable_")) {
            "공유 서버 설정이 준비되지 않았어요. 관리자 키는 앱에 포함하지 않습니다."
        }
        val endpoint = URL("${baseUrl.trimEnd('/')}/rest/v1/$path")
        require(endpoint.protocol == "https" && endpoint.userInfo == null)
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("apikey", publishableKey)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            prefer?.let { connection.setRequestProperty("Prefer", it) }
            if (body != null) {
                connection.doOutput = true
                val bytes = when (body) {
                    is JSONObject -> body.toString().toByteArray(Charsets.UTF_8)
                    is JSONArray -> body.toString().toByteArray(Charsets.UTF_8)
                    else -> error("Unsupported request payload")
                }
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 2_097_152) throw java.io.IOException("공유 응답이 너무 큽니다.")
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }.orEmpty()
            if (status !in 200..299) {
                throw SharedApiException(status, when (status) {
                    401, 403 -> "공유 서버 권한을 확인할 수 없어요. 로그인 상태를 확인해 주세요."
                    404 -> if (path.startsWith("shared_finance_items")) {
                        "공동 재정 기능의 서버 업데이트가 아직 적용되지 않았어요."
                    } else {
                        "공유 기능 서버 설정이 아직 적용되지 않았어요. DB 마이그레이션을 먼저 적용해 주세요."
                    }
                    409, 422 -> "초대 코드가 만료됐거나 사용할 수 없어요. 코드와 계정 상태를 확인해 주세요."
                    in 500..599 -> "공유 서버가 잠시 응답하지 않아요. 잠시 후 다시 시도해 주세요."
                    else -> "공유 요청을 완료하지 못했어요. 네트워크와 초대 상태를 확인해 주세요."
                })
            }
            response
        } catch (error: java.io.IOException) {
            throw error
        } catch (error: Exception) {
            throw java.io.IOException("공유 서버에 연결하지 못했어요. 네트워크를 확인해 주세요.", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun jsonObject(value: String): JSONObject = when {
        value.isBlank() -> JSONObject()
        value.trimStart().startsWith("[") -> JSONArray(value).optJSONObject(0) ?: JSONObject()
        else -> JSONObject(value)
    }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
