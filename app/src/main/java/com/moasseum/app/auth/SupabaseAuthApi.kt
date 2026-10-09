package com.moasseum.app.auth

import com.moasseum.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SupabaseAuthApi(
    private val baseUrl: String = BuildConfig.SUPABASE_URL,
    private val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
) : OAuthAuthApi {
    override val configured: Boolean get() = baseUrl.isNotBlank() && publishableKey.isNotBlank()
    override suspend fun login(email: String, password: String): AuthSession {
        validateCredentials(email, password)
        return parseSession(request("token?grant_type=password", JSONObject().put("email", email).put("password", password)))
    }
    override suspend fun signup(email: String, password: String): AuthSession? {
        validateCredentials(email, password)
        val response = request("signup", JSONObject().put("email", email).put("password", password))
        return if (response.optString("access_token").isBlank()) null else parseSession(response)
    }
    override suspend fun signupWithLink(email: String, password: String, flow: PendingAuthFlow): AuthSession? {
        validateCredentials(email, password)
        val response = request(redirectPath("signup", flow), challengeBody(flow).put("email", email).put("password", password))
        return if (response.optString("access_token").isBlank()) null else parseSession(response)
    }
    override suspend fun resendWithLink(email: String, flow: PendingAuthFlow) {
        validateCredentials(email)
        request(redirectPath("resend", flow), JSONObject().put("email", email).put("type", "signup"))
    }
    override suspend fun recoverWithLink(email: String, flow: PendingAuthFlow) {
        validateCredentials(email)
        request(redirectPath("recover", flow), challengeBody(flow).put("email", email))
    }
    override suspend fun exchangeCode(code: String, verifier: String): AuthSession {
        require(code.matches(Regex("[A-Za-z0-9._~-]{1,512}")) && verifier.matches(Regex("[A-Za-z0-9_-]{43}"))) { "인증 링크를 다시 요청해 주세요." }
        return parseSession(request("token?grant_type=pkce", JSONObject().put("auth_code", code).put("code_verifier", verifier)))
    }
    override suspend fun enabledSocialProviders(): Set<String> {
        val settings = request("settings", method = "GET")
        return parseEnabledSocialProviders(settings)
    }
    override suspend fun authorizationUrl(provider: String, flow: PendingAuthFlow): String {
        require(provider in SocialOAuth.supportedProviders && flow.provider == provider) { "지원하지 않는 로그인 방식입니다." }
        check(provider in enabledSocialProviders()) {
            if (provider == "kakao") "카카오 로그인 서버 설정이 아직 준비되지 않았어요. 이메일 로그인은 계속 사용할 수 있습니다."
            else "구글 로그인 서버 설정이 아직 준비되지 않았어요. 이메일 로그인은 계속 사용할 수 있습니다."
        }
        return SocialOAuth.authorizationUrl(baseUrl, flow)
    }
    private fun redirectPath(path: String, flow: PendingAuthFlow): String = "$path?redirect_to=${java.net.URLEncoder.encode(flow.callbackUri, "UTF-8")}"
    private fun challengeBody(flow: PendingAuthFlow) = JSONObject().put("code_challenge", flow.challenge).put("code_challenge_method", "s256")
    override suspend fun resendSignup(email: String) { validateCredentials(email); request("resend", JSONObject().put("email", email).put("type", "signup")) }
    override suspend fun recover(email: String) { validateCredentials(email); request("recover", JSONObject().put("email", email)) }
    override suspend fun verify(email: String, code: String, recovery: Boolean): AuthSession {
        validateCredentials(email)
        require(code.matches(Regex("[0-9]{6,10}"))) { "메일에 있는 인증 번호를 입력해 주세요." }
        return parseSession(request("verify", JSONObject().put("email", email).put("token", code).put("type", if (recovery) "recovery" else "signup")))
    }
    override suspend fun updatePassword(session: AuthSession, password: String) {
        validateCredentials(session.user.email, password)
        request("user", JSONObject().put("password", password), method = "PUT", token = session.accessToken)
    }
    override suspend fun refresh(refreshToken: String): AuthSession = parseSession(request("token?grant_type=refresh_token", JSONObject().put("refresh_token", refreshToken)))
    override suspend fun logout(accessToken: String) { request("logout?scope=local", JSONObject(), token = accessToken) }

    private suspend fun request(path: String, body: JSONObject? = null, method: String = "POST", token: String? = null): JSONObject = withContext(Dispatchers.IO) {
        check(configured) { "로그인 서버가 아직 연결되지 않았어요. 앱 전용 Supabase 프로젝트 설정이 필요합니다." }
        val endpoint = URL("${baseUrl.trimEnd('/')}/auth/v1/$path")
        require(endpoint.protocol == "https" && endpoint.userInfo == null) { "로그인 서버는 HTTPS 주소만 사용할 수 있어요." }
        val connection = endpoint.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method; connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.instanceFollowRedirects = false; connection.doOutput = body != null
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", publishableKey)
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.use {
                val bytes = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    if (bytes.size() + count > 1_048_576) throw java.io.IOException("인증 응답이 너무 큽니다.")
                    bytes.write(buffer, 0, count)
                }
                bytes.toString(Charsets.UTF_8.name())
            }.orEmpty()
            if (status !in 200..299) {
                val code = runCatching { JSONObject(text).optString("error_code").ifBlank { JSONObject(text).optString("code") } }.getOrDefault("")
                val message = when {
                    status == 429 -> "요청이 많아요. 잠시 후 다시 시도해 주세요."
                    code == "email_not_confirmed" -> "이메일 인증 후 로그인해 주세요."
                    code == "weak_password" -> "더 안전한 비밀번호를 입력해 주세요."
                    code == "otp_expired" -> "인증 번호가 만료됐거나 올바르지 않아요. 다시 요청해 주세요."
                    code == "invalid_credentials" -> "이메일 또는 비밀번호가 올바르지 않아요."
                    code == "user_already_exists" -> "이미 등록된 계정입니다. 로그인해 주세요."
                    code == "email_address_not_authorized" -> "현재 무료 기본 메일은 서버 조직에 등록된 이메일로만 보낼 수 있어요. 본인용 이메일을 사용해 주세요."
                    code in setOf("flow_state_expired", "flow_state_not_found", "bad_code_verifier") -> "인증이 만료됐거나 다른 기기에서 시작됐어요. 이 앱에서 인증을 다시 시작해 주세요."
                    status in 500..599 -> "로그인 서버가 잠시 응답하지 않아요."
                    else -> "인증 요청을 완료하지 못했어요. 입력 내용과 서버 설정을 확인해 주세요."
                }
                throw AuthHttpException(status, code, message)
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (error: AuthHttpException) { throw error }
        catch (_: org.json.JSONException) { throw java.io.IOException("로그인 서버의 응답 형식이 올바르지 않아요.") }
        catch (error: java.io.IOException) { throw java.io.IOException("로그인 서버에 연결하지 못했어요. 인터넷 연결을 확인해 주세요.", error) }
        finally { connection.disconnect() }
    }

    companion object {
        internal fun parseEnabledSocialProviders(settings: JSONObject): Set<String> {
            val external = settings.optJSONObject("external") ?: return emptySet()
            return SocialOAuth.supportedProviders.filterTo(mutableSetOf()) { external.optBoolean(it, false) }
        }
        internal fun parseSession(response: JSONObject, now: Long = System.currentTimeMillis() / 1000): AuthSession {
            try {
            val user = response.getJSONObject("user")
            val id = user.getString("id").also { java.util.UUID.fromString(it) }
            val providers = buildSet {
                user.optJSONArray("identities")?.let { identities ->
                    for (index in 0 until identities.length()) identities.optJSONObject(index)?.optString("provider")?.takeIf { it.isNotBlank() }?.let(::add)
                }
                user.optJSONObject("app_metadata")?.optString("provider")
                    ?.takeIf { it in SocialOAuth.supportedProviders }?.let(::add)
            }
            val email = user.opt("email")?.takeIf { it != JSONObject.NULL }?.toString()?.trim().orEmpty()
            if (email.isBlank()) require("kakao" in providers) else validateCredentials(email)
            val access = response.getString("access_token"); val refresh = response.getString("refresh_token")
            require(access.isNotBlank() && refresh.isNotBlank())
            val expires = if (response.has("expires_at")) response.getLong("expires_at") else now + response.getLong("expires_in")
            require(expires > now && expires - now <= 7 * 86400)
            val primaryProvider = if (email.isBlank()) "kakao" else providers.firstOrNull { it in SocialOAuth.supportedProviders }
            return AuthSession(SignedInUser(id, email, primaryProvider), access, refresh, expires, providers)
            } catch (_: Exception) {
                // Parser diagnostics can contain the entire response, including tokens.
                throw IllegalArgumentException("로그인 서버의 세션 응답을 확인하지 못했어요. 다시 시도해 주세요.")
            }
        }
    }
}
