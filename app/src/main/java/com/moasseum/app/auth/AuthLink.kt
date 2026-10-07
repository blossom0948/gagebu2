package com.moasseum.app.auth

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

// No passwords or tokens in the callback URL. The random verifier stays on this device.
class PendingAuthFlow(val email: String, val verifier: String, val flowId: String, val recovery: Boolean, val createdAt: Long, val redirectUri: String, val provider: String? = null) {
    init {
        require(provider == null || provider == "google")
        require(provider == null || (!recovery && email.isEmpty()))
    }
    val challenge: String get() = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
    val callbackUri: String get() = "$redirectUri?flow_id=$flowId"
    fun isCurrent(now: Long): Boolean = createdAt <= now + 60 && now - createdAt <= 3600
    override fun toString(): String = "PendingAuthFlow(redacted)"
    companion object {
        fun create(email: String, recovery: Boolean, now: Long, redirectUri: String, provider: String? = null): PendingAuthFlow {
            val random = SecureRandom()
            fun token() = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also(random::nextBytes))
            return PendingAuthFlow(email, token(), token(), recovery, now, redirectUri, provider)
        }
    }
}

class AuthLink(val flowId: String, val code: String?, val failed: Boolean) {
    override fun toString(): String = "AuthLink(redacted)"
    companion object {
        fun parse(raw: String, redirectUri: String): AuthLink {
            try {
                require(raw.length <= 4096)
                val uri = URI(raw)
                val expected = URI(redirectUri)
                require(uri.scheme == expected.scheme && uri.rawAuthority == expected.rawAuthority && uri.path == expected.path)
                require(uri.userInfo == null && uri.port == -1 && uri.rawFragment == null)
                val values = linkedMapOf<String, String>()
                for (part in uri.rawQuery.orEmpty().split('&').filter { it.isNotBlank() }) {
                    val pair = part.split('=', limit = 2)
                    val key = URLDecoder.decode(pair[0], "UTF-8")
                    val value = URLDecoder.decode(pair.getOrElse(1) { "" }, "UTF-8")
                    require(values.put(key, value) == null)
                }
                // Never accept implicit-flow tokens supplied by another app.
                require("access_token" !in values && "refresh_token" !in values)
                val id = values["flow_id"].orEmpty().also { require(it.matches(Regex("[A-Za-z0-9_-]{43}"))) }
                val failed = !values["error"].isNullOrBlank() || !values["error_code"].isNullOrBlank()
                val code = values["code"]
                require(failed || code?.matches(Regex("[A-Za-z0-9._~-]{1,512}")) == true)
                return AuthLink(id, code, failed)
            } catch (_: Exception) {
                throw IllegalArgumentException("인증 링크가 올바르지 않아요. 이 앱에서 로그인이나 인증 메일을 다시 요청해 주세요.")
            }
        }
    }
}
