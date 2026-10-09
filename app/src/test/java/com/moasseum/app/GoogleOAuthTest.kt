package com.moasseum.app

import com.moasseum.app.auth.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

class GoogleOAuthTest {
    private val redirect = "com.moasseum.app.qa://auth/callback"
    private class Store : SessionStore {
        var value: AuthSession? = null
        override fun read() = value
        override fun write(session: AuthSession) { value = session }
        override fun clear() { value = null }
    }
    private class Pending : PendingAuthStore {
        var value: PendingAuthFlow? = null
        override fun read() = value
        override fun write(flow: PendingAuthFlow) { value = flow }
        override fun clear() { value = null }
    }
    private class Api : OAuthAuthApi {
        override val configured = true
        var ready = true
        var flow: PendingAuthFlow? = null
        var exchanges = 0
        var providers = setOf("google")
        var email = "selected-google@example.invalid"
        fun session() = AuthSession(SignedInUser("72a43f9d-00e0-44db-af34-4cf143314bb7", email, providers.firstOrNull()), "synthetic-access", "synthetic-refresh", 4600, providers)
        override suspend fun enabledSocialProviders() = if (ready) SocialOAuth.supportedProviders else emptySet()
        override suspend fun authorizationUrl(provider: String, flow: PendingAuthFlow): String {
            check(ready) { "Google provider not ready" }
            check(flow.provider == provider)
            this.flow = flow
            return SocialOAuth.authorizationUrl("https://auth.example.invalid", flow)
        }
        override suspend fun exchangeCode(code: String, verifier: String): AuthSession { exchanges++; require(verifier == flow!!.verifier); return session() }
        override suspend fun signupWithLink(email: String, password: String, flow: PendingAuthFlow): AuthSession? { this.flow = flow; return null }
        override suspend fun resendWithLink(email: String, flow: PendingAuthFlow) { this.flow = flow }
        override suspend fun recoverWithLink(email: String, flow: PendingAuthFlow) { this.flow = flow }
        override suspend fun login(email: String, password: String) = session()
        override suspend fun signup(email: String, password: String): AuthSession? = null
        override suspend fun resendSignup(email: String) {}
        override suspend fun recover(email: String) {}
        override suspend fun verify(email: String, code: String, recovery: Boolean) = session()
        override suspend fun updatePassword(session: AuthSession, password: String) {}
        override suspend fun refresh(refreshToken: String) = session()
        override suspend fun logout(accessToken: String) {}
    }
    private fun callback(flow: PendingAuthFlow) = "${flow.callbackUri}&code=synthetic-code"

