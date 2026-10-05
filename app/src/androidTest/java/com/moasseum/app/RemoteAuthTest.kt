package com.moasseum.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moasseum.app.auth.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteAuthTest {
    @Test fun realSupabaseLoginRefreshRestartAndLogout() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName.endsWith(".qa"))
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Remote QA is explicitly opt-in", arguments.getString("remoteAuth") == "true")
        val email = arguments.getString("qaEmail") ?: error("Provide a synthetic QA email")
        val password = arguments.getString("qaPassword") ?: error("Provide ephemeral QA credentials")
        check(email.endsWith("@example.invalid"))
        val api = SupabaseAuthApi()
        assertTrue(api.configured)
        val store = EncryptedSessionStore(context)
        store.clear()
        try {
            val repo = AuthRepository(api, store)
            repo.initialize(); assertTrue("Remote login failed", repo.login(email, password).isSuccess)
            val original = store.read() ?: error("Session not saved")
            assertEquals(email, original.user.email)
            assertTrue(original.accessToken.isNotBlank())
            val refreshed = api.refresh(original.refreshToken)
            assertEquals(original.user.id, refreshed.user.id)
            store.write(refreshed)
            if (arguments.getString("requireWorkerAuth") == "true") {
                val client = com.moasseum.app.data.AiClient(bearerTokenProvider = { refreshed.accessToken })
                val candidate = client.parseTransaction("QA검사상점에서 1000원 지출", java.time.LocalDate.of(2026, 10, 5)).getOrThrow()
                assertEquals(com.moasseum.app.domain.AiCandidateSource.SERVER, candidate.source)
                assertEquals(1000L, candidate.amount)
                val withoutLogin = com.moasseum.app.data.AiClient().analyzeSpending(com.moasseum.app.domain.LedgerUiState(java.time.YearMonth.of(2026, 10), emptyList(), null))
                assertTrue("Unauthenticated AI request must fail", withoutLogin.isFailure)
            }
            val restarted = AuthRepository(api, EncryptedSessionStore(context))
            restarted.initialize(); assertEquals(email, restarted.state.value.user?.email)
            assertNotNull(restarted.validAccessToken())
            assertTrue(restarted.logout().isSuccess); assertNull(store.read())
            val rejected = runCatching { api.refresh(refreshed.refreshToken) }.exceptionOrNull()
            assertTrue("Logged-out refresh token must be revoked", rejected is AuthHttpException)
            val wrongPassword = runCatching { api.login(email, "this-is-not-the-qa-password") }.exceptionOrNull()
            assertTrue(wrongPassword is AuthHttpException)
        } finally { store.clear() }
    }
}
