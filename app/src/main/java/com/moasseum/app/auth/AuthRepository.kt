package com.moasseum.app.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AuthRepository(private val api: AuthApi, private val store: SessionStore, private val now: () -> Long = { System.currentTimeMillis() / 1000 }) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(AuthState())
    val state = mutableState.asStateFlow()
    val configured: Boolean get() = api.configured
    private var session: AuthSession? = null
    private var recoverySession: AuthSession? = null
    private suspend fun save(value: AuthSession) {
        withContext(Dispatchers.IO) { store.write(value) }
        session = value; recoverySession = null
        mutableState.value = mutableState.value.copy(user = value.user, initialized = true)
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
    suspend fun login(email: String, password: String) = action { save(api.login(email.trim(), password)); message("로그인했어요. 기기 기록은 자동 공유되지 않아요.") }
    suspend fun signup(email: String, password: String) = action {
        val result = api.signup(email.trim(), password)
        if (result != null) save(result)
        message(if (result == null) "인증 메일을 보냈어요. 메일의 인증 번호를 입력하거나 확인 링크를 누른 뒤 로그인해 주세요." else "가입하고 로그인했어요.")
    }
    suspend fun resend(email: String) = action { api.resendSignup(email.trim()); message("가입 인증 메일을 다시 요청했어요.") }
    suspend fun verifySignup(email: String, code: String) = action { save(api.verify(email.trim(), code.trim(), false)); message("이메일 인증을 마쳤어요.") }
    suspend fun recover(email: String) = action { recoverySession = null; api.recover(email.trim()); message("등록된 이메일이라면 재설정 인증 메일이 전송됩니다.") }
    suspend fun verifyRecovery(email: String, code: String) = action { recoverySession = api.verify(email.trim(), code.trim(), true); message("인증됐어요. 새 비밀번호를 입력해 주세요.") }
    suspend fun resetPassword(password: String) = action {
        val verified = recoverySession ?: error("먼저 재설정 인증 번호를 확인해 주세요.")
        require(verified.expiresAt > now()) { "재설정 인증이 만료됐어요. 다시 인증해 주세요." }
        api.updatePassword(verified, password); save(verified); message("비밀번호를 변경했어요.")
    }
    suspend fun logout(): Result<Unit> = mutex.withLock {
        val token = session?.accessToken
        try {
            withContext(Dispatchers.IO) { store.clear() }; session = null; recoverySession = null
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
}
