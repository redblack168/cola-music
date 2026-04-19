package com.colamusic.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.colamusic.core.common.Logx
import com.colamusic.core.download.DownloadRepository
import com.colamusic.core.download.DownloadStorage
import com.colamusic.core.network.SessionStore
import com.colamusic.core.network.SubsonicUrls
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import okio.source
import java.io.File

/**
 * Drains the download queue by fetching each queued song from Navidrome using
 * `/rest/download.view?id=…` and writing it atomically to the music cache.
 * Progress is mirrored into the DB so the UI stays live.
 *
 * Retries are left to WorkManager's backoff. LRU eviction runs after each
 * batch completes.
 */
@HiltWorker
class DownloadSongWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: DownloadRepository,
    private val storage: DownloadStorage,
    private val sessionStore: SessionStore,
    private val okHttp: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val cfg = sessionStore.current.value ?: return@withContext Result.retry()
        var processed = 0
        while (true) {
            val next = repo.nextQueued(limit = 1).firstOrNull() ?: break
            val songId = next.songId
            repo.markDownloading(songId)
            val url = SubsonicUrls.downloadUrl(cfg, songId)
            val suffix = next.suffix
            val rel = storage.relativePathFor(songId, suffix)
            val tmp = File(storage.fileFor(rel).absolutePath + ".part")
            val final = storage.fileFor(rel)
            tmp.parentFile?.mkdirs()
            val outcome = runCatching {
                okHttp.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body ?: error("empty body")
                    val total = body.contentLength()
                    var downloaded = 0L
                    var lastReport = 0
                    body.source().use { source ->
                        tmp.sink().buffer().use { sink ->
                            val buf = okio.Buffer()
                            while (true) {
                                val read = source.read(buf, 64 * 1024L)
                                if (read == -1L) break
                                sink.write(buf, read)
                                downloaded += read
                                val pct = if (total > 0) ((downloaded * 100L) / total).toInt() else 0
                                if (pct - lastReport >= 5 || (total <= 0 && downloaded % (512 * 1024L) == 0L)) {
                                    repo.markProgress(songId, pct, downloaded)
                                    lastReport = pct
                                }
                            }
                            sink.flush()
                        }
                    }
                    downloaded
                }
            }
            if (outcome.isFailure) {
                val e = outcome.exceptionOrNull()
                Logx.w("download", "song $songId failed: ${e?.message}")
                repo.markFailed(songId, e?.message)
                runCatching { tmp.delete() }
                continue
            }
            val bytes = outcome.getOrThrow()
            // Atomic move
            if (final.exists()) final.delete()
            if (!tmp.renameTo(final)) {
                Logx.w("download", "rename failed for $songId; copying")
                tmp.source().use { src -> final.sink().buffer().use { sink -> sink.writeAll(src); sink.flush() } }
                tmp.delete()
            }
            repo.markCompleted(songId, rel, bytes)
            processed++
        }
        // Enforce cap
        runCatching { repo.enforceStorageCap() }
        Logx.i("download", "download worker done; processed=$processed")
        Result.success()
    }

    companion object { const val UNIQUE_NAME = "cola.downloadSongs" }
}
