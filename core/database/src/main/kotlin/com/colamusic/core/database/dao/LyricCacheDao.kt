package com.colamusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.colamusic.core.database.entity.LyricCacheEntity

@Dao
interface LyricCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LyricCacheEntity)

    @Query("SELECT * FROM lyric_cache WHERE songId = :songId LIMIT 1")
    suspend fun find(songId: String): LyricCacheEntity?

    @Query("DELETE FROM lyric_cache WHERE songId = :songId")
    suspend fun delete(songId: String)

    @Query("DELETE FROM lyric_cache WHERE expiresAtMs < :now")
    suspend fun purgeExpired(now: Long)
}
