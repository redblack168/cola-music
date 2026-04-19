package com.colamusic.core.download

import com.colamusic.core.common.Logx
import com.colamusic.core.database.dao.DownloadedSongDao
import com.colamusic.core.database.entity.DownloadedSongEntity
import com.colamusic.core.model.DownloadStatus
import com.colamusic.core.model.DownloadedSong
import com.colamusic.core.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade for queueing and observing downloaded songs. The actual bytes are
 * fetched by a WorkManager worker; this class owns only the DB state and file
 * path resolution.
 */
@Singleton
class DownloadRepository @Inject constructor(
    private val dao: DownloadedSongDao,
    private val storage: DownloadStorage,
    private val prefs: DownloadPreferences,
) {
    val completed: Flow<List<DownloadedSong>> = dao.observeCompleted().map { it.map { e -> e.toDomain() } }
    val active: Flow<List<DownloadedSong>> = dao.observeActive().map { it.map { e -> e.toDomain() } }

    suspend fun isDownloaded(songId: String): Boolean {
        val row = dao.find(songId) ?: return false
        if (row.status != DownloadStatus.Completed.name) return false
        return storage.resolveOrNull(row.relativePath) != null
    }

    fun offlineFileFor(songId: String): java.io.File? = runCatching {
        kotlinx.coroutines.runBlocking { dao.find(songId) }
            ?.takeIf { it.status == DownloadStatus.Completed.name }
            ?.let { storage.resolveOrNull(it.relativePath) }
    }.getOrNull()

    /** Inserts queued rows for any songs not already downloaded. */
    suspend fun enqueue(songs: List<Song>) {
        val now = System.currentTimeMillis()
        val rows = songs.map { s ->
            val existing = dao.find(s.id)
            if (existing != null && existing.status == DownloadStatus.Completed.name) return@map existing
            DownloadedSongEntity(
                songId = s.id,
                title = s.title,
                artist = s.artist,
                album = s.album,
                albumId = s.albumId,
                coverArt = s.coverArt,
                duration = s.duration,
                suffix = s.suffix,
                bitRate = s.bitRate,
                sampleRate = s.sampleRate,
                bitDepth = s.bitDepth,
                relativePath = existing?.relativePath,
                byteSize = 0,
                status = DownloadStatus.Queued.name,
                progressPct = 0,
                errorMessage = null,
                updatedAtMs = now,
                completedAtMs = null,
            )
        }
        dao.upsertAll(rows)
    }

    suspend fun enqueueSingle(song: Song) = enqueue(listOf(song))

    suspend fun nextQueued(limit: Int = 4): List<DownloadedSongEntity> =
        dao.activeRows().filter { it.status == DownloadStatus.Queued.name }.take(limit)

    suspend fun markDownloading(songId: String) {
        val row = dao.find(songId) ?: return
        dao.update(row.copy(status = DownloadStatus.Downloading.name, updatedAtMs = System.currentTimeMillis()))
    }

    suspend fun markProgress(songId: String, pct: Int, bytes: Long) {
        val row = dao.find(songId) ?: return
        dao.update(row.copy(
            progressPct = pct.coerceIn(0, 100),
            byteSize = bytes,
            updatedAtMs = System.currentTimeMillis(),
        ))
    }

    suspend fun markCompleted(songId: String, relativePath: String, bytes: Long) {
        val row = dao.find(songId) ?: return
        val now = System.currentTimeMillis()
        dao.update(row.copy(
            status = DownloadStatus.Completed.name,
            progressPct = 100,
            relativePath = relativePath,
            byteSize = bytes,
            updatedAtMs = now,
            completedAtMs = now,
            errorMessage = null,
        ))
    }

    suspend fun markFailed(songId: String, message: String?) {
        val row = dao.find(songId) ?: return
        dao.update(row.copy(
            status = DownloadStatus.Failed.name,
            errorMessage = message,
            updatedAtMs = System.currentTimeMillis(),
        ))
    }

    suspend fun remove(songId: String) {
        val row = dao.find(songId) ?: return
        storage.deleteFile(row.relativePath)
        dao.delete(songId)
    }

    /** Drops completed downloads until usage is ≤ cap. Called periodically. */
    suspend fun enforceStorageCap(capBytes: Long? = null) {
        val cap = capBytes ?: (prefs.storageCapBytes.firstOrNull() ?: DownloadPreferences.DEFAULT_CAP)
        var used = storage.usedBytes()
        if (used <= cap) return
        Logx.i("download", "storage over cap (${used/1_000_000}MB > ${cap/1_000_000}MB), evicting LRU")
        var evicted = 0
        while (used > cap) {
            val page = dao.oldestCompleted(limit = 8).takeIf { it.isNotEmpty() } ?: break
            for (row in page) {
                storage.deleteFile(row.relativePath)
                dao.delete(row.songId)
                used -= row.byteSize
                evicted += 1
                if (used <= cap) break
            }
        }
        Logx.i("download", "LRU eviction complete; removed $evicted files, used=${used/1_000_000}MB")
    }
}

private fun DownloadedSongEntity.toDomain(): DownloadedSong {
    val song = Song(
        id = songId, title = title, album = album, albumId = albumId,
        artist = artist, artistId = null, duration = duration,
        suffix = suffix, bitRate = bitRate, sampleRate = sampleRate,
        bitDepth = bitDepth, coverArt = coverArt,
    )
    val status = runCatching { DownloadStatus.valueOf(this.status) }
        .getOrDefault(DownloadStatus.Failed)
    return DownloadedSong(
        song = song, status = status, progressPct = progressPct,
        byteSize = byteSize, relativePath = relativePath,
        errorMessage = errorMessage, updatedAtMs = updatedAtMs,
        completedAtMs = completedAtMs,
    )
}
