package com.colamusic.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.colamusic.core.common.Logx
import com.colamusic.core.common.Outcome
import com.colamusic.core.database.entity.CachedAlbumEntity
import com.colamusic.core.database.search.AlbumSearchIndexer
import com.colamusic.core.lyrics.normalize.TextNormalizer
import com.colamusic.core.network.MusicServerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Pages through `getAlbumList2?type=alphabeticalByName` and populates the
 * local cached_albums + album_search (FTS4) tables with normalized fields.
 *
 * Runs off the main thread via WorkManager; triggered on login and periodically.
 * On failure, the worker retries with backoff (WorkManager default).
 */
@HiltWorker
class AlbumSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repo: MusicServerRepository,
    private val indexer: AlbumSearchIndexer,
    private val normalizer: TextNormalizer,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        var offset = 0
        var total = 0
        while (true) {
            val page = repo.allAlbumsByName(PAGE_SIZE, offset)
            val items = when (page) {
                is Outcome.Success -> page.value
                is Outcome.Failure -> {
                    Logx.w("sync", "album sync failed at offset=$offset: ${page.message}")
                    return if (offset == 0) Result.retry() else Result.success()
                }
            }
            if (items.isEmpty()) break
            val now = System.currentTimeMillis()
            val entities = items.map { a ->
                CachedAlbumEntity(
                    id = a.id,
                    name = a.name,
                    nameNormalized = normalizer.normalize(a.name, appContext),
                    artist = a.artist,
                    artistId = a.artistId,
                    artistNormalized = normalizer.normalizeArtist(a.artist, appContext),
                    year = a.year,
                    coverArt = a.coverArt,
                    songCount = a.songCount,
                    duration = a.duration,
                    starred = a.starred,
                    updatedAtMs = now,
                )
            }
            indexer.upsertBatch(entities)
            total += items.size
            if (items.size < PAGE_SIZE) break
            offset += items.size
        }
        Logx.i("sync", "album sync complete: $total albums indexed")
        return Result.success()
    }

    companion object {
        private const val PAGE_SIZE = 200
        const val UNIQUE_NAME = "cola.albumSync"
    }
}
