package com.moasseum.app.auth

import java.net.URI
import java.net.URLEncoder

// Browser OAuth, not an embedded WebView. Only the challenge leaves this device.
object GoogleOAuth {
    fun authorizationUrl(baseUrl: String, flow: PendingAuthFlow): String {
        val base = URI(baseUrl.trimEnd('/'))
        require(base.scheme == "https" && !base.host.isNullOrBlank() && base.userInfo == null && base.port == -1 && base.rawQuery == null && base.rawFragment == null && base.path.isNullOrEmpty()) {
            "구글 로그인 서버 주소를 확인해 주세요."
        }
        require(flow.provider == "google" && !flow.recovery && flow.email.isEmpty())
        val parameters = linkedMapOf(
            "provider" to "google", "redirect_to" to flow.callbackUri,
            "code_challenge" to flow.challenge, "code_challenge_method" to "s256",
            "scopes" to "openid email profile", "prompt" to "select_account",
        ).entries.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" }
        return "${base.toASCIIString()}/auth/v1/authorize?$parameters"
    }
}
