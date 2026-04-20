package com.colamusic.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.colamusic.BuildConfig
import com.colamusic.core.common.Logx
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks api.github.com/repos/redblack168/cola-music/releases/latest and
 * surfaces an [UpdateInfo] when the remote version is newer than the
 * installed one. Users can tap "立即下载" to pull the release APK via
 * DownloadManager; the completion receiver opens the system package
 * installer on the downloaded file.
 */
@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // Dedicated plain client — shared Hilt-provided clients carry Subsonic /
    // Plex / Emby auth interceptors we don't want firing on github.com.
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$REPO/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "cola-music/${BuildConfig.VERSION_NAME}")
            .build()
        val body = runCatching {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null else resp.body?.string()
            }
        }.getOrNull() ?: return@withContext UpdateResult.Error("无法连接 GitHub")

        val root = runCatching { JSONObject(body) }.getOrElse {
            Logx.w("update", "parse failed: ${it.message}")
            return@withContext UpdateResult.Error("解析失败")
        }
        val tag = root.optString("tag_name").orEmpty()
        val notes = root.optString("body").orEmpty()
        val htmlUrl = root.optString("html_url").orEmpty()
        val assets = root.optJSONArray("assets")
        var apkUrl: String? = null
        var apkName: String? = null
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val n = a.optString("name")
                if (n.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.optString("browser_download_url")
                    apkName = n
                    break
                }
            }
        }

        val latest = tag.removePrefix("v").trim()
        val current = BuildConfig.VERSION_NAME
        Logx.i("update", "latest=$latest current=$current apk=$apkUrl")

        if (latest.isEmpty() || !isNewer(latest, current) || apkUrl == null || apkName == null) {
            return@withContext UpdateResult.UpToDate(current)
        }
        UpdateResult.Available(
            UpdateInfo(
                latestVersion = latest,
                currentVersion = current,
                notes = notes.take(2000),
                apkUrl = apkUrl,
                apkName = apkName,
                htmlUrl = htmlUrl,
            )
        )
    }

    /** Kicks a DownloadManager job. Returns the queued download id. */
    fun download(info: UpdateInfo): Long {
        val dir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        // Wipe prior APKs so we don't accumulate over versions.
        dir.listFiles { f -> f.name.endsWith(".apk") }?.forEach { runCatching { it.delete() } }
        val target = File(dir, info.apkName)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val req = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("可乐音乐 ${info.latestVersion}")
            .setDescription("下载更新中")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(target))
            .setAllowedOverMetered(true)
        return dm.enqueue(req)
    }

    /** Called by [UpdateInstallReceiver] on ACTION_DOWNLOAD_COMPLETE. */
    fun openInstall(apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(install)
    }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = parseVersion(remote)
        val c = parseVersion(current)
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }

    private fun parseVersion(v: String): List<Int> =
        v.split('.', '-').mapNotNull { it.toIntOrNull() }

    companion object {
        private const val REPO = "redblack168/cola-music"
    }
}

sealed class UpdateResult {
    data class Available(val info: UpdateInfo) : UpdateResult()
    data class UpToDate(val version: String) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}

data class UpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val notes: String,
    val apkUrl: String,
    val apkName: String,
    val htmlUrl: String,
)

