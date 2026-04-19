package com.colamusic.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.colamusic.core.database.entity.CachedAlbumEntity

@Dao
interface CachedAlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedAlbumEntity>)

    @Query("SELECT * FROM cached_albums ORDER BY updatedAtMs DESC")
    fun pagedAll(): PagingSource<Int, CachedAlbumEntity>

    @Query("""
        SELECT * FROM cached_albums
        WHERE nameNormalized LIKE '%' || :q || '%' OR artistNormalized LIKE '%' || :q || '%'
        ORDER BY updatedAtMs DESC LIMIT 50
    """)
    suspend fun searchNormalized(q: String): List<CachedAlbumEntity>

    @Query("DELETE FROM cached_albums") suspend fun clear()
}
