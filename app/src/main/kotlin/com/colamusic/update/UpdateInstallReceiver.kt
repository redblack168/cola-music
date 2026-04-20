package com.colamusic.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.colamusic.core.common.Logx
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

/**
 * Fires when the DownloadManager finishes pulling the release APK. Before
 * launching the system installer we validate the file:
 *   - non-empty and at least 1 MB (rules out HTML error pages / truncated
 *     responses; the Cola release APK is ~17 MB)
 *   - first 4 bytes are the ZIP local-file-header magic (`PK\x03\x04`).
 *     APKs are ZIPs; if the magic isn't there, the file is either an HTML
 *     redirect document or a partial download.
 *
 * If validation fails we surface a toast pointing the user back to GitHub
 * instead of feeding an obviously broken file to PackageInstaller (which
 * would render as "解析失败 / file not intact" with no useful diagnostic).
 */
@AndroidEntryPoint
class UpdateInstallReceiver : BroadcastReceiver() {

    @Inject lateinit var updater: UpdateChecker

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (id < 0) return

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val q = DownloadManager.Query().setFilterById(id)
        dm.query(q).use { c ->
            if (c == null || !c.moveToFirst()) return
            val statusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val uriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON)
            if (statusIdx < 0 || uriIdx < 0) return

            val status = c.getInt(statusIdx)
            if (status != DownloadManager.STATUS_SUCCESSFUL) {
                val reason = if (reasonIdx >= 0) c.getInt(reasonIdx) else -1
                Logx.w("update", "download not successful (status=$status reason=$reason)")
                toast(context, "更新下载失败 (status=$status)")
                return
            }

            val local = c.getString(uriIdx) ?: return
            val file = File(Uri.parse(local).path ?: return)
            if (!file.exists()) {
                Logx.w("update", "downloaded file missing: $local")
                toast(context, "更新文件丢失,请重试")
                return
            }

            val problem = validateApk(file)
            if (problem != null) {
                Logx.w("update", "downloaded file invalid: $problem (size=${file.length()})")
                toast(context, "更新文件不完整或损坏 ($problem),请到 GitHub 手动下载")
                runCatching { file.delete() }
                return
            }

            runCatching { updater.openInstall(file) }
                .onFailure {
                    Logx.e("update", "openInstall failed", it)
                    toast(context, "无法打开安装器: ${it.message}")
                }
        }
    }

    private fun validateApk(file: File): String? {
        val len = file.length()
        if (len < MIN_APK_BYTES) return "size=${len}B"
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                if (magic[0] == 0x50.toByte() && magic[1] == 0x4B.toByte() &&
                    magic[2] == 0x03.toByte() && magic[3] == 0x04.toByte()
                ) null else "not a ZIP"
            }
        }.getOrElse { "read failed: ${it.message}" }
    }

    private fun toast(context: Context, message: String) {
        // BroadcastReceiver context is fine for short toasts — they're
        // queued onto the main looper internally.
        runCatching { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    private companion object {
        // Real Cola release APK is ~17 MB; anything under 1 MB is almost
        // certainly an error response (HTML, redirect doc, or a stub).
        const val MIN_APK_BYTES = 1_000_000L
    }
}
