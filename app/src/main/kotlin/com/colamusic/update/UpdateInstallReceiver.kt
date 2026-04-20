package com.colamusic.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.colamusic.core.common.Logx
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

/**
 * Fires when the DownloadManager finishes pulling the release APK; we
 * resolve the downloaded file's local path and hand it to UpdateChecker
 * to open the system package installer.
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
            if (statusIdx < 0 || uriIdx < 0) return
            if (c.getInt(statusIdx) != DownloadManager.STATUS_SUCCESSFUL) return
            val local = c.getString(uriIdx) ?: return
            val file = File(Uri.parse(local).path ?: return)
            if (!file.exists()) {
                Logx.w("update", "downloaded file missing: $local")
                return
            }
            runCatching { updater.openInstall(file) }
                .onFailure { Logx.e("update", "openInstall failed", it) }
        }
    }
}
