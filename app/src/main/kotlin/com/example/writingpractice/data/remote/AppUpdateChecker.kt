package com.example.writingpractice.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.writingpractice.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(val version: String, val downloadUrl: String)

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
private data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String
)

@Singleton
class AppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    companion object {
        private const val RELEASES_URL =
            "https://api.github.com/repos/leopold0106/writing-practice/releases/latest"
        private const val AUTHORITY = "com.example.writingpractice.fileprovider"
    }

    suspend fun checkForUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            val response = http.newCall(request).execute()
            if (response.code == 404) {
                // No releases published yet — treat as up to date.
                return@withContext Result.success(null)
            }
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("서버 응답 오류: ${response.code}"))
            }
            val body = response.body?.string()
                ?: return@withContext Result.failure(Exception("빈 응답"))
            val release = json.decodeFromString<GithubRelease>(body)
            val remoteVersion = release.tagName.removePrefix("v")
            if (remoteVersion.isBlank()) {
                return@withContext Result.failure(Exception("버전 형식 오류: ${release.tagName}"))
            }
            if (!isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                return@withContext Result.success(null)
            }
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext Result.failure(Exception("APK 파일을 찾을 수 없습니다"))
            Result.success(UpdateInfo(version = release.tagName, downloadUrl = apkAsset.browserDownloadUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(url: String, onProgress: (Float) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val apkFile = File(context.getExternalFilesDir(null), "update.apk")
                val request = Request.Builder().url(url).build()
                val response = http.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("다운로드 실패: ${response.code}"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(Exception("빈 응답"))
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                onProgress(downloadedBytes.toFloat() / totalBytes)
                            }
                        }
                    }
                }
                Result.success(apkFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun installApk(apkFile: File) {
        if (!canInstallPackages()) {
            openInstallPermissionSettings()
            return
        }
        val uri = FileProvider.getUriForFile(context, AUTHORITY, apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun canInstallPackages(): Boolean = context.packageManager.canRequestPackageInstalls()

    // Returns true if remote > current using component-wise semver comparison.
    // Supports both "2" and "1.0.1" style versions.
    private fun isNewerVersion(remote: String, current: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val c = current.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv > cv) return true
            if (rv < cv) return false
        }
        return false
    }

    fun openInstallPermissionSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
