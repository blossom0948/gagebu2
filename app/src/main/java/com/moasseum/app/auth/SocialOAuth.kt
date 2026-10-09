package com.moasseum.app.auth

import java.net.URI
import java.net.URLEncoder

object SocialOAuth {
    val supportedProviders = setOf("google", "kakao")

    fun displayName(provider: String): String = when (provider) {
        "google" -> "Google"
        "kakao" -> "카카오"
        else -> error("지원하지 않는 로그인 방식입니다.")
    }

    fun authorizationUrl(baseUrl: String, flow: PendingAuthFlow): String {
        val provider = flow.provider ?: error("로그인 요청을 확인해 주세요.")
        require(provider in supportedProviders && !flow.recovery && flow.email.isEmpty())
        val base = URI(baseUrl.trimEnd('/'))
        require(base.scheme == "https" && !base.host.isNullOrBlank() && base.userInfo == null && base.port == -1 && base.rawQuery == null && base.rawFragment == null && base.path.isNullOrEmpty()) {
            "로그인 서버 주소를 확인해 주세요."
        }
        val parameters = linkedMapOf(
            "provider" to provider,
            "redirect_to" to flow.callbackUri,
            "code_challenge" to flow.challenge,
            "code_challenge_method" to "s256",
            "scopes" to when (provider) {
                "google" -> "openid email profile"
                // Kakao email is unavailable to this non-business app; profile fields stay optional.
                "kakao" -> "profile_nickname profile_image"
                else -> error("지원하지 않는 로그인 방식입니다.")
            },
        ).apply {
            if (provider == "google") put("prompt", "select_account")
        }
        val query = parameters.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
        return "${base.toASCIIString()}/auth/v1/authorize?$query"
    }
}
