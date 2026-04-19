package com.colamusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.colamusic.core.database.entity.AlbumSearchEntity

@Dao
interface AlbumSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<AlbumSearchEntity>)

    /**
     * FTS4 MATCH search over album_search. The caller must pass a
     * pre-normalized query. Returns up to 50 album ids by default.
     */
    @Query("""
        SELECT c.* FROM cached_albums c
        INNER JOIN album_search s ON s.albumId = c.id
        WHERE album_search MATCH :match
        ORDER BY c.updatedAtMs DESC
        LIMIT :limit
    """)
    suspend fun searchMatch(match: String, limit: Int = 50): List<com.colamusic.core.database.entity.CachedAlbumEntity>

    @Query("DELETE FROM album_search") suspend fun clear()
}
