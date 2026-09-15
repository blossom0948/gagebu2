package com.moasseum.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.moasseum.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(
    val tagName: String,
    val apkUrl: String,
    val releasePageUrl: String,
    val versionCode: Int,
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val release: AppRelease) : UpdateCheckState
    data object OpeningDownloadPage : UpdateCheckState
    data object DownloadPageOpened : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

object AppUpdateManager {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/blossom0948/gagebu2/releases/latest"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000

    suspend fun checkForUpdate(): Result<AppRelease?> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Moasseum/${BuildConfig.VERSION_NAME}")
            }
            connection.useConnection { responseCode ->
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) return@useConnection null
                if (responseCode !in 200..299) {
                    error("릴리스 정보를 가져오지 못했어요. ($responseCode)")
                }

                val releaseJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(releaseJson)
                val tagName = json.optString("tag_name").takeIf { it.isNotBlank() }
                    ?: error("릴리스 버전을 확인하지 못했어요.")
                val versionCode = parseVersionCode(json, tagName)
                    ?: error("릴리스 버전 형식을 확인하지 못했어요.")
                val apkUrl = json.optJSONArray("assets")
                    ?.let { assets ->
                        (0 until assets.length())
                            .asSequence()
                            .map { assets.optJSONObject(it) }
                            .filterNotNull()
                            .firstOrNull { asset ->
                                asset.optString("name").endsWith(".apk", ignoreCase = true)
                            }
                            ?.optString("browser_download_url")
                    }
                    ?.takeIf { it.isNotBlank() }
                    ?: error("릴리스 APK를 찾지 못했어요.")
                val releasePageUrl = json.optString("html_url").takeIf { it.isNotBlank() }
                    ?: error("릴리스 페이지를 찾지 못했어요.")

                if (versionCode <= BuildConfig.VERSION_CODE) null
                else AppRelease(
                    tagName = tagName,
                    apkUrl = apkUrl,
                    releasePageUrl = releasePageUrl,
                    versionCode = versionCode,
                )
            }
        }
    }

    fun openReleasePage(context: Context, release: AppRelease): Result<Unit> {
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.releasePageUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun parseVersionCode(json: JSONObject, tagName: String): Int? {
        val explicitCode = json.optInt("version_code", 0).takeIf { it > 0 }
        if (explicitCode != null) return explicitCode
        return Regex("v\\d+\\.\\d+\\.(\\d+)")
            .find(tagName)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
    }

    private inline fun <T> HttpURLConnection.useConnection(block: (Int) -> T): T {
        return try {
            block(responseCode)
        } finally {
            disconnect()
        }
    }
}
