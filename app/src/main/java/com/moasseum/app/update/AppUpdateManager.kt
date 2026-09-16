package com.moasseum.app.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.moasseum.app.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppRelease(
    val tagName: String,
    val apkUrl: String,
    val releasePageUrl: String,
    val versionName: String,
    val sha256: String? = null,
    val apkSizeBytes: Long = 0L,
)

sealed interface UpdateCheckState {
    data object Idle : UpdateCheckState
    data object Checking : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data class Available(val release: AppRelease) : UpdateCheckState
    data class Downloading(val progressPercent: Int) : UpdateCheckState
    data object WaitingForInstallPermission : UpdateCheckState
    data object OpeningInstaller : UpdateCheckState
    data object InstallerOpened : UpdateCheckState
    data class Error(val message: String) : UpdateCheckState
}

object AppUpdateManager {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/blossom0948/gagebu2/releases/latest"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val MAX_APK_BYTES = 160L * 1024L * 1024L

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
                if (responseCode !in 200..299) error("릴리스 정보를 가져오지 못했어요. ($responseCode)")

                val releaseJson = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(releaseJson)
                val tagName = json.optString("tag_name").takeIf { it.isNotBlank() }
                    ?: error("릴리스 버전을 확인하지 못했어요.")
                val versionName = parseVersionName(tagName)
                    ?: error("릴리스 버전 형식을 확인하지 못했어요.")
                val apkAsset = json.optJSONArray("assets")
                    ?.let { assets ->
                        (0 until assets.length())
                            .asSequence()
                            .map { assets.optJSONObject(it) }
                            .filterNotNull()
                            .firstOrNull { asset -> asset.optString("name").equals("app-release.apk", ignoreCase = true) }
                    }
                    ?: error("릴리스 APK를 찾지 못했어요.")
                val apkUrl = apkAsset.optString("browser_download_url")
                    .takeIf { it.isNotBlank() }
                    ?: error("릴리스 APK 주소를 찾지 못했어요.")
                val releasePageUrl = json.optString("html_url").takeIf { it.isNotBlank() }
                    ?: error("릴리스 페이지를 확인하지 못했어요.")
                val digest = apkAsset.optString("digest")
                    .removePrefix("sha256:")
                    .takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }
                val size = apkAsset.optLong("size", 0L).coerceAtLeast(0L)

                if (!isVersionNewer(versionName, BuildConfig.VERSION_NAME)) null
                else AppRelease(tagName, apkUrl, releasePageUrl, versionName, digest, size)
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: AppRelease,
        onProgress: (Int) -> Unit = {},
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(release.apkUrl.startsWith("https://github.com/blossom0948/gagebu2/releases/download/")) {
                "업데이트 APK의 출처가 올바르지 않아요."
            }
            require(release.versionName.matches(Regex("\\d+\\.\\d+\\.\\d+"))) { "업데이트 버전이 올바르지 않아요." }
            require(release.apkSizeBytes in 1..MAX_APK_BYTES) { "APK 파일 크기를 확인할 수 없거나 너무 커요." }

            val updateDirectory = File(context.cacheDir, "updates")
            check(updateDirectory.exists() || updateDirectory.mkdirs()) { "업데이트 저장 공간을 만들지 못했어요." }
            val apkFile = File(updateDirectory, "moasseum-${release.versionName}.apk")
            val partialFile = File(updateDirectory, "moasseum-${release.versionName}.apk.part")
            partialFile.delete()
            var downloadedCompletely = false
            try {
                val connection = (URL(release.apkUrl).openConnection() as? HttpURLConnection)
                    ?: throw IOException("APK 다운로드 주소를 열지 못했어요.")
                connection.instanceFollowRedirects = true
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/octet-stream")
                connection.setRequestProperty("User-Agent", "Moasseum/${BuildConfig.VERSION_NAME}")
                try {
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) throw IOException("APK 다운로드에 실패했어요. ($responseCode)")
                    val contentLength = connection.contentLengthLong
                    if (contentLength > MAX_APK_BYTES || (contentLength > 0 && contentLength != release.apkSizeBytes)) {
                        throw IOException("APK 파일 크기가 릴리스 정보와 일치하지 않아요.")
                    }
                    connection.inputStream.use { input ->
                        FileOutputStream(partialFile).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var downloaded = 0L
                            var lastReported = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                downloaded += read
                                if (downloaded > MAX_APK_BYTES || downloaded > release.apkSizeBytes) {
                                    throw IOException("다운로드 APK 크기가 예상보다 커요.")
                                }
                                output.write(buffer, 0, read)
                                val progress = ((downloaded * 100L) / release.apkSizeBytes).toInt().coerceIn(0, 100)
                                if (progress != lastReported) {
                                    lastReported = progress
                                    onProgress(progress)
                                }
                            }
                            output.fd.sync()
                            if (downloaded != release.apkSizeBytes) throw IOException("APK 다운로드가 끝까지 완료되지 않았어요.")
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                release.sha256?.let { expected ->
                    val actual = sha256(partialFile)
                    if (!actual.equals(expected, ignoreCase = true)) throw IOException("다운로드한 APK의 SHA-256 검증에 실패했어요.")
                }
                verifyApk(context, partialFile, release.versionName)
                if (apkFile.exists() && !apkFile.delete()) throw IOException("이전 임시 업데이트 파일을 정리하지 못했어요.")
                if (!partialFile.renameTo(apkFile)) throw IOException("다운로드 APK를 준비하지 못했어요.")
                downloadedCompletely = true
                onProgress(100)
                apkFile
            } finally {
                if (!downloadedCompletely) partialFile.delete()
            }
        }
    }

    fun canInstallFromThisApp(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchInstaller(context: Context, apkFile: File): Result<Unit> = runCatching {
        require(apkFile.isFile && apkFile.length() > 0L) { "설치할 APK 파일이 없어요. 다시 다운로드해 주세요." }
        check(canInstallFromThisApp(context)) { "이 앱에서 설치하도록 허용한 뒤 다시 시도해 주세요." }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun verifyApk(context: Context, apkFile: File, expectedVersionName: String) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: throw IOException("다운로드한 파일이 정상적인 APK가 아니에요.")
        if (archive.packageName != context.packageName) throw IOException("다른 앱의 APK라 설치를 중단했어요.")
        if (archive.versionName != expectedVersionName) throw IOException("APK 버전이 릴리스 정보와 일치하지 않아요.")
        val current = context.packageManager.getPackageInfo(context.packageName, flags)
        if (PackageInfoCompat.getLongVersionCode(archive) <= PackageInfoCompat.getLongVersionCode(current)) {
            throw IOException("설치된 버전보다 새 버전이 아니에요.")
        }
        if (signerDigests(archive) != signerDigests(current)) {
            throw IOException("기존 앱과 서명 키가 달라 업데이트를 안전하게 중단했어요.")
        }
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        @Suppress("DEPRECATION")
        val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            info.signatures?.toList().orEmpty()
        }
        if (signers.isEmpty()) throw IOException("APK 서명을 확인할 수 없어요.")
        return signers.map { signature -> digest(signature.toByteArray()) }.toSet()
    }

    private fun sha256(file: File): String = file.inputStream().buffered().use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun digest(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value)
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun parseVersionName(tagName: String): String? =
        Regex("v?(\\d+\\.\\d+\\.\\d+)").find(tagName)?.groupValues?.getOrNull(1)

    private fun isVersionNewer(remote: String, current: String): Boolean {
        val remoteParts = versionParts(remote) ?: return remote != current
        val currentParts = versionParts(current) ?: return remote != current
        for (index in remoteParts.indices) {
            if (remoteParts[index] != currentParts[index]) return remoteParts[index] > currentParts[index]
        }
        return false
    }

    private fun versionParts(version: String): List<Int>? =
        Regex("(\\d+)(?:\\.(\\d+))(?:\\.(\\d+))")
            .find(version)
            ?.groupValues
            ?.drop(1)
            ?.mapNotNull(String::toIntOrNull)
            ?.takeIf { it.size == 3 }

    private inline fun <T> HttpURLConnection.useConnection(block: (Int) -> T): T =
        try {
            block(responseCode)
        } finally {
            disconnect()
        }
}
