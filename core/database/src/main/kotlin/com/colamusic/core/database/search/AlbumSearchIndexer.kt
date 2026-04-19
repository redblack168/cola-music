package com.colamusic.core.database.search

import com.colamusic.core.database.dao.AlbumSearchDao
import com.colamusic.core.database.dao.CachedAlbumDao
import com.colamusic.core.database.entity.AlbumSearchEntity
import com.colamusic.core.database.entity.CachedAlbumEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapts CachedAlbumEntity → AlbumSearchEntity rows and persists both tables
 * in lock-step. Call [upsertBatch] from the library sync worker.
 */
@Singleton
class AlbumSearchIndexer @Inject constructor(
    private val cachedAlbumDao: CachedAlbumDao,
    private val searchDao: AlbumSearchDao,
) {
    suspend fun upsertBatch(entities: List<CachedAlbumEntity>) {
        if (entities.isEmpty()) return
        cachedAlbumDao.upsertAll(entities)
        searchDao.upsertAll(entities.map { it.toSearchRow() })
    }

    private fun CachedAlbumEntity.toSearchRow() = AlbumSearchEntity(
        rowid = 0,
        albumId = id,
        nameNormalized = nameNormalized,
        artistNormalized = artistNormalized,
        pinyin = PinyinIndexer.initials(name) +
            (if (PinyinIndexer.initials(artist).isNotEmpty()) " " +
                PinyinIndexer.initials(artist) else ""),
    )

    suspend fun clear() {
        cachedAlbumDao.clear()
        searchDao.clear()
    }
}
