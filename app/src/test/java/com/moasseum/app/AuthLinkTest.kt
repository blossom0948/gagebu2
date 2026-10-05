package com.moasseum.app

import com.moasseum.app.auth.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AuthLinkTest {
    private val redirect = "com.moasseum.app.qa://auth/callback"
    private val email = "qa@example.invalid"
    private class Store : SessionStore {
        var value: AuthSession? = null
        override fun read() = value
        override fun write(session: AuthSession) { value = session }
        override fun clear() { value = null }
    }
    private class PendingStore : PendingAuthStore {
        var value: PendingAuthFlow? = null
        override fun read() = value
        override fun write(flow: PendingAuthFlow) { value = flow }
        override fun clear() { value = null }
    }
    private class Api : AuthLinkApi {
        override val configured = true
        var flow: PendingAuthFlow? = null
        var exchangeCalls = 0
        var resultEmail = "qa@example.invalid"
        var passwordUpdated = false
        fun session() = AuthSession(SignedInUser("72a43f9d-00e0-44db-af34-4cf143314bb7", resultEmail), "synthetic-access", "synthetic-refresh", 4600)
        override suspend fun signupWithLink(email: String, password: String, flow: PendingAuthFlow): AuthSession? { this.flow = flow; return null }
        override suspend fun recoverWithLink(email: String, flow: PendingAuthFlow) { this.flow = flow }
        override suspend fun resendWithLink(email: String, flow: PendingAuthFlow) { this.flow = flow }
        override suspend fun exchangeCode(code: String, verifier: String): AuthSession { exchangeCalls++; require(verifier == flow!!.verifier); return session() }
        override suspend fun login(email: String, password: String) = session()
        override suspend fun signup(email: String, password: String): AuthSession? = null
        override suspend fun resendSignup(email: String) {}
        override suspend fun recover(email: String) {}
        override suspend fun verify(email: String, code: String, recovery: Boolean) = session()
        override suspend fun updatePassword(session: AuthSession, password: String) { passwordUpdated = true }
        override suspend fun refresh(refreshToken: String) = session()
        override suspend fun logout(accessToken: String) {}
    }
    @Test fun challengeMatchesRfc7636VectorAndRandomValuesAreUnpredictable() {
        val vector = PendingAuthFlow(email, "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "x".repeat(43), false, 1000, redirect)
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", vector.challenge)
        val a = PendingAuthFlow.create(email, false, 1000, redirect)
        val b = PendingAuthFlow.create(email, false, 1000, redirect)
        assertEquals(43, a.verifier.length); assertEquals(43, a.challenge.length)
        assertNotEquals(a.verifier, b.verifier); assertNotEquals(a.flowId, b.flowId)
        assertEquals("PendingAuthFlow(redacted)", a.toString())
    }
    @Test fun parsesOnlyExactReturnAddressAndCodeNotSuppliedSessionTokens() {
        val flow = PendingAuthFlow.create(email, false, 1000, redirect)
        val callback = "${flow.callbackUri}&code=synthetic-code"
        assertEquals("synthetic-code", AuthLink.parse(callback, redirect).code)
        val bad = listOf(callback.replace(".qa", ""), callback.replace("auth/callback", "other/callback"), callback.replace("auth/callback", "auth:9/callback"), callback + "&code=duplicate", callback + "#access_token=secret", callback + "&access_token=secret", callback + "&refresh_token=secret", callback + "&broken=%XX", "${flow.callbackUri}&code=" + "s".repeat(5000))
        for (link in bad) assertTrue(runCatching { AuthLink.parse(link, redirect) }.isFailure)
        assertEquals("AuthLink(redacted)", AuthLink.parse(callback, redirect).toString())
    }
    @Test fun signupLinkSurvivesRepositoryRestartAndCannotReplay() = runBlocking {
        val api = Api(); val store = Store(); val pending = PendingStore()
        val repo = AuthRepository(api, store, pending, redirect) { 1000 }
        repo.initialize(); assertTrue(repo.signup(email, "synthetic-password").isSuccess)
        assertNull(store.value); val link = "${pending.value!!.callbackUri}&code=synthetic-code"
        val restarted = AuthRepository(api, store, pending, redirect) { 1000 }; restarted.initialize()
        assertTrue(restarted.handleAuthCallback(link).isSuccess)
        assertEquals(email, store.value!!.user.email); assertNull(pending.value)
        assertTrue(restarted.handleAuthCallback(link).isFailure); assertEquals(1, api.exchangeCalls)
    }
    @Test fun recoveryLinkDoesNotSignInBeforePasswordUpdateAndCanBeCancelled() = runBlocking {
        val api = Api(); val store = Store(); val pending = PendingStore()
        val repo = AuthRepository(api, store, pending, redirect) { 1000 }; repo.initialize()
        repo.recover(email)
        assertTrue(repo.handleAuthCallback("${pending.value!!.callbackUri}&code=synthetic-code").isSuccess)
        assertNull(store.value); assertNull(repo.state.value.user); assertEquals(email, repo.state.value.recoveryEmail)
        repo.cancelRecovery(); assertTrue(repo.resetPassword("synthetic-password").isFailure); assertFalse(api.passwordUpdated)
        repo.recover(email); repo.handleAuthCallback("${pending.value!!.callbackUri}&code=another-code")
        assertTrue(repo.resetPassword("synthetic-password").isSuccess)
        assertTrue(api.passwordUpdated); assertEquals(email, repo.state.value.user!!.email); assertNull(repo.state.value.recoveryEmail)
    }
    @Test fun staleWrongNonceAndDifferentAccountNeverReplaceSession() = runBlocking {
        val api = Api(); val store = Store(); val pending = PendingStore(); var clock = 1000L
        val repo = AuthRepository(api, store, pending, redirect) { clock }; repo.initialize(); repo.recover(email)
        val flow = pending.value!!
        assertTrue(repo.handleAuthCallback("$redirect?flow_id=${"z".repeat(43)}&code=synthetic-code").isFailure)
        assertEquals(0, api.exchangeCalls)
        clock = 5000; assertTrue(repo.handleAuthCallback("${flow.callbackUri}&code=synthetic-code").isFailure); assertEquals(0, api.exchangeCalls)
        clock = 1000; api.resultEmail = "different@example.invalid"
        assertTrue(repo.handleAuthCallback("${flow.callbackUri}&code=synthetic-code").isFailure); assertNull(store.value); assertNull(repo.state.value.recoveryEmail)
    }
    @Test fun resendPreservesVerifierAndNewRequestSupersedesOldLink() = runBlocking {
        val api = Api(); val pending = PendingStore(); val repo = AuthRepository(api, Store(), pending, redirect) { 1000 }
        repo.initialize(); repo.signup(email, "synthetic-password"); val first = pending.value!!
        repo.resend(email); assertSame(first, pending.value); assertSame(first, api.flow)
        repo.recover(email)
        assertNotEquals(first.flowId, pending.value!!.flowId)
        assertTrue(repo.handleAuthCallback("${first.callbackUri}&code=synthetic-code").isFailure); assertEquals(0, api.exchangeCalls)
    }
    @Test fun serverErrorLinkIsNotEchoedAndNeverExchanged() = runBlocking {
        val api = Api(); val pending = PendingStore(); val repo = AuthRepository(api, Store(), pending, redirect) { 1000 }
        repo.initialize(); repo.signup(email, "synthetic-password")
        assertTrue(repo.handleAuthCallback("${pending.value!!.callbackUri}&error=access_denied&error_description=SECRET_TOKEN").isFailure)
        assertFalse(repo.state.value.error!!.contains("SECRET_TOKEN")); assertEquals(0, api.exchangeCalls); assertNull(pending.value)
    }
    @Test fun missingFlowShowsReturnLinkErrorWithoutInventingLogin() = runBlocking {
        val api = Api(); val repo = AuthRepository(api, Store(), PendingStore(), redirect) { 1000 }
        repo.initialize()
        assertTrue(repo.handleAuthCallback("$redirect?flow_id=${"a".repeat(43)}&code=synthetic-code").isFailure)
        assertEquals(1L, repo.state.value.linkEvent); assertNotNull(repo.state.value.error)
        assertNull(repo.state.value.user); assertEquals(0, api.exchangeCalls)
    }
}
