package com.moasseum.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.moasseum.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppRelease(
    val tagName: String,
    val apkUrl: String,
    val versionCode: Int,
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val release: AppRelease) : UpdateCheckState
    data class Downloading(val release: AppRelease) : UpdateCheckState
    data object WaitingForInstallPermission : UpdateCheckState
    data class Installing(val release: AppRelease) : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

sealed interface InstallResult {
    data object Started : InstallResult
    data object PermissionRequired : InstallResult
    data class Failed(val message: String) : InstallResult
}

object AppUpdateManager {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/blossom0948/gagebu2/releases/latest"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
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

                if (versionCode <= BuildConfig.VERSION_CODE) null
                else AppRelease(tagName = tagName, apkUrl = apkUrl, versionCode = versionCode)
            }
        }
    }

    suspend fun downloadApk(context: Context, release: AppRelease): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
            val safeTag = release.tagName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val apkFile = File(updateDirectory, "moasseum-$safeTag.apk")
            val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Moasseum/${BuildConfig.VERSION_NAME}")
            }
            connection.useConnection { responseCode ->
                if (responseCode !in 200..299) error("APK를 다운로드하지 못했어요. ($responseCode)")
                connection.inputStream.use { input ->
                    apkFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            apkFile
        }
    }

    fun install(context: Context, apkFile: File): InstallResult {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            return InstallResult.Failed("다운로드된 APK를 찾지 못했어요.")
        }
        if (!context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return InstallResult.PermissionRequired
        }

        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile,
            )
            val installerIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installerIntent)
            InstallResult.Started
        }.getOrElse { error ->
            InstallResult.Failed(error.message ?: "APK 설치 화면을 열지 못했어요.")
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
