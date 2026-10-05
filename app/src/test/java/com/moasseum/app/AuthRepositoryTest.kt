package com.moasseum.app

import com.moasseum.app.auth.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AuthRepositoryTest {
    private class Store : SessionStore {
        var value: AuthSession? = null
        override fun read() = value
        override fun write(session: AuthSession) { value = session }
        override fun clear() { value = null }
    }
    private class Api : AuthApi {
        override var configured = true
        var refreshCalls = 0
        var refreshError: Exception? = null
        var expiry = 1600L
        var updated = false
        fun session() = AuthSession(SignedInUser("72a43f9d-00e0-44db-af34-4cf143314bb7", "qa@example.invalid"), "synthetic-access", "synthetic-refresh", expiry)
        override suspend fun login(email: String, password: String) = session()
        override suspend fun signup(email: String, password: String): AuthSession? = null
        override suspend fun resendSignup(email: String) {}
        override suspend fun recover(email: String) {}
        override suspend fun verify(email: String, code: String, recovery: Boolean) = session()
        override suspend fun updatePassword(session: AuthSession, password: String) { updated = true }
        override suspend fun refresh(refreshToken: String): AuthSession { refreshCalls++; refreshError?.let { throw it }; expiry = 2600; return session() }
        override suspend fun logout(accessToken: String) {}
    }
    @Test fun loginAndRestartRetainSessionButNotPassword() = runBlocking {
        val api = Api(); val store = Store(); val repo = AuthRepository(api, store) { 1000 }
        repo.initialize(); assertTrue(repo.login("qa@example.invalid", "not-a-real-password").isSuccess)
        val restarted = AuthRepository(api, store) { 1000 }; restarted.initialize()
        assertEquals("qa@example.invalid", restarted.state.value.user!!.email)
        assertEquals("synthetic-access", restarted.validAccessToken()); assertEquals("AuthSession(redacted)", store.value.toString())
    }
    @Test fun signupAwaitingConfirmationDoesNotInventSession() = runBlocking {
        val repo = AuthRepository(Api(), Store()) { 1000 }; repo.initialize(); repo.signup("qa@example.invalid", "password123")
        assertNull(repo.state.value.user); assertTrue(repo.state.value.message!!.contains("인증"))
    }
    @Test fun resetRequiresVerifiedRecoverySession() = runBlocking {
        val api = Api(); val repo = AuthRepository(api, Store()) { 1000 }; repo.initialize()
        assertTrue(repo.resetPassword("password123").isFailure); assertFalse(api.updated)
        assertTrue(repo.verifyRecovery("qa@example.invalid", "123456").isSuccess)
        assertTrue(repo.resetPassword("password123").isSuccess); assertTrue(api.updated)
    }
    @Test fun concurrentRefreshIsSerialized() = runBlocking {
        val api = Api().apply { expiry = 1020 }; val repo = AuthRepository(api, Store()) { 1000 }
        repo.initialize(); repo.login("qa@example.invalid", "password123")
        coroutineScope { (1..20).map { async { repo.validAccessToken() } }.awaitAll() }
        assertEquals(1, api.refreshCalls)
    }
    @Test fun rejectedRefreshClearsExpiredSession() = runBlocking {
        val api = Api().apply { expiry = 1020; refreshError = AuthHttpException(400, "refresh_token_not_found", "expired") }
        val store = Store(); val repo = AuthRepository(api, store) { 1000 }; repo.initialize(); repo.login("qa@example.invalid", "password123")
        assertNull(repo.validAccessToken()); assertNull(store.value); assertNull(repo.state.value.user)
    }
    @Test fun offlineRefreshKeepsSessionForRetryButDoesNotReturnExpiredAccessToken() = runBlocking {
        val api = Api().apply { expiry = 900; refreshError = java.io.IOException("offline") }; val store = Store()
        val repo = AuthRepository(api, store) { 1000 }; repo.initialize(); repo.login("qa@example.invalid", "password123")
        assertNull(repo.validAccessToken()); assertNotNull(store.value)
    }
    @Test fun logoutRemovesPersistedSession() = runBlocking {
        val store = Store(); val repo = AuthRepository(Api(), store) { 1000 }; repo.initialize(); repo.login("qa@example.invalid", "password123"); repo.logout()
        assertNull(store.value); assertNull(repo.state.value.user)
    }
    @Test fun missingConfigurationIsExplicitAndNotFakeSuccess() = runBlocking {
        val repo = AuthRepository(Api().apply { configured = false }, Store()); repo.initialize()
        assertTrue(repo.login("qa@example.invalid", "password123").isFailure); assertNull(repo.state.value.user)
    }
    @Test(expected = IllegalArgumentException::class) fun badEmailRejected() { validateCredentials("not-an-email", "password123") }
    @Test(expected = IllegalArgumentException::class) fun shortPasswordRejected() { validateCredentials("qa@example.invalid", "abc") }
}
