package com.moasseum.app

import com.moasseum.app.auth.SupabaseAuthApi
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AuthSessionParsingTest {
    private fun response() = JSONObject().put("user", JSONObject().put("id", "72a43f9d-00e0-44db-af34-4cf143314bb7").put("email", "qa@example.invalid"))
        .put("access_token", "synthetic-access").put("refresh_token", "synthetic-refresh").put("expires_in", 3600)
    @Test fun relativeExpirationParsedCorrectly() { assertEquals(4600L, SupabaseAuthApi.parseSession(response(), 1000).expiresAt) }
    @Test fun absoluteExpirationDoesNotRequireExpiresIn() { assertEquals(4600L, SupabaseAuthApi.parseSession(response().apply { remove("expires_in"); put("expires_at", 4600) }, 1000).expiresAt) }
    @Test(expected = IllegalArgumentException::class) fun expiredTokenRejected() { SupabaseAuthApi.parseSession(response().put("expires_at", 900), 1000) }
    @Test(expected = IllegalArgumentException::class) fun invalidUserIdRejected() { val r = response(); r.getJSONObject("user").put("id", "not-a-uuid"); SupabaseAuthApi.parseSession(r, 1000) }
    @Test(expected = IllegalArgumentException::class) fun missingAccessTokenRejected() { SupabaseAuthApi.parseSession(response().put("access_token", ""), 1000) }
    @Test fun malformedSessionNeverExposesTokensInError() {
        val error = runCatching { SupabaseAuthApi.parseSession(response().put("expires_at", "SYNTHETIC_PRIVATE_TOKEN"), 1000) }.exceptionOrNull()!!
        assertFalse(error.toString().contains("SYNTHETIC_PRIVATE_TOKEN"))
        assertFalse(error.toString().contains("synthetic-access"))
        assertNull(error.cause)
    }
}