    @Test fun googleAuthorizationUrlHasOnlyBasicScopesAndChallengeNotVerifierOrTokens() {
        val flow = PendingAuthFlow.create("", false, 1000, redirect, "google")
        val url = SocialOAuth.authorizationUrl("https://auth.example.invalid/", flow)
        val uri = URI(url)
        val values = uri.rawQuery.split('&').associate { val parts = it.split('=', limit = 2); parts[0] to URLDecoder.decode(parts[1], "UTF-8") }
        assertEquals("https", uri.scheme); assertEquals("auth.example.invalid", uri.host); assertEquals("/auth/v1/authorize", uri.path)
        assertEquals("google", values["provider"]); assertEquals(flow.callbackUri, values["redirect_to"])
        assertEquals(flow.challenge, values["code_challenge"]); assertEquals("s256", values["code_challenge_method"])
        assertEquals("openid email profile", values["scopes"]); assertEquals("select_account", values["prompt"])
        assertFalse(url.contains(flow.verifier)); assertFalse(values.containsKey("access_type")); assertFalse(values.containsKey("refresh_token")); assertFalse(values.containsKey("client_secret"))
    }
    @Test fun kakaoAuthorizationUrlRequestsOnlyAvailableOptionalProfileScopes() {
        val flow = PendingAuthFlow.create("", false, 1000, redirect, "kakao")
        val uri = URI(SocialOAuth.authorizationUrl("https://auth.example.invalid", flow))
        val values = uri.rawQuery.split('&').associate { val parts = it.split('=', limit = 2); parts[0] to URLDecoder.decode(parts[1], "UTF-8") }
        assertEquals("kakao", values["provider"])
        assertEquals("profile_nickname profile_image", values["scopes"])
        assertEquals(flow.callbackUri, values["redirect_to"])
        assertEquals(flow.challenge, values["code_challenge"])
        assertFalse(values.containsKey("prompt"))
        assertFalse(uri.toString().contains(flow.verifier))
        assertFalse(uri.toString().contains("client_secret"))
    }
    @Test fun rejectsUntrustedBaseUrlsAndUnsupportedFlows() {
        val flow = PendingAuthFlow.create("", false, 1000, redirect, "google")
        for (base in listOf("http://auth.example.invalid", "https://user@auth.example.invalid", "https://auth.example.invalid:443", "https://auth.example.invalid?x=1", "https://auth.example.invalid#fragment", "https://auth.example.invalid/other")) assertTrue(runCatching { SocialOAuth.authorizationUrl(base, flow) }.isFailure)
        assertTrue(runCatching { SocialOAuth.authorizationUrl("https://auth.example.invalid", PendingAuthFlow.create("qa@example.invalid", false, 1000, redirect)) }.isFailure)
        assertTrue(runCatching { PendingAuthFlow.create("qa@example.invalid", false, 1000, redirect, "google") }.isFailure)
        assertTrue(runCatching { PendingAuthFlow.create("", true, 1000, redirect, "google") }.isFailure)
        assertTrue(runCatching { PendingAuthFlow.create("", false, 1000, redirect, "facebook") }.isFailure)
    }
    @Test fun googleReturnSurvivesRestartAndAcceptsOnlyServerGoogleIdentity() = runBlocking {
        val api = Api(); val store = Store(); val pending = Pending()
        val repo = AuthRepository(api, store, pending, redirect) { 1000 }; repo.initialize()
        assertTrue(repo.beginSocialLogin("google").isSuccess)
        val flow = pending.value!!; assertEquals("google", flow.provider); assertEquals("", flow.email)
        assertNull(store.value); assertNull(repo.state.value.user)
        val restarted = AuthRepository(api, store, pending, redirect) { 1000 }; restarted.initialize()
        assertTrue(restarted.handleAuthCallback(callback(flow)).isSuccess)
        assertEquals("selected-google@example.invalid", store.value!!.user.email); assertEquals(setOf("google"), store.value!!.providers)
        assertNull(pending.value); assertTrue(restarted.handleAuthCallback(callback(flow)).isFailure); assertEquals(1, api.exchanges)
    }
    @Test fun wrongProviderNeverReplacesExistingSession() = runBlocking {
        val api = Api(); val store = Store(); val pending = Pending()
        store.value = AuthSession(SignedInUser("72a43f9d-00e0-44db-af34-4cf143314bb7", "existing@example.invalid"), "existing-access", "existing-refresh", 4600)
        val existing = store.value
        val repo = AuthRepository(api, store, pending, redirect) { 1000 }; repo.initialize(); repo.beginSocialLogin("google")
        api.providers = setOf("email")
        assertTrue(repo.handleAuthCallback(callback(pending.value!!)).isFailure)
        assertSame(existing, store.value); assertEquals("existing@example.invalid", repo.state.value.user!!.email)
    }
    @Test fun unconfiguredGoogleDoesNotDiscardPendingEmailVerification() = runBlocking {
        val api = Api(); val pending = Pending(); val repo = AuthRepository(api, Store(), pending, redirect) { 1000 }; repo.initialize()
        repo.signup("qa@example.invalid", "synthetic-password"); val emailFlow = pending.value
        api.ready = false
        assertTrue(repo.beginSocialLogin("google").isFailure); assertSame(emailFlow, pending.value); assertNull(repo.state.value.user)
    }
    @Test fun denialIsNotSuccessAndDoesNotEchoServerError() = runBlocking {
        val api = Api(); val pending = Pending(); val repo = AuthRepository(api, Store(), pending, redirect) { 1000 }; repo.initialize(); repo.beginSocialLogin("google")
        assertTrue(repo.handleAuthCallback("${pending.value!!.callbackUri}&error=access_denied&error_description=SECRET_VALUE").isFailure)
        assertFalse(repo.state.value.error!!.contains("SECRET_VALUE")); assertEquals(0, api.exchanges); assertNull(pending.value); assertNull(repo.state.value.user)
    }
    @Test fun cancellationClearsOnlyCurrentGoogleFlow() = runBlocking {
        val api = Api(); val pending = Pending(); val repo = AuthRepository(api, Store(), pending, redirect) { 1000 }; repo.initialize(); repo.beginSocialLogin("google")
        val first = pending.value!!; repo.cancelSocialLogin(); assertNull(pending.value)
        assertTrue(repo.handleAuthCallback(callback(first)).isFailure); assertEquals(0, api.exchanges)
        repo.signup("qa@example.invalid", "synthetic-password"); val emailFlow = pending.value
        repo.cancelSocialLogin(); assertSame(emailFlow, pending.value)
    }
    @Test fun expiredOrSupersededGoogleRequestCannotAuthenticate() = runBlocking {
        val api = Api(); val pending = Pending(); var clock = 1000L
        val repo = AuthRepository(api, Store(), pending, redirect) { clock }; repo.initialize(); repo.beginSocialLogin("google")
        val first = pending.value!!
        clock = 5000; assertTrue(repo.handleAuthCallback(callback(first)).isFailure); assertEquals(0, api.exchanges)
        clock = 1000; repo.beginSocialLogin("google"); assertNotEquals(first.flowId, pending.value!!.flowId)
        assertTrue(repo.handleAuthCallback(callback(first)).isFailure); assertEquals(0, api.exchanges)
    }
    @Test fun parsesProviderIdentityButNeverUsesGoogleProviderTokenAsSession() {
        val json = JSONObject().put("access_token", "synthetic-supabase-access").put("refresh_token", "synthetic-supabase-refresh").put("expires_at", 4600)
            .put("provider_token", "SYNTHETIC_GOOGLE_TOKEN").put("provider_refresh_token", "SYNTHETIC_GOOGLE_REFRESH")
            .put("user", JSONObject().put("id", "72a43f9d-00e0-44db-af34-4cf143314bb7").put("email", "selected@example.invalid").put("identities", org.json.JSONArray().put(JSONObject().put("provider", "google"))))
        val session = SupabaseAuthApi.parseSession(json, 1000)
        assertEquals(setOf("google"), session.providers); assertEquals("synthetic-supabase-access", session.accessToken); assertEquals("synthetic-supabase-refresh", session.refreshToken)
        assertEquals("AuthSession(redacted)", session.toString())
        json.getJSONObject("user").remove("identities"); assertTrue(SupabaseAuthApi.parseSession(json, 1000).providers.isEmpty())
    }
    @Test fun logoutAlsoInvalidatesPendingGoogleReturn() = runBlocking {
        val api = Api(); val pending = Pending(); val repo = AuthRepository(api, Store(), pending, redirect) { 1000 }; repo.initialize(); repo.beginSocialLogin("google")
        val flow = pending.value!!; assertTrue(repo.logout().isSuccess); assertNull(pending.value)
        assertTrue(repo.handleAuthCallback(callback(flow)).isFailure); assertEquals(0, api.exchanges)
    }
    @Test fun brokenPendingStorageDoesNotCrashDialogCancellation() = runBlocking {
        val broken = object : PendingAuthStore {
            override fun read(): PendingAuthFlow? = throw java.io.IOException("SYNTHETIC_PRIVATE_DIAGNOSTIC")
            override fun write(flow: PendingAuthFlow) {}
            override fun clear() {}
        }
        val repo = AuthRepository(Api(), Store(), broken, redirect) { 1000 }; repo.initialize()
        assertTrue(repo.cancelSocialLogin().isFailure)
        assertFalse(repo.state.value.error!!.contains("SYNTHETIC_PRIVATE_DIAGNOSTIC")); assertNull(repo.state.value.user)
    }

