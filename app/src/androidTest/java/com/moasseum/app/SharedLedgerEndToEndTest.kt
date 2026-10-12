package com.moasseum.app

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.auth.AuthApi
import com.moasseum.app.auth.AuthRepository
import com.moasseum.app.auth.AuthSession
import com.moasseum.app.auth.SessionStore
import com.moasseum.app.auth.SignedInUser
import com.moasseum.app.data.*
import com.moasseum.app.data.local.FinanceDatabase
import com.moasseum.app.data.local.TransactionEntity
import com.moasseum.app.domain.SharedLedgerSettings
import com.moasseum.app.domain.SharedMoneyMode
import com.moasseum.app.domain.CurrencyConversion
import com.moasseum.app.domain.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SharedLedgerEndToEndTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun twoSignedInRoomClientsOnlyExchangeExplicitlySharedTransactions() = runBlocking {
        check(context.packageName.endsWith(".qa")) { "This integration test must only run in the isolated QA app." }
        val server = InMemorySharedLedgerGateway()
        val dbA = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        val dbB = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        val dbUnrelated = Room.inMemoryDatabaseBuilder(context, FinanceDatabase::class.java).build()
        try {
            val authA = authFor("a1c032bd-66f2-43ca-8d3c-c1596de09e11")
            val authB = authFor("b2d143ce-77a3-44db-9e4d-d2607ef10f22")
            val authUnrelated = authFor("c3e254df-88b4-45ec-af5e-e3718f021033")
            val repoA = SharedLedgerRepository(dbA.financeDao(), authA, server)
            val repoB = SharedLedgerRepository(dbB.financeDao(), authB, server)
            val repoUnrelated = SharedLedgerRepository(dbUnrelated.financeDao(), authUnrelated, server)
            val financeA = FinanceRepository(dbA.financeDao())
            val financeB = FinanceRepository(dbB.financeDao())
            financeA.addTransaction(1_250, TransactionType.EXPENSE, LocalDate.parse("2026-10-02"), "FOOD", "QA 공유 지출", "선택 공유")
            financeA.addTransaction(73_000, TransactionType.INCOME, LocalDate.parse("2026-10-03"), "SALARY", "QA 개인 수입", "공유 금지")
            financeA.addTransaction(
                13_335, TransactionType.EXPENSE, LocalDate.parse("2026-10-05"), "SHOPPING", "QA 외화 지출", "환율 왕복",
                currencyConversion = CurrencyConversion("USD", "10.00", "1333.5", "2026-10-05"),
            )
            financeB.addTransaction(4_200, TransactionType.EXPENSE, LocalDate.parse("2026-10-04"), "TRANSPORT", "QA 파트너 지출", "상대 선택")

            val invite = repoA.createInvite()
            assertEquals(1, repoA.refresh().memberCount)
            val joined = repoB.join(requireNotNull(invite.invite).code)
            assertEquals(2, joined.memberCount)
            assertEquals(2, repoA.refresh().memberCount)

            val relationshipStart = LocalDate.parse("2023-05-21")
            repoA.saveSettings(SharedLedgerSettings(SharedMoneyMode.SHARED_FUND, relationshipStart))
            assertEquals(relationshipStart, repoB.refresh().settings.relationshipStartDate)

            val selectedPhotoBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
            val photoSnapshot = repoA.addMemoryPhoto("선택한 QA 사진", "임의 테스트 메모", LocalDate.parse("2026-10-12"), selectedPhotoBytes)
            assertEquals(1, photoSnapshot.memories.size)
            val featuredPhotoId = photoSnapshot.memories.single().id
            repoA.saveSettings(SharedLedgerSettings(SharedMoneyMode.SHARED_FUND, relationshipStart, featuredPhotoId))
            assertEquals(featuredPhotoId, repoB.refresh().settings.featuredMemoryPhotoId)
            val partnerPhoto = repoB.refresh().memories.single()
            assertEquals("선택한 QA 사진", partnerPhoto.title)
            assertEquals("임의 테스트 메모", partnerPhoto.memo)
            assertEquals(selectedPhotoBytes.toList(), repoB.downloadMemoryPhoto(partnerPhoto).toList())

            val aRows = dbA.financeDao().allTransactionsForBackup().associateBy { it.merchant }
            val aSharedId = aRows.getValue("QA 공유 지출").id
            val aPrivateId = aRows.getValue("QA 개인 수입").id
            val aForeignId = aRows.getValue("QA 외화 지출").id
            val bRows = dbB.financeDao().allTransactionsForBackup().associateBy { it.merchant }
            val bSharedId = bRows.getValue("QA 파트너 지출").id

            val afterShareA = repoA.shareTransactions(listOf(aSharedId, aForeignId))
            assertEquals(2, afterShareA.memberCount)
            assertEquals(setOf("QA 공유 지출", "QA 외화 지출"), repoB.refresh().transactions.map { it.merchant }.toSet())
            assertFalse(server.rows.values.flatMap { it.values }.any { it.merchant == "QA 개인 수입" })
            val remoteForeign = server.rows.values.flatMap { it.values }.single { it.merchant == "QA 외화 지출" }
            assertEquals("USD", remoteForeign.currency)
            assertEquals("10.00", remoteForeign.originalAmount)
            assertEquals("1333.5", remoteForeign.exchangeRate)
            assertEquals("2026-10-05", remoteForeign.exchangeRateDate)

            repoB.shareTransactions(listOf(bSharedId))
            val roundTripA = repoA.refresh()
            val roundTripB = repoB.refresh()
            assertEquals(setOf("QA 공유 지출", "QA 외화 지출", "QA 파트너 지출"), roundTripA.transactions.map { it.merchant }.toSet())
            assertEquals(setOf("QA 공유 지출", "QA 외화 지출", "QA 파트너 지출"), roundTripB.transactions.map { it.merchant }.toSet())
            val receivedFx = roundTripB.transactions.single { it.merchant == "QA 외화 지출" }
            assertEquals(13_335L, receivedFx.amount)
            assertEquals("USD", receivedFx.currency)
            assertEquals("10.00", receivedFx.originalAmount)
            assertEquals("1333.5", receivedFx.exchangeRateToKrw)
            assertEquals("2026-10-05", receivedFx.exchangeRateDate)
            assertNotNull(dbA.financeDao().getTransaction(aPrivateId))
            assertEquals("PRIVATE", dbA.financeDao().getTransaction(aPrivateId)!!.sharingScope)
            assertEquals("SHARED", dbA.financeDao().getTransaction(aSharedId)!!.sharingScope)
            assertEquals("SHARED", dbB.financeDao().getTransaction(bSharedId)!!.sharingScope)
            assertNull(repoUnrelated.refresh().ledgerId)
            assertTrue(repoUnrelated.refresh().transactions.isEmpty())
            assertTrue(repoUnrelated.refresh().memories.isEmpty())
            val afterPhotoDelete = repoA.deleteMemoryPhoto(partnerPhoto)
            assertTrue(afterPhotoDelete.memories.isEmpty())
            assertNull(afterPhotoDelete.settings.featuredMemoryPhotoId)
            assertTrue(repoB.refresh().memories.isEmpty())
            assertNull(repoB.refresh().settings.featuredMemoryPhotoId)
        } finally {
            dbA.close()
            dbB.close()
            dbUnrelated.close()
        }
    }

    private suspend fun authFor(userId: String): AuthRepository {
        val api = SyntheticAuthApi(userId)
        return AuthRepository(api, InMemorySessionStore()).also {
            it.initialize()
            check(it.login("${userId.take(6)}@example.invalid", "qa-password-only").isSuccess)
        }
    }

    private class InMemorySessionStore : SessionStore {
        private var value: AuthSession? = null
        override fun read() = value
        override fun write(session: AuthSession) { value = session }
        override fun clear() { value = null }
    }

    private class SyntheticAuthApi(private val userId: String) : AuthApi {
        override val configured = true
        private fun session() = AuthSession(
            SignedInUser(userId, "${userId.take(6)}@example.invalid"),
            "qa-access-$userId", "qa-refresh-$userId", Long.MAX_VALUE,
        )
        override suspend fun login(email: String, password: String) = session()
        override suspend fun signup(email: String, password: String): AuthSession? = null
        override suspend fun resendSignup(email: String) = Unit
        override suspend fun recover(email: String) = Unit
        override suspend fun verify(email: String, code: String, recovery: Boolean) = session()
        override suspend fun updatePassword(session: AuthSession, password: String) = Unit
        override suspend fun refresh(refreshToken: String) = session()
        override suspend fun logout(accessToken: String) = Unit
    }

    private class InMemorySharedLedgerGateway : SharedLedgerGateway {
        private val memberships = mutableMapOf<String, String>()
        private val members = mutableMapOf<String, MutableSet<String>>()
        private val invites = mutableMapOf<String, String>()
        val rows = mutableMapOf<String, MutableMap<String, RemoteSharedTransaction>>()
        private val photos = mutableMapOf<String, MutableMap<String, SharedMemoryPhoto>>()
        private val photoBytes = mutableMapOf<String, ByteArray>()
        private val uploadedPhotoPaths = mutableSetOf<String>()
        private val settings = mutableMapOf<String, SharedLedgerSettings>()
        private fun user(token: String): String = token.removePrefix("qa-access-").also {
            require(it != token && it.isNotBlank()) { "Unknown synthetic identity" }
        }
        private fun member(token: String, ledgerId: String) {
            check(memberships[user(token)] == ledgerId) { "This synthetic account is not a ledger member." }
        }
        override suspend fun findLedger(token: String, userId: String): String? {
            check(user(token) == userId)
            return memberships[userId]
        }
        override suspend fun countMembers(token: String, ledgerId: String): Int { member(token, ledgerId); return members[ledgerId]?.size ?: 0 }
        override suspend fun fetchGoals(token: String, ledgerId: String): List<SharedLedgerGoal> { member(token, ledgerId); return emptyList() }
        override suspend fun saveGoal(token: String, ledgerId: String, ownerId: String, goalId: String?, title: String, targetAmount: Long, currentAmount: Long, monthKey: String) = member(token, ledgerId)
        override suspend fun deleteGoal(token: String, ledgerId: String, goalId: String) = member(token, ledgerId)
        override suspend fun fetchFinanceItems(token: String, ledgerId: String): List<SharedFinanceItem> { member(token, ledgerId); return emptyList() }
        override suspend fun saveFinanceItem(token: String, ledgerId: String, ownerId: String, item: SharedFinanceItem) = member(token, ledgerId)
        override suspend fun deleteFinanceItem(token: String, ledgerId: String, itemId: String) = member(token, ledgerId)
        override suspend fun fetchMemories(token: String, ledgerId: String): List<SharedMemoryPhoto> {
            member(token, ledgerId)
            return photos[ledgerId]?.values?.filter { it.objectPath in uploadedPhotoPaths }?.toList().orEmpty()
        }
        override suspend fun reserveMemory(token: String, ledgerId: String, ownerId: String, id: String, title: String, memo: String, date: LocalDate, path: String, sizeBytes: Long) {
            member(token, ledgerId)
            check(user(token) == ownerId)
            require(sizeBytes in 1..SharedLedgerRepository.MAX_MEMORY_PHOTO_BYTES.toLong())
            val rows = photos.getOrPut(ledgerId) { mutableMapOf() }
            check(rows.size < SharedLedgerRepository.MAX_MEMORY_PHOTOS)
            check(rows.values.sumOf { it.sizeBytes } + sizeBytes <= SharedLedgerRepository.MAX_MEMORY_BYTES)
            check(id !in rows)
            rows[id] = SharedMemoryPhoto(id, ledgerId, ownerId, title, memo, date, path, sizeBytes)
        }
        override suspend fun uploadMemoryObject(token: String, path: String, bytes: ByteArray) {
            val photo = photos.values.asSequence().flatMap { it.values.asSequence() }.single { it.objectPath == path }
            member(token, photo.ledgerId)
            photoBytes[path] = bytes.copyOf()
        }
        override suspend fun markMemoryUploaded(token: String, ledgerId: String, id: String) {
            member(token, ledgerId)
            val photo = requireNotNull(photos[ledgerId]?.get(id))
            check(photo.objectPath in photoBytes)
            uploadedPhotoPaths += photo.objectPath
        }
        override suspend fun downloadMemoryObject(token: String, path: String): ByteArray {
            val photo = photos.values.asSequence().flatMap { it.values.asSequence() }.single { it.objectPath == path }
            member(token, photo.ledgerId)
            check(path in uploadedPhotoPaths)
            return requireNotNull(photoBytes[path]).copyOf()
        }
        override suspend fun deleteMemoryObject(token: String, path: String) {
            val photo = photos.values.asSequence().flatMap { it.values.asSequence() }.single { it.objectPath == path }
            member(token, photo.ledgerId)
            photoBytes.remove(path)
            uploadedPhotoPaths.remove(path)
        }
        override suspend fun deleteMemory(token: String, ledgerId: String, id: String) {
            member(token, ledgerId)
            photos[ledgerId]?.remove(id)?.let {
                photoBytes.remove(it.objectPath)
                uploadedPhotoPaths.remove(it.objectPath)
            }
        }
        override suspend fun fetchSettings(token: String, ledgerId: String): SharedLedgerSettings {
            member(token, ledgerId)
            return settings[ledgerId] ?: SharedLedgerSettings()
        }
        override suspend fun saveSettings(token: String, ledgerId: String, userId: String, settings: SharedLedgerSettings) {
            member(token, ledgerId)
            check(user(token) == userId)
            this.settings[ledgerId] = settings
        }
        override suspend fun leave(token: String): String? {
            val account = user(token)
            val ledger = memberships.remove(account) ?: return null
            members[ledger]?.remove(account)
            return ledger
        }
        override suspend fun createInvite(token: String): SharedLedgerInvite {
            val account = user(token)
            val ledger = memberships[account] ?: UUID.randomUUID().toString().also {
                memberships[account] = it
                members.getOrPut(it) { mutableSetOf() }.add(account)
            }
            val code = "QA${UUID.randomUUID().toString().replace("-", "").take(18).uppercase()}"
            invites[code] = ledger
            return SharedLedgerInvite(code, "2099-01-01T00:00:00Z")
        }
        override suspend fun join(token: String, code: String) {
            val account = user(token)
            val ledger = invites[code] ?: error("No synthetic invite")
            val ledgerMembers = members.getValue(ledger)
            check(ledgerMembers.size < 2 || account in ledgerMembers)
            memberships[account] = ledger
            ledgerMembers.add(account)
        }
        override suspend fun upsertTransactions(token: String, ledgerId: String, ownerId: String, rows: List<TransactionEntity>) {
            member(token, ledgerId)
            check(user(token) == ownerId)
            rows.filter { it.type in setOf("EXPENSE", "INCOME") && it.sharingScope == "SHARED" }.forEach { row ->
                val id = UUID.fromString(requireNotNull(row.cloudId)).toString()
                this.rows.getOrPut(ledgerId) { mutableMapOf() }[id] = RemoteSharedTransaction(
                    ledgerId = ledgerId, transactionId = id, ownerId = ownerId, type = row.type, amount = row.amount,
                    occurredAt = row.occurredAt, timezone = row.timezone, categoryKey = row.categoryKey,
                    merchant = row.merchant, memo = row.memo, paymentMethod = row.paymentMethod,
                    updatedAt = row.updatedAt, deletedAt = row.deletedAt, currency = row.currency,
                    originalAmount = row.originalAmount, exchangeRate = row.exchangeRate, exchangeRateDate = row.exchangeRateDate,
                )
            }
        }
        override suspend fun fetchTransactions(token: String, ledgerId: String): List<RemoteSharedTransaction> {
            member(token, ledgerId)
            return rows[ledgerId]?.values?.toList().orEmpty()
        }
        override suspend fun deleteTransaction(token: String, ledgerId: String, transactionId: String, ownerId: String) {
            member(token, ledgerId)
            check(user(token) == ownerId)
            rows[ledgerId]?.remove(transactionId)
        }
    }
}
