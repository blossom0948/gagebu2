package com.moasseum.app.auth

data class SignedInUser(val id: String, val email: String)
data class AuthState(val user: SignedInUser? = null, val initialized: Boolean = false, val busy: Boolean = false, val message: String? = null, val error: String? = null, val recoveryEmail: String? = null, val linkEvent: Long = 0)

class AuthSession(val user: SignedInUser, val accessToken: String, val refreshToken: String, val expiresAt: Long) {
    override fun toString(): String = "AuthSession(redacted)"
}

interface SessionStore {
    fun read(): AuthSession?
    fun write(session: AuthSession)
    fun clear()
}

interface AuthApi {
    val configured: Boolean
    suspend fun login(email: String, password: String): AuthSession
    suspend fun signup(email: String, password: String): AuthSession?
    suspend fun resendSignup(email: String)
    suspend fun recover(email: String)
    suspend fun verify(email: String, code: String, recovery: Boolean): AuthSession
    suspend fun updatePassword(session: AuthSession, password: String)
    suspend fun refresh(refreshToken: String): AuthSession
    suspend fun logout(accessToken: String)
}

interface AuthLinkApi : AuthApi {
    suspend fun signupWithLink(email: String, password: String, flow: PendingAuthFlow): AuthSession?
    suspend fun resendWithLink(email: String, flow: PendingAuthFlow)
    suspend fun recoverWithLink(email: String, flow: PendingAuthFlow)
    suspend fun exchangeCode(code: String, verifier: String): AuthSession
}

interface PendingAuthStore {
    fun read(): PendingAuthFlow?
    fun write(flow: PendingAuthFlow)
    fun clear()
}

class AuthHttpException(val status: Int, val code: String, message: String) : java.io.IOException(message)

fun validateCredentials(email: String, password: String? = null) {
    require(email.length <= 254 && email.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) { "이메일 주소를 확인해 주세요." }
    if (password != null) require(password.length in 8..256) { "비밀번호는 8~256자로 입력해 주세요." }
}