    @Test fun kakaoReturnRequiresVerifiedKakaoIdentity() = runBlocking {
        val api = Api(); api.providers = setOf("kakao")
        val store = Store(); val pending = Pending()
        val repo = AuthRepository(api, store, pending, redirect) { 1000 }; repo.initialize()
        assertTrue(repo.beginSocialLogin("kakao").isSuccess)
        assertEquals("kakao", pending.value!!.provider)
        assertTrue(repo.handleAuthCallback(callback(pending.value!!)).isSuccess)
        assertEquals(setOf("kakao"), store.value!!.providers)
        assertEquals("카카오 로그인 완료", repo.state.value.message)
    }

    @Test fun kakaoLoginAcceptsVerifiedAccountWithoutEmail() = runBlocking {
        val api = Api(); api.providers = setOf("kakao"); api.email = ""
        val store = Store(); val pending = Pending()
        val repo = AuthRepository(api, store, pending, redirect) { 1000 }; repo.initialize()
        assertTrue(repo.beginSocialLogin("kakao").isSuccess)
        assertTrue(repo.handleAuthCallback(callback(pending.value!!)).isSuccess)
        assertEquals("", store.value!!.user.email)
        assertEquals("kakao", store.value!!.user.provider)
        assertEquals("카카오 계정", store.value!!.user.displayLabel)
    }

    @Test fun supabaseSocialProviderAvailabilityUsesOnlyKnownEnabledProviders() {
        val settings = JSONObject().put("external", JSONObject().put("google", true).put("kakao", false).put("facebook", true))
        assertEquals(setOf("google"), SupabaseAuthApi.parseEnabledSocialProviders(settings))
        assertTrue(SupabaseAuthApi.parseEnabledSocialProviders(JSONObject().put("external", JSONObject().put("kakao", true))).contains("kakao"))
        assertTrue(SupabaseAuthApi.parseEnabledSocialProviders(JSONObject()).isEmpty())
    }

    @Test fun sessionParserAllowsMissingEmailOnlyForVerifiedKakaoIdentity() {
        fun response(provider: String?) = JSONObject()
            .put("access_token", "synthetic-access")
            .put("refresh_token", "synthetic-refresh")
            .put("expires_at", 4600)
            .put("user", JSONObject()
                .put("id", "72a43f9d-00e0-44db-af34-4cf143314bb7")
                .put("email", JSONObject.NULL)
                .put("identities", org.json.JSONArray().apply { provider?.let { put(JSONObject().put("provider", it)) } }))
        val kakao = SupabaseAuthApi.parseSession(response("kakao"), 1000)
        assertEquals("", kakao.user.email)
        assertEquals("kakao", kakao.user.provider)
        assertTrue(runCatching { SupabaseAuthApi.parseSession(response("google"), 1000) }.isFailure)
        assertTrue(runCatching { SupabaseAuthApi.parseSession(response(null), 1000) }.isFailure)
    }
}
