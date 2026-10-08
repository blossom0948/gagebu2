package com.moasseum.app.data

import com.moasseum.app.BuildConfig
import com.moasseum.app.auth.AuthRepository
import com.moasseum.app.data.local.FinanceDao
import com.moasseum.app.data.local.TransactionEntity
import com.moasseum.app.domain.Transaction
import com.moasseum.app.domain.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
data class SharedLedgerSnapshot(
    val ledgerId: String? = null,
    val memberCount: Int = 0,
    val transactions: List<Transaction> = emptyList(),
    val invite: SharedLedgerInvite? = null,
    val goals: List<SharedLedgerGoal> = emptyList(),
    val financeItems: List<SharedFinanceItem> = emptyList(),
    val financeSyncAvailable: Boolean = true,
)

class SharedApiException(val statusCode: Int, message: String) : java.io.IOException(message)

/**
 * The app sends only explicitly shared transaction fields. This repository never
 * uploads personal rows, accounts, budgets, or notification previews.
 */
class SharedLedgerRepository(
    private val dao: FinanceDao,
    private val auth: AuthRepository,
    private val api: SharedLedgerApi = SharedLedgerApi(),
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

    suspend fun setShared(transactionId: Long, shared: Boolean): SharedLedgerSnapshot {
        val identity = identity()
        val ledgerId = api.findLedger(identity.token, identity.userId)
            ?: error("먼저 함께 쓰기 초대장을 만들거나 초대 코드를 입력해 주세요.")
        val transaction = dao.getTransaction(transactionId)
            ?: error("거래를 찾지 못했어요. 목록을 새로고침해 주세요.")
        require(transaction.type in setOf("EXPENSE", "INCOME")) { "계좌 이체는 공유할 수 없어요." }
        if (shared) {
            val changed = dao.shareTransaction(transactionId, identity.userId, UUID.randomUUID().toString(), System.currentTimeMillis())
            require(changed > 0) { "삭제된 거래는 공유할 수 없어요." }
            // Intent is saved locally first. If the network fails, Retry uploads only this selected row.
            return sync(identity, ledgerId)
        }
        val current = dao.getTransaction(transactionId) ?: error("거래를 찾지 못했어요.")
        current.cloudId?.let { api.deleteTransaction(identity.token, ledgerId, it, identity.userId) }
        dao.unshareTransaction(transactionId, System.currentTimeMillis())
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
        return SharedLedgerSnapshot(
            ledgerId = ledgerId,
            memberCount = api.countMembers(identity.token, ledgerId),
            transactions = all,
            goals = api.fetchGoals(identity.token, ledgerId),
            financeItems = financeItems,
            financeSyncAvailable = financeResult.isSuccess,
        )
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
)

class SharedLedgerApi(
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
) {
    suspend fun findLedger(token: String, userId: String): String? {
        val result = request("GET", "shared_ledger_members?select=ledger_id&user_id=eq.${enc(userId)}&limit=1", token)
        return JSONArray(result).optJSONObject(0)?.optString("ledger_id")?.takeIf(String::isNotBlank)
    }

    suspend fun countMembers(token: String, ledgerId: String): Int = JSONArray(
        request("GET", "shared_ledger_members?select=user_id&ledger_id=eq.${enc(ledgerId)}&limit=2", token),
    ).length()

    suspend fun fetchGoals(token: String, ledgerId: String): List<SharedLedgerGoal> {
        val rows = try {
            JSONArray(request(
                "GET",
                "shared_goals?select=id,owner_id,title,target_amount,current_amount,month_key&ledger_id=eq.${enc(ledgerId)}&order=month_key.desc&limit=120",
                token,
            ))
        } catch (error: SharedApiException) {
            if (error.statusCode != 400) throw error
            // Keep the rest of a shared ledger usable when the additive goal
            // progress migration has not reached the server yet.
            JSONArray(request(
                "GET",
                "shared_goals?select=id,owner_id,title,target_amount,month_key&ledger_id=eq.${enc(ledgerId)}&order=month_key.desc&limit=120",
                token,
            ))
        }
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
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

    suspend fun saveGoal(
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

    suspend fun fetchFinanceItems(token: String, ledgerId: String): List<SharedFinanceItem> {
        val rows = JSONArray(request(
            "GET",
            "shared_finance_items?select=id,owner_id,kind,title,amount,due_day,month_key,date_key,memo&ledger_id=eq.${enc(ledgerId)}&order=kind.asc,title.asc&limit=300",
            token,
        ))
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
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

    suspend fun saveFinanceItem(token: String, ledgerId: String, ownerId: String, item: SharedFinanceItem) {
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

    suspend fun deleteFinanceItem(token: String, ledgerId: String, itemId: String) {
        UUID.fromString(itemId)
        val deleted = JSONArray(request(
            "DELETE", "shared_finance_items?id=eq.${enc(itemId)}&ledger_id=eq.${enc(ledgerId)}",
            token, prefer = "return=representation",
        ))
        check(deleted.length() == 1) { "공동 재정 항목이 이미 삭제되었어요." }
    }

    suspend fun deleteGoal(token: String, ledgerId: String, goalId: String) {
        UUID.fromString(goalId)
        val deleted = JSONArray(request(
            "DELETE",
            "shared_goals?id=eq.${enc(goalId)}&ledger_id=eq.${enc(ledgerId)}",
            token,
            prefer = "return=representation",
        ))
        check(deleted.length() == 1) { "공동 목표가 이미 삭제되었어요. 새로고침해 주세요." }
    }

    suspend fun createInvite(token: String): SharedLedgerInvite {
        val response = jsonObject(request("POST", "rpc/create_shared_ledger_invite", token, JSONObject()))
        val code = response.getString("invite_code")
        require(code.matches(Regex("[A-F0-9]{20}"))) { "초대 코드를 안전하게 만들지 못했어요." }
        return SharedLedgerInvite(code, response.optString("expires_at"))
    }

    suspend fun join(token: String, code: String) {
        require(code.matches(Regex("[A-F0-9]{20}"))) { "초대 코드는 20자리 영문·숫자입니다." }
        request("POST", "rpc/join_shared_ledger", token, JSONObject().put("invite_code", code))
    }

    suspend fun upsertTransactions(token: String, ledgerId: String, ownerId: String, rows: List<TransactionEntity>) {
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
                .put("updated_at", Instant.ofEpochMilli(row.updatedAt).toString())
            if (row.deletedAt == null) item.put("deleted_at", JSONObject.NULL)
            else item.put("deleted_at", Instant.ofEpochMilli(row.deletedAt).toString())
            payload.put(item)
        }
        if (payload.length() == 0) return
        request(
            "POST",
            "shared_transactions?on_conflict=ledger_id%2Ctransaction_id",
            token,
            payload,
            prefer = "resolution=merge-duplicates,return=minimal",
        )
    }

    suspend fun fetchTransactions(token: String, ledgerId: String): List<RemoteSharedTransaction> {
        val columns = "ledger_id,transaction_id,owner_id,type,amount,occurred_at,timezone,category_key,merchant,memo,payment_method,updated_at,deleted_at"
        val response = JSONArray(request(
            "GET",
            "shared_transactions?select=$columns&ledger_id=eq.${enc(ledgerId)}&order=updated_at.asc&limit=1000",
            token,
        ))
        return (0 until response.length()).map { index ->
            val item = response.getJSONObject(index)
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
            )
        }
    }

    suspend fun deleteTransaction(token: String, ledgerId: String, transactionId: String, ownerId: String) {
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
