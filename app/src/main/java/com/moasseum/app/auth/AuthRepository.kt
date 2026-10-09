package com.moasseum.app.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AuthRepository(private val api: AuthApi, private val store: SessionStore, private val pendingStore: PendingAuthStore? = null, private val redirectUri: String? = null, private val now: () -> Long = { System.currentTimeMillis() / 1000 }) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()
    val configured: Boolean get() = api.configured
    val supportsEmailLinks: Boolean get() = api is AuthLinkApi && pendingStore != null && redirectUri != null
    val supportsSocialLogin: Boolean get() = api is OAuthAuthApi && supportsEmailLinks
    private var session: AuthSession? = null
    private var recoverySession: AuthSession? = null
    private suspend fun save(value: AuthSession) {
        withContext(Dispatchers.IO) { store.write(value) }
        session = value; recoverySession = null
        mutableState.value = mutableState.value.copy(user = value.user, initialized = true, recoveryEmail = null)
    }
    suspend fun initialize() = mutex.withLock {
        if (mutableState.value.initialized) return@withLock
        val result = withContext(Dispatchers.IO) { runCatching { store.read() } }
        session = result.getOrNull()
        if (result.isFailure) withContext(Dispatchers.IO) { store.clear() }
        mutableState.value = AuthState(user = session?.user, initialized = true, error = if (result.isFailure) "저장된 로그인 정보를 복구하지 못했어요. 다시 로그인해 주세요." else null)
    }
    private suspend fun action(block: suspend () -> Unit): Result<Unit> = mutex.withLock {
        mutableState.value = mutableState.value.copy(busy = true, message = null, error = null)
        try {
            check(configured) { "로그인 서버가 아직 연결되지 않았어요. 앱 전용 Supabase 설정이 필요합니다." }
            block(); Result.success(Unit)
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { mutableState.value = mutableState.value.copy(error = error.message ?: "로그인 작업에 실패했어요."); Result.failure(error) }
        finally { mutableState.value = mutableState.value.copy(busy = false, initialized = true) }
    }
    suspend fun login(email: String, password: String) = action {
        save(api.login(email.trim(), password)); clearPending(); message("로그인했어요.")
    }
    suspend fun refreshSocialProviders() {
        if (!supportsSocialLogin || !configured) {
            mutableState.value = mutableState.value.copy(socialProvidersChecked = true)
            return
        }
        val providers = try {
            (api as OAuthAuthApi).enabledSocialProviders().intersect(SocialOAuth.supportedProviders)
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (_: Exception) { emptySet() }
        mutableState.value = mutableState.value.copy(socialProviders = providers, socialProvidersChecked = true)
    }
    suspend fun beginSocialLogin(provider: String): Result<String> {
        var url: String? = null
        val result = action {
            require(provider in SocialOAuth.supportedProviders) { "지원하지 않는 로그인 방식입니다." }
            check(supportsSocialLogin) { "이 버전에서는 소셜 로그인을 사용할 수 없어요." }
            val flow = PendingAuthFlow.create("", false, now(), redirectUri!!, provider = provider)
            url = (api as OAuthAuthApi).authorizationUrl(provider, flow)
            // Persist before browser launch, and only after provider availability is verified.
            withContext(Dispatchers.IO) { pendingStore!!.write(flow) }
            recoverySession = null
            mutableState.value = mutableState.value.copy(recoveryEmail = null)
            message("브라우저에서 ${SocialOAuth.displayName(provider)} 계정을 선택해 주세요. 완료하면 앱으로 돌아옵니다.")
        }
        return result.map { checkNotNull(url) }
    }
    suspend fun cancelSocialLogin(): Result<Unit> = mutex.withLock {
        try {
            if (readPending()?.provider in SocialOAuth.supportedProviders) clearPending()
            Result.success(Unit)
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) {
            mutableState.value = mutableState.value.copy(error = "저장된 인증 요청을 정리하지 못했어요. 앱에서 인증을 다시 시작해 주세요.")
            Result.failure(error)
        }
    }
    suspend fun signup(email: String, password: String) = action {
        val address = email.trim()
        validateCredentials(address, password)
        val result = if (supportsEmailLinks) (api as AuthLinkApi).signupWithLink(address, password, beginFlow(address, false)) else api.signup(address, password)
        if (result != null) { save(result); clearPending() }
        message(if (result == null) "인증 메일을 보냈어요. 이 휴대폰에서 메일의 확인 링크를 누르면 앱으로 돌아옵니다." else "가입하고 로그인했어요.")
    }
    suspend fun resend(email: String) = action {
        val address = email.trim()
        if (supportsEmailLinks) {
            val flow = readPending() ?: error("이 앱에서 회원가입을 다시 요청해 주세요.")
            require(flow.provider == null && !flow.recovery && flow.email.equals(address, true) && flow.isCurrent(now())) { "가입 인증이 만료됐어요. 회원가입을 다시 요청해 주세요." }
            (api as AuthLinkApi).resendWithLink(address, flow)
        } else api.resendSignup(address)
        message("가입 인증 메일을 다시 요청했어요.")
    }
    suspend fun verifySignup(email: String, code: String) = action { save(api.verify(email.trim(), code.trim(), false)); clearPending(); message("이메일 인증을 마쳤어요.") }
    suspend fun recover(email: String) = action {
        val address = email.trim(); validateCredentials(address)
        recoverySession = null
        mutableState.value = mutableState.value.copy(recoveryEmail = null)
        if (supportsEmailLinks) (api as AuthLinkApi).recoverWithLink(address, beginFlow(address, true)) else api.recover(address)
        message("등록된 이메일이라면 재설정 메일이 전송됩니다. 이 휴대폰에서 메일의 링크를 눌러 주세요.")
    }
    suspend fun verifyRecovery(email: String, code: String) = action {
        recoverySession = api.verify(email.trim(), code.trim(), true); clearPending()
        mutableState.value = mutableState.value.copy(recoveryEmail = recoverySession?.user?.email)
        message("인증됐어요. 새 비밀번호를 입력해 주세요.")
    }
    suspend fun handleAuthCallback(raw: String): Result<Unit> = action {
        // Only a locally initiated, unexpired PKCE flow can change the session.
        check(supportsEmailLinks) { "이 앱에서 인증을 다시 시작해 주세요." }
        val link = AuthLink.parse(raw, redirectUri!!)
        // A well-formed return link must also show an explanation when it is old
        // or the app has been reinstalled; silently opening Home looks like success.
        mutableState.value = mutableState.value.copy(linkEvent = mutableState.value.linkEvent + 1)
        val pending = readPending() ?: error("이 앱에서 먼저 로그인이나 인증 메일을 요청해 주세요. 다른 기기에서 시작한 인증은 사용할 수 없어요.")
        require(pending.flowId == link.flowId && pending.redirectUri == redirectUri) { "이 앱에서 시작한 최신 인증 요청을 완료해 주세요." }
        require(pending.isCurrent(now())) { "인증 요청이 만료됐어요. 이 앱에서 인증을 다시 시작해 주세요." }
        if (link.failed) { clearPending(); error(if (pending.provider in SocialOAuth.supportedProviders) "${SocialOAuth.displayName(pending.provider!!)} 로그인이 취소됐거나 완료되지 않았어요. 다시 시도해 주세요." else "인증 링크가 만료됐거나 이미 사용됐어요. 인증 메일을 다시 요청해 주세요.") }
        val verified = (api as AuthLinkApi).exchangeCode(link.code!!, pending.verifier)
        if (pending.provider in SocialOAuth.supportedProviders) {
            require(pending.provider in verified.providers) { "${SocialOAuth.displayName(pending.provider!!)} 인증 계정을 확인하지 못했어요. 다시 로그인해 주세요." }
            if (verified.user.email.isNotBlank()) validateCredentials(verified.user.email)
            else require(pending.provider == "kakao") { "카카오 계정 이메일을 확인할 수 없어요. 이메일 로그인을 사용해 주세요." }
        } else require(verified.user.email.equals(pending.email, true)) { "인증 계정이 일치하지 않아요. 인증 메일을 다시 요청해 주세요." }
        if (pending.recovery) {
            recoverySession = verified
            mutableState.value = mutableState.value.copy(recoveryEmail = verified.user.email)
            message("이메일 인증을 마쳤어요. 새 비밀번호를 입력해 주세요.")
        } else {
            save(verified); message(if (pending.provider in SocialOAuth.supportedProviders) "${SocialOAuth.displayName(pending.provider!!)} 로그인 완료" else "이메일 인증·로그인 완료")
        }
        clearPending()
    }
    suspend fun resetPassword(password: String) = action {
        val verified = recoverySession ?: error("먼저 재설정 메일의 링크나 인증 번호로 이메일을 확인해 주세요.")
        require(verified.expiresAt > now()) { "재설정 인증이 만료됐어요. 다시 인증해 주세요." }
        api.updatePassword(verified, password); save(verified); message("비밀번호를 변경했어요.")
    }
    suspend fun cancelRecovery() = mutex.withLock {
        recoverySession = null
        mutableState.value = mutableState.value.copy(recoveryEmail = null)
    }
    suspend fun logout(): Result<Unit> = mutex.withLock {
        val token = session?.accessToken
        try {
            withContext(Dispatchers.IO) { store.clear() }; clearPending(); session = null; recoverySession = null
            mutableState.value = AuthState(initialized = true, busy = true)
            val revoked = token == null || try { api.logout(token); true }
                catch (error: kotlinx.coroutines.CancellationException) { throw error }
                catch (_: Exception) { false }
            mutableState.value = AuthState(initialized = true, message = if (revoked) "로그아웃했어요. 기기 기록은 유지됩니다." else "이 기기에서 로그아웃했어요. 인터넷 연결 문제로 서버 세션 해제는 확인하지 못했습니다.")
            Result.success(Unit)
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { mutableState.value = mutableState.value.copy(error = "로그아웃 정보를 지우지 못했어요."); Result.failure(error) }
        finally { mutableState.value = mutableState.value.copy(busy = false) }
    }
    suspend fun validAccessToken(): String? = mutex.withLock {
        val existing = session ?: return@withLock null
        if (existing.expiresAt > now() + 60) return@withLock existing.accessToken
        try {
            val updated = api.refresh(existing.refreshToken)
            require(updated.user.id == existing.user.id) { "로그인 계정이 일치하지 않아요." }
            save(updated); updated.accessToken
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: AuthHttpException) {
            if (error.status in setOf(400, 401, 403)) {
                withContext(Dispatchers.IO) { store.clear() }; session = null
                mutableState.value = AuthState(initialized = true, error = "로그인이 만료됐어요. 다시 로그인해 주세요.")
            }
            existing.accessToken.takeIf { session != null && existing.expiresAt > now() }
        } catch (_: Exception) { existing.accessToken.takeIf { existing.expiresAt > now() } }
    }
    private fun message(value: String) { mutableState.value = mutableState.value.copy(message = value, error = null) }
    private suspend fun beginFlow(email: String, recovery: Boolean): PendingAuthFlow {
        val flow = PendingAuthFlow.create(email, recovery, now(), redirectUri!!)
        withContext(Dispatchers.IO) { pendingStore!!.write(flow) }
        return flow
    }
    private suspend fun readPending(): PendingAuthFlow? = withContext(Dispatchers.IO) {
        try { pendingStore?.read() }
        catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (_: Exception) { throw java.io.IOException("저장된 인증 요청을 확인하지 못했어요. 인증 메일을 다시 요청해 주세요.") }
    }
    private suspend fun clearPending() = withContext(Dispatchers.IO) { pendingStore?.clear() }
}
